package Indexer;

import DB.DBConnection;
import org.example.Main;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.sql.*;

import javax.print.Doc;
import java.io.IOException;
import java.rmi.dgc.DGC;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.shekhargulati.urlcleaner.UrlCleaner;

public class Indexer {
    DBConnection db;
    String htmlContent;
    Parser parser;
    int rootDocID;

    List<String> linkElements;
    HashMap<Integer, String> linkAndDocIdElements = new HashMap<Integer, String>();

    public Indexer(DBConnection db, String htmlContent, int docId) throws IOException {
        this.db = db;
        this.htmlContent = htmlContent;
        this.parser = new Parser(htmlContent);
        this.rootDocID = docId;
    }


    // Function to Parse terms and links from the HTML content and store them into Db
    public void indexHTMlContent() throws IOException {
        HashMap<String, Integer> termFrequencies = parser.parseContent();
        List<String> parsedLinks = parser.parseLinks();
        String createdDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // Insert terms into features table
        for (Map.Entry<String, Integer> entry : termFrequencies.entrySet()) {
            String term = entry.getKey();
            int frequency = entry.getValue();
            db.insertFeature(rootDocID, term, frequency );
        }

        // Insert links into documents and links tables
        for (String link : parsedLinks) {
            int docId = db.insertDocument(link, createdDate);
            db.insertLink(rootDocID, docId);
            linkAndDocIdElements.put(docId, link);
        }
    }

    public HashMap<Integer, String> getLinks() {
        return linkAndDocIdElements;
    }

    /**
     * Calculates BM25 scores and updates the features table.
     */
    public void calculateBM25() {
        try {
            // Document lengths abrufen
            HashMap<Integer, Integer> documentLengths = new HashMap<>();
            String docStatsQuery = "SELECT docid, document_length FROM document_length_view";
            try (ResultSet statsResult = db.executeQuery(docStatsQuery)) {
                while (statsResult.next()) {
                    int docId = statsResult.getInt("docid");
                    int documentLength = statsResult.getInt("document_length");
                    documentLengths.put(docId, documentLength);
                }
            }

            // Durchschnittliche Dokumentlänge abrufen
            String avgLengthQuery = "SELECT average_length FROM average_length_view";
            double avgDocumentLength = 0.0;
            try (ResultSet avgLengthResult = db.executeQuery(avgLengthQuery)) {
                if (avgLengthResult.next()) {
                    avgDocumentLength = avgLengthResult.getDouble("average_length");
                }
            }

            if (avgDocumentLength == 0) {
                System.err.println("Durchschnittliche Dokumentlänge ist 0. Abbruch der Berechnung.");
                return;
            }

            // BM25-Werte berechnen
            double k1 = 1.2;
            double b = 0.75;
            String featuresQuery = "SELECT docid, term, term_frequency FROM features";
            try (ResultSet featuresResult = db.executeQuery(featuresQuery)) {
                while (featuresResult.next()) {
                    int docId = featuresResult.getInt("docid");
                    String term = featuresResult.getString("term");
                    int termFrequency = featuresResult.getInt("term_frequency");

                    int docLength = documentLengths.getOrDefault(docId, 0);
                    double numerator = termFrequency * (k1 + 1);
                    double denominator = termFrequency + k1 * (1 - b + b * (docLength / avgDocumentLength));
                    double bm25Score = numerator / denominator;

                    // BM25-Wert aktualisieren
                    String updateQuery = "UPDATE features SET bm25_score = ? WHERE docid = ? AND term = ?";
                    try (PreparedStatement pstmt = db.getConnection().prepareStatement(updateQuery)) {
                        pstmt.setDouble(1, bm25Score);
                        pstmt.setInt(2, docId);
                        pstmt.setString(3, term);
                        pstmt.executeUpdate();
                    }
                }
            }

            System.out.println("BM25 scores erfolgreich berechnet und in der Tabelle 'features' aktualisiert.");
        } catch (SQLException e) {
            System.err.println("Fehler bei der BM25-Berechnung: " + e.getMessage());
            e.printStackTrace();
        }
    }

}





