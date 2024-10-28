package Indexer;

import DB.DBConnection;
import org.example.Main;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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


/**
 * Parse the text contained in the HTML document
 * Eliminate Stopwords/HTML markup
 * Extract meta data
 * Save Informations in the table
 * **/

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
            db.insertFeature(rootDocID, term, frequency);
        }

        // TODO calculate the Score for each item
        // TODO we can use Threds to calculate the The score and seperatley add the links into the table

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

}





