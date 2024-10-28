package controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;

@WebServlet("/search")
public class SearchServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/yourdatabase";
    private static final String DB_USER = "yourusername";
    private static final String DB_PASSWORD = "yourpassword";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String query = request.getParameter("query");
        int k = Integer.parseInt(request.getParameter("k"));
        boolean isConjunctive = Boolean.parseBoolean(request.getParameter("conjunctive"));

        if (query == null || query.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Query parameter is missing or empty.");
            return;
        }

        response.setContentType("application/json");
        try (PrintWriter out = response.getWriter()) {
            JSONObject jsonResponse = executeSearch(query, k, isConjunctive);
            out.print(jsonResponse);
            out.flush();
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing search query: " + e.getMessage());
        }
    }

    private JSONObject executeSearch(String query, int k, boolean isConjunctive) throws SQLException {
        JSONObject resultJson = new JSONObject();
        JSONArray resultList = new JSONArray();
        JSONArray stats = new JSONArray();
        int totalTerms = 0;

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Set up the SQL query for disjunctive or conjunctive search
            String sql = createSQLQuery(query, isConjunctive);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, k);

                try (ResultSet resultSet = statement.executeQuery()) {
                    int rank = 1;
                    while (resultSet.next()) {
                        JSONObject result = new JSONObject();
                        result.put("rank", rank++);
                        result.put("url", resultSet.getString("url"));
                        result.put("score", resultSet.getDouble("score"));
                        resultList.put(result);
                    }
                }

                // Retrieve document frequency and collection word count
                totalTerms = calculateTermStats(connection, query, stats);
            }
        }

        // Build final JSON response
        resultJson.put("resultList", resultList);
        resultJson.put("query", new JSONObject().put("k", k).put("query", query));
        resultJson.put("stat", stats);
        resultJson.put("cw", totalTerms);

        return resultJson;
    }

    private String createSQLQuery(String query, boolean isConjunctive) {
        String[] terms = query.split(" ");
        StringBuilder sql = new StringBuilder("SELECT docid, url, SUM(score) as score FROM features ");
        sql.append("JOIN documents ON features.docid = documents.docid ");

        sql.append("WHERE ");
        for (int i = 0; i < terms.length; i++) {
            sql.append("term = ?");
            if (isConjunctive && i < terms.length - 1) {
                sql.append(" AND ");
            } else if (!isConjunctive && i < terms.length - 1) {
                sql.append(" OR ");
            }
        }

        sql.append(" GROUP BY docid, url ORDER BY score DESC LIMIT ?");
        return sql.toString();
    }

    private int calculateTermStats(Connection connection, String query, JSONArray stats) throws SQLException {
        String[] terms = query.split(" ");
        int totalTerms = 0;

        String termStatsSQL = "SELECT term, COUNT(docid) as df FROM features WHERE term = ? GROUP BY term";
        for (String term : terms) {
            try (PreparedStatement statement = connection.prepareStatement(termStatsSQL)) {
                statement.setString(1, term);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        JSONObject stat = new JSONObject();
                        stat.put("term", rs.getString("term"));
                        stat.put("df", rs.getInt("df"));
                        stats.put(stat);
                    }
                }
            }
        }

        String totalWordsSQL = "SELECT COUNT(*) FROM features";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(totalWordsSQL)) {
            if (rs.next()) {
                totalTerms = rs.getInt(1);
            }
        }

        return totalTerms;
    }
}
