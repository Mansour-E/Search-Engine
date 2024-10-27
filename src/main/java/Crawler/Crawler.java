package Crawler;

import DB.DBConnection;
import Indexer.Indexer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Crawler {
    DBConnection db;
    String rootUrl;
    int depthToCrawl;
    int nbrToCrawl;
    Boolean flag;

    int crawledUrlCount = 0;
    Queue<URLDepthPair> urlQueue = new LinkedList<>();
    Set<String> visitedPages = new HashSet<>();
    ExecutorService threadPool;

    public Crawler(DBConnection db, String rootUrl , int depthToCrawl, int nbrToCrawl, Boolean flag) {
        this.db = db;
        this.rootUrl = rootUrl;
        this.depthToCrawl = depthToCrawl;
        this.nbrToCrawl = nbrToCrawl;
        this.flag = flag;
        this.threadPool = Executors.newFixedThreadPool(10);

        // docId =  = -1 means that the Url does not have any id
        urlQueue.add(new URLDepthPair(-1, rootUrl, 0));
    }


    public void crawl() throws IOException {
        while (!urlQueue.isEmpty() && crawledUrlCount < nbrToCrawl) {

            URLDepthPair urlDepthPair = urlQueue.poll();

            Date d = new Date();
            SimpleDateFormat ft = new SimpleDateFormat("hh:mm:ss");
            System.out.println("Executing Time for link Id " + urlDepthPair.id + "and link url "+urlDepthPair.url + " = " + ft.format(d));


            crawlPage(urlDepthPair);

            /*
            // TODO Improve the Multi-threaded crawling
            threadPool.submit(() -> {
                try {
                    crawlPage(urlDepthPair);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            Thread.sleep(1000);
             */
        }

        // threadPool.shutdown();
        // threadPool.awaitTermination(1, TimeUnit.HOURS);
    }

    private void crawlPage(URLDepthPair urlDepthPair) throws IOException {
        int docId = urlDepthPair.id;
        String url = urlDepthPair.url;
        int depth = urlDepthPair.depth;

        if (visitedPages.contains(url) || depth > depthToCrawl) {
            return;
        }

        visitedPages.add(url);
        crawledUrlCount++;
        System.out.println("visitedPages " + visitedPages);
        System.out.println("crawledUrlCount " + crawledUrlCount);


        // Fetch the page content
        XhtmlConverter xhtmlConverter = new XhtmlConverter(url);
        String htmlContent = xhtmlConverter.convertToXHML();

        // Create document in the database
        String crawledDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (docId == -1 ) {
            docId = db.insertDocument(url, crawledDate);
        }else {
            // IDEA
            // update the crawledDate
            // Ad new column called crawled state (crawled, visited)
            // connection.updateDocumentCrawledDate(docId, crawledDate);
        }

        // Index the page using the Indexer
        Indexer indexer = new Indexer(db, htmlContent, docId);
        indexer.indexHTMlContent();

        // Add child links to the queue for further crawling
        HashMap<Integer, String> childElements = indexer.getLinks();

        for (Integer childId : childElements.keySet()) {
            String childUrl = childElements.get(childId);
            if (!visitedPages.contains(childUrl) && depth + 1 <= depthToCrawl) {
                urlQueue.add(new URLDepthPair(childId, childUrl, depth + 1));
            }
        }
    }

    private static class URLDepthPair {
        int id;
        String url;
        int depth;

        URLDepthPair(int id, String url, int depth) {
            this.id = id;
            this.url = url;
            this.depth = depth;
        }
    }
}
