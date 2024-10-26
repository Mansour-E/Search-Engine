package Indexer;

import DB.DBConnection;
import org.example.Main;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.print.Doc;
import java.io.IOException;
import java.rmi.dgc.DGC;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
        this.parser = new Parser();
        this.rootDocID = docId;
    }


    public void indexHTMlContent () throws IOException {
        List<String> linkElements= parser.parseLinks(htmlContent);
        HashMap<String, Integer> termElements = parser.parseContent(htmlContent);

        // add the Terms to featruesTable
        for (String term : termElements.keySet() ) {
            db.insertFeature(rootDocID, term, termElements.get(term));
        }

        // add the Links to LinksTable;
        for(String link: linkElements) {
            // Create child-document in the Documents Table
            String createdDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            int docId = db.insertDocument(link, createdDate);

            // Insert child-document in the Links Table
            db.insertLink(rootDocID, docId);
            linkAndDocIdElements.put(docId, link);
        }

    }

    public HashMap<Integer, String> getLinks() {
        return linkAndDocIdElements;
    }

}





