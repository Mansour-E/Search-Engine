package Indexer;

import DB.DBConnection;
import Sheet2.Classifier.Classifier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Indexer {
    DBConnection db;
    String htmlContent;
    Parser parser;
    int rootDocID;
    Classifier classifier;
    String lang;
    Set<String> visitedPages;
    Set<String> allUrlsInDB;

    List<String> linkElements;
    HashMap<Integer, String> linkAndDocIdElements = new HashMap<>();

    public Indexer(DBConnection db, String htmlContent, int docId, Set<String> visitedPages, Set<String> allUrlsInDB, String lang) throws IOException {
        this.db = db;
        this.htmlContent = htmlContent;
        this.parser = new Parser(htmlContent);
        this.classifier = new Classifier();
        this.lang = lang;
        this.rootDocID = docId;
        this.visitedPages = visitedPages;
        this.allUrlsInDB = allUrlsInDB;
    }

    public void indexHTMlContent() throws IOException {
        HashMap<String, Integer> termFrequencies = parser.parseContent(lang);
        List<String> parsedLinks = parser.parseLinks(visitedPages);
        String createdDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        for (Map.Entry<String, Integer> entry : termFrequencies.entrySet()) {
            db.insertFeature(rootDocID, entry.getKey(), entry.getValue());
        }

        for (String link : parsedLinks) {
            if (!allUrlsInDB.contains(link)) {
                int docId = db.insertDocument(link, createdDate, "Unknown");
                db.insertLink(rootDocID, docId);
                linkAndDocIdElements.put(docId, link);
                allUrlsInDB.add(link);
            }
        }
    }

    public HashMap<Integer, String> getLinks() {
        return linkAndDocIdElements;
    }
}
