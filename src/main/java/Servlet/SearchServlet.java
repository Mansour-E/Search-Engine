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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;
import org.json.JSONArray;


public class SearchServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String query = request.getParameter("query");

        int resultSize = Integer.parseInt(request.getParameter("k"));

        if (query != null) {
            try {
                JSONObject jsonQuery = new JSONObject(query);

                // Create connection with db
                DBConnection db = new DBConnection("IS-Project", "postgres", "9157", false);

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

    // Version 1
    /*
    private JSONObject executeSearch (DBConnection db, JSONObject jsonQuery, int resultSize) throws SQLException {

        String[] langTerms;
        boolean isConjuctive;
        String[] searchTerms;
        String searchTermsAsString = "";
        Set<String> allowedDomainsAndSites = new HashSet<>();
        List<SearchResult> foundItems;
        JSONObject resultJson = new JSONObject();

        JSONArray jsonSearchTerms = jsonQuery.getJSONArray("searchTerms");
        searchTerms = new String[jsonSearchTerms.length()];
        for (int i = 0; i < jsonSearchTerms.length(); i++) {
            String term = jsonSearchTerms.getString(i);
            searchTerms[i] = term ;
            searchTermsAsString += (term + " ");
        }

        JSONArray jsonSiteDomainTerms = jsonQuery.getJSONArray("domainSiteTerms");
        for (int i = 0; i < jsonSiteDomainTerms.length(); i++) {
            allowedDomainsAndSites.add(jsonSiteDomainTerms.getString(i));
        }

        isConjuctive = jsonQuery.getBoolean("isConjunctive");

        JSONArray languageTerms = jsonQuery.getJSONArray ("languages");
        langTerms = new String[languageTerms.length()];
        for (int i = 0; i < languageTerms.length(); i++) {
            String lang = languageTerms.getString(i);
            langTerms[i] = lang ;
        }



        if (isConjuctive) {
            foundItems = db.conjuntiveCrawling(searchTerms ,resultSize, List.of(langTerms));
        } else {
            foundItems = db.disjunctiveCrawling(searchTerms ,resultSize, List.of(langTerms));
        }

        if(foundItems.isEmpty()) {
            System.out.println("there are noterms that match these words");
        }

        System.out.printf("allowedDomainsAndSites " + allowedDomainsAndSites);
        // create ResultList Object
        JSONArray resultList = new JSONArray();
        for (int i = 0; i < foundItems.size(); i++) {
            SearchResult foundItem = foundItems.get(i);
            String itemUrl = foundItem.getUrl();
            boolean shouldAddItem = allowedDomainsAndSites.isEmpty();

            // Check if item URL is allowed
            if (!shouldAddItem) {
                for (String allowedDomain : allowedDomainsAndSites) {
                    System.out.printf("allowedDomain " +allowedDomain);

                    if (itemUrl.contains(allowedDomain)) {
                        shouldAddItem = true;
                        break;
                    }
                }
            }

            // If allowed, add item to result list
            if (shouldAddItem) {
                JSONObject foundItemObject = new JSONObject();
                foundItemObject.put("rank", i + 1);
                foundItemObject.put("url", itemUrl);
                foundItemObject.put("score", foundItem.getScore());
                resultList.put(foundItemObject);
                // System.out.println("rank " + (i + 1) + ": " + itemUrl + " (Score: " + foundItem.getScore() + ")");
            }
        }
        resultJson.put("resultList", resultList);

        //create Query Object
        JSONObject queryObject = new JSONObject();
        queryObject.put("k", resultSize);
        queryObject.put("query", searchTermsAsString);
        resultJson.put("query", queryObject);

        // create stat object
        JSONArray stat =  db.computeStat(searchTerms);
        resultJson.put("stat", stat);

        //add cw term
        int cw = db.calcualteCW();
        resultJson.put("cw", cw);


        return resultJson;
    }
    */
    // Version 2
    private JSONObject executeSearch (DBConnection db, JSONObject jsonQuery, int resultSize) throws SQLException {

        String[] langTerms;
        String[] conjuctiveSearchTerms;
        String[] disjunctiveSearchTerms;
        String searchTermsAsString = "";
        Set<String> allowedDomainsAndSites = new HashSet<>();
        List<SearchResult> foundItems;
        JSONObject resultJson = new JSONObject();

        JSONArray jsonConjuctiveSearchTerms = jsonQuery.getJSONArray("conjuctiveSearchTerms");
        conjuctiveSearchTerms = new String[jsonConjuctiveSearchTerms.length()];
        for (int i = 0; i < jsonConjuctiveSearchTerms.length(); i++) {
            String term = jsonConjuctiveSearchTerms.getString(i);
            conjuctiveSearchTerms[i] = term ;
            searchTermsAsString += (term + " ");
        }

        JSONArray jsonDisjunctiveSearchTerms = jsonQuery.getJSONArray("disjunctiveSearchTerms");
        disjunctiveSearchTerms = new String[jsonDisjunctiveSearchTerms.length()];
        for (int i = 0; i < jsonDisjunctiveSearchTerms.length(); i++) {
            String term = jsonDisjunctiveSearchTerms.getString(i);
            disjunctiveSearchTerms[i] = term ;
            searchTermsAsString += (term + " ");
        }

        JSONArray jsonSiteDomainTerms = jsonQuery.getJSONArray("domainSiteTerms");
        for (int i = 0; i < jsonSiteDomainTerms.length(); i++) {
            allowedDomainsAndSites.add(jsonSiteDomainTerms.getString(i));
        }

        JSONArray languageTerms = jsonQuery.getJSONArray ("languages");
        langTerms = new String[languageTerms.length()];
        for (int i = 0; i < languageTerms.length(); i++) {
            String lang = languageTerms.getString(i);
            langTerms[i] = lang ;
        }

        String scoreOption = jsonQuery.getString("scoreOption");
        foundItems = db.searchCrawling(conjuctiveSearchTerms, disjunctiveSearchTerms,resultSize, List.of(langTerms), scoreOption);

        if(foundItems.isEmpty()) {
            System.out.println("there are noterms that match these words");
        }
        // create ResultList Object
        JSONArray resultList = new JSONArray();
        int displayRankingCounter = 1;
        for (int i = 0; i < foundItems.size(); i++) {
            SearchResult foundItem = foundItems.get(i);
            String itemUrl = foundItem.getUrl();
            boolean shouldAddItem = allowedDomainsAndSites.isEmpty();

            // Check if item URL is allowed
            if (!shouldAddItem) {
                for (String allowedDomain : allowedDomainsAndSites) {
                    if (itemUrl.contains(allowedDomain)) {
                        shouldAddItem = true;
                        break;
                    }
                }
            }

            // If allowed, add item to result list
            if (shouldAddItem) {
                Double score = foundItem.getScore();
                JSONObject foundItemObject = new JSONObject();
                foundItemObject.put("rank", displayRankingCounter);
                foundItemObject.put("url", itemUrl);
                foundItemObject.put("score", score);
                resultList.put(foundItemObject);
                displayRankingCounter ++;
            }
        }
        resultJson.put("resultList", resultList);

        //create Query Object
        JSONObject queryObject = new JSONObject();
        queryObject.put("k", resultSize);
        queryObject.put("query", searchTermsAsString);
        resultJson.put("query", queryObject);

        // create stat object

        JSONArray stat =  db.computeStat(conjuctiveSearchTerms, disjunctiveSearchTerms);
        resultJson.put("stat", stat);

        //add cw term
        int cw = db.calcualteCW();
        resultJson.put("cw", cw);

        return resultJson;
    }


}
