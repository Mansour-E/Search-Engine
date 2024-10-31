package controller;

import CommandInterface.SearchResult;
import DB.DBConnection;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/search")
public class SearchServlet extends HttpServlet {
/**
    private DBConnection dbConnection;

    @Override
    public void init() throws ServletException {
        // Erstelle eine neue Verbindung zur Datenbank
        dbConnection = new DBConnection("dbName", "dbOwner", "dbPassword");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Holen Sie sich das Such-Keyword und die Anzahl der Ergebnisse aus der Anfrage
        String keyword = request.getParameter("query");
        int resultSize = Integer.parseInt(request.getParameter("limit"));

        // Konvertiere das Keyword in ein Array, um die Crawling-Methoden zu verwenden
        String[] keywords = {keyword};

        // Führe die Suche durch (kann conjuntiveCrawling oder disjunctiveCrawling sein)
        List<SearchResult> results = dbConnection.conjunctiveCrawling(keywords, resultSize);

        // Erstelle ein JSON-Array aus den Suchergebnissen
        JSONArray jsonResults = new JSONArray();
        for (SearchResult result : results) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("docID", result.getDocID());
            jsonObject.put("url", result.getUrl());
            jsonObject.put("score", result.getScore());
            jsonResults.put(jsonObject);
        }

        // Setze den Content-Typ auf JSON und sende die Antwort
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonResults.toString());
    }

    @Override
    public void destroy() {
        // Schließe die Datenbankverbindung
        if (dbConnection != null) {
            dbConnection.close();
        }
    }

@Override
protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    try {
        out.println("<html>");
        out.println("<head><title>Simple Servlet</title></head>");
        out.println("<body>");
        out.println("<h1>Hello from SimpleServlet!</h1>");
        out.println("<p>This is a simple servlet response.</p>");
        out.println("</body>");
        out.println("</html>");
    } finally {
        out.close();
    }
}**/
}