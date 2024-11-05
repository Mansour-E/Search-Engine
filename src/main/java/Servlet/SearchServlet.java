package Servlet;

import CommandInterface.SearchResult;
import DB.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.*;
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
        int resultSize = Integer.parseInt(request.getParameter("k"));


        if (query != null) {
            try {
                JSONObject jsonQuery = new JSONObject(query);

                // Create connection with db
                DBConnection db = new DBConnection("IS-Project", "postgres", "Yessin.10", false);

                JSONObject resultList = this.executeSearch(db, jsonQuery, resultSize);
                System.out.printf("resultList" +resultList);

                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(resultList.toString());


            } catch (Exception e) {
                e.printStackTrace();
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid query format.");
            }
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing query parameter.");
        }

    }


    private JSONObject executeSearch (DBConnection db, JSONObject jsonQuery, int resultSize) throws SQLException {

        boolean isConjuctive;
        String[] searchTerms;
        String searchTermsAsString = "";
        List<SearchResult> foundItems;

        JSONObject resultJson = new JSONObject();

        JSONArray jsonSearchTerms = jsonQuery.getJSONArray("searchTerms");
        searchTerms = new String[jsonSearchTerms.length()];
        for (int i = 0; i < jsonSearchTerms.length(); i++) {
            String term = jsonSearchTerms.getString(i);
            searchTerms[i] = term ;
            searchTermsAsString += (" " + term);
        }

        isConjuctive = jsonQuery.getBoolean("isConjunctive");

        // System.out.println("Domain Site Terms: " + jsonQuery.getJSONArray("domainSiteTerms"));

        if (isConjuctive) {
            foundItems = db.conjuntiveCrawling(searchTerms, resultSize);
        } else {
            foundItems = db.disjunctiveCrawling(searchTerms, resultSize);
        }

        System.out.printf("foundItems" + foundItems);

        if(foundItems.isEmpty()) {
            System.out.println("there are noterms that match these words");
        }

        // create ResultList Project
        JSONArray resultList = new JSONArray();
        for (int i = 0; i < foundItems.size(); i++) {
            SearchResult foundItem = foundItems.get(i);
            JSONObject foundItemObject = new JSONObject();
            foundItemObject.put("rank", (i+1));
            foundItemObject.put("url", foundItem.getUrl());
            foundItemObject.put("score",  foundItem.getScore());
            resultList.put(foundItemObject);
            System.out.println("rank " + (i+1) + ": " + foundItem.getUrl() + " (Score: " + foundItem.getScore() + ")");
        }
        resultJson.put("resultList", resultList);

        //create Query Object
        JSONObject queryObject = new JSONObject();
        queryObject.put("k", resultSize);
        queryObject.put("query", searchTermsAsString);
        resultJson.put("query", queryObject);

        //TODO cretae stat project
        // TODO cretae cw project

        return resultJson;
    }

}


