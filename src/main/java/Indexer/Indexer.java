package Indexer;

import DB.DBConnection;
import Sheet2.Classifier.Classifier;
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
    Classifier classifier;
    String lang;
    Set<String> visitedPages;

    List<String> linkElements;
    HashMap<Integer, String> linkAndDocIdElements = new HashMap<Integer, String>();

    public Indexer(DBConnection db, String htmlContent, int docId,Set<String> visitedPages, String lang) throws IOException {
        this.db = db;
        this.htmlContent = htmlContent;
        this.parser = new Parser(htmlContent);
        this.classifier = new Classifier();
        this.lang = lang;
        this.rootDocID = docId;
        this.visitedPages = visitedPages;
    }


    // Function to Parse terms and links from the HTML content and store them into Db
    public void indexHTMlContent() throws IOException {
        HashMap<String, Integer> termFrequencies = parser.parseContent(lang);
        List<String> parsedLinks = parser.parseLinks(visitedPages);
        String createdDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // Insert terms into features table
        for (Map.Entry<String, Integer> entry : termFrequencies.entrySet()) {
            String term = entry.getKey();
            int frequency = entry.getValue();
            db.insertFeature(rootDocID, term, frequency );
        }

        // Insert links into documents and links tables
        for (String link : parsedLinks) {
            // Bug come from here . i must check if the url is the DB or Not ?
            int docId = db.insertDocument(link, createdDate, "Unknown");
            db.insertLink(rootDocID, docId);
            linkAndDocIdElements.put(docId, link);
        }
    }

    public HashMap<Integer, String> getLinks() {
        return linkAndDocIdElements;
    }

    // In Indexer.java
    public void calculateBM25() throws SQLException {
        db.calculateBM25InDatabase();
    }

}





