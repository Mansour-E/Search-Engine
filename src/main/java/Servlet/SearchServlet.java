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

@WebServlet("/search")
public class SearchServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/yourdatabase";
    private static final String DB_USER = "yourusername";
    private static final String DB_PASSWORD = "yourpassword";


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String query = request.getParameter("query");
        int resultSize = Integer.parseInt(request.getParameter("k"));
        String scoringModel = request.getParameter("scoringModel"); // "tfidf" oder "bm25"

        if (query != null) {
            try {
                JSONObject jsonQuery = new JSONObject(query);

                // Verbindung zur DB herstellen
                DBConnection db = new DBConnection("IS-Project", "postgres", "Yessin.10", false);

                // View basierend auf dem Scoring-Modell wählen
                String viewName = scoringModel.equalsIgnoreCase("bm25") ? "bm25_view" : "features_tfidf";

                // Suchen und Ergebnisse mit dem ausgewählten Modell berechnen
                JSONObject resultList = this.executeSearch(db, jsonQuery, resultSize, viewName);

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



    private JSONObject executeSearch(DBConnection db, JSONObject jsonQuery, int resultSize, String viewName) throws SQLException {

        String[] searchTerms;
        String searchTermsAsString = "";
        Set<String> allowedDomainsAndSites = new HashSet<>();
        List<SearchResult> foundItems;
        JSONObject resultJson = new JSONObject();

        // Suchbegriffe extrahieren
        JSONArray jsonSearchTerms = jsonQuery.getJSONArray("searchTerms");
        searchTerms = new String[jsonSearchTerms.length()];
        for (int i = 0; i < jsonSearchTerms.length(); i++) {
            String term = jsonSearchTerms.getString(i);
            searchTerms[i] = term;
            searchTermsAsString += (term + " ");
        }

        // Zugelassene Domains/Sites extrahieren
        JSONArray jsonSiteDomainTerms = jsonQuery.getJSONArray("domainSiteTerms");
        for (int i = 0; i < jsonSiteDomainTerms.length(); i++) {
            allowedDomainsAndSites.add(jsonSiteDomainTerms.getString(i));
        }

        // Ergebnisse basierend auf dem View abrufen
        foundItems = db.searchWithView(searchTerms, resultSize, viewName);

        // Wenn keine Ergebnisse gefunden wurden
        if (foundItems.isEmpty()) {
            System.out.println("Keine Begriffe gefunden, die diesen Wörtern entsprechen");
        }

        // Ergebnisliste erstellen
        JSONArray resultList = new JSONArray();
        for (int i = 0; i < foundItems.size(); i++) {
            SearchResult foundItem = foundItems.get(i);
            String itemUrl = foundItem.getUrl();
            boolean shouldAddItem = allowedDomainsAndSites.isEmpty();

            // Überprüfen, ob die URL erlaubt ist
            if (!shouldAddItem) {
                for (String allowedDomain : allowedDomainsAndSites) {
                    if (itemUrl.contains(allowedDomain)) {
                        shouldAddItem = true;
                        break;
                    }
                }
            }

            // Wenn erlaubt, füge das Ergebnis hinzu
            if (shouldAddItem) {
                JSONObject foundItemObject = new JSONObject();
                foundItemObject.put("rank", i + 1);
                foundItemObject.put("url", itemUrl);
                foundItemObject.put("score", foundItem.getScore());
                resultList.put(foundItemObject);
            }
        }

        resultJson.put("resultList", resultList);

        // Query-Objekt erstellen
        JSONObject queryObject = new JSONObject();
        queryObject.put("k", resultSize);
        queryObject.put("query", searchTermsAsString);
        resultJson.put("query", queryObject);

        // Statistiken berechnen und hinzufügen
        JSONArray stat = db.computeStat(searchTerms);
        resultJson.put("stat", stat);

        // CW-Wert hinzufügen
        int cw = db.calcualteCW();
        resultJson.put("cw", cw);

        return resultJson;
    }


}


