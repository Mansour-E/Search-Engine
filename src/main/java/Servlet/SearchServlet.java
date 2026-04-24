package Servlet;

import CommandInterface.SearchResult;
import DB.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
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
                DBConnection db = new DBConnection("IS-Project", "postgres", "9157", false);
                JSONObject resultList = this.executeSearch(db, jsonQuery, resultSize);
                System.out.printf("resultList" + resultList);

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

    private JSONObject executeSearch(DBConnection db, JSONObject jsonQuery, int resultSize) throws SQLException {
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
            conjuctiveSearchTerms[i] = term;
            searchTermsAsString += (term + " ");
        }

        JSONArray jsonDisjunctiveSearchTerms = jsonQuery.getJSONArray("disjunctiveSearchTerms");
        disjunctiveSearchTerms = new String[jsonDisjunctiveSearchTerms.length()];
        for (int i = 0; i < jsonDisjunctiveSearchTerms.length(); i++) {
            String term = jsonDisjunctiveSearchTerms.getString(i);
            disjunctiveSearchTerms[i] = term;
            searchTermsAsString += (term + " ");
        }

        JSONArray jsonSiteDomainTerms = jsonQuery.getJSONArray("domainSiteTerms");
        for (int i = 0; i < jsonSiteDomainTerms.length(); i++) {
            allowedDomainsAndSites.add(jsonSiteDomainTerms.getString(i));
        }

        JSONArray languageTerms = jsonQuery.getJSONArray("languages");
        langTerms = new String[languageTerms.length()];
        for (int i = 0; i < languageTerms.length(); i++) {
            langTerms[i] = languageTerms.getString(i);
        }

        String scoreOption = jsonQuery.getString("scoreOption");
        foundItems = db.searchCrawling(conjuctiveSearchTerms, disjunctiveSearchTerms, resultSize, List.of(langTerms), scoreOption);

        if (foundItems.isEmpty()) {
            System.out.println("No terms found that match these words.");
        }

        JSONArray resultList = new JSONArray();
        int displayRankingCounter = 1;
        for (int i = 0; i < foundItems.size(); i++) {
            SearchResult foundItem = foundItems.get(i);
            String itemUrl = foundItem.getUrl();
            boolean shouldAddItem = allowedDomainsAndSites.isEmpty();

            if (!shouldAddItem) {
                for (String allowedDomain : allowedDomainsAndSites) {
                    if (itemUrl.contains(allowedDomain)) {
                        shouldAddItem = true;
                        break;
                    }
                }
            }

            if (shouldAddItem) {
                JSONObject foundItemObject = new JSONObject();
                foundItemObject.put("rank", displayRankingCounter);
                foundItemObject.put("url", itemUrl);
                foundItemObject.put("score", foundItem.getScore());
                resultList.put(foundItemObject);
                displayRankingCounter++;
            }
        }
        resultJson.put("resultList", resultList);

        JSONObject queryObject = new JSONObject();
        queryObject.put("k", resultSize);
        queryObject.put("query", searchTermsAsString);
        resultJson.put("query", queryObject);

        JSONArray stat = db.computeStat(conjuctiveSearchTerms, disjunctiveSearchTerms);
        resultJson.put("stat", stat);

        int cw = db.calcualteCW();
        resultJson.put("cw", cw);

        return resultJson;
    }
}
