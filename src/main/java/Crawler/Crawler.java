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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Crawler {
    DBConnection db;
    String rootUrl;
    int depthToCrawl;
    int nbrToCrawl;
    Boolean allowToLeaveDomains;

    static List<String> allowedDomainsAndSites = Arrays.asList("cs.uni-kl.de", "cs.rptu.de");
    int crawledUrlCount = 0;
    Queue<URLDepthPair> urlQueue = new LinkedList<>();
    Set<String> visitedPages = new HashSet<>();
    ExecutorService threadPool;

    public Crawler(DBConnection db, String rootUrl , int depthToCrawl, int nbrToCrawl, Boolean allowToLeaveDomains) {
        this.db = db;
        this.rootUrl = rootUrl;
        this.depthToCrawl = depthToCrawl;
        this.nbrToCrawl = nbrToCrawl;
        this.allowToLeaveDomains = allowToLeaveDomains;
        this.threadPool = Executors.newFixedThreadPool(10);

        // docId =  = -1 means that the Url does not have any id
        urlQueue.add(new URLDepthPair(-1, rootUrl, 0));
    }


    public void crawl() throws IOException {
        while (!urlQueue.isEmpty() && crawledUrlCount < nbrToCrawl) {

            URLDepthPair urlDepthPair = urlQueue.poll();

            Date d = new Date();
            SimpleDateFormat ft = new SimpleDateFormat("hh:mm:ss");
            System.out.println("Executing Time for link Id " + urlDepthPair.id + " and link url "+urlDepthPair.url + " = " + ft.format(d));


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

        // Check if page is already visited
        if (visitedPages.contains(url)) {
            System.out.println("URL " + url + " is already visited. Skipping.");
            return;
        }
        // Check if the URL exceeds depth
        if (depth > depthToCrawl) {
            System.out.println("URL " + url + " exceeds maximum crawl depth. Skipping.");
            return;
        }
        // Check if the URL is allowed to crawl
        if (!isUrlAllowedToCrawl(url)) {
            return;
        }

        // Add the URL to visited pages and continue with the crawl process
        visitedPages.add(url);
        System.out.println("Crawling URL: " + url + " at depth: " + depth);

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

    // Function that allows verifying if the given URL must be crawled or not
    private boolean isUrlAllowedToCrawl(String url) {
        // If allowed to leave the domain, return true immediately
        if (allowToLeaveDomains) return true;

        String domainRegex = "^(?:https?://)?(?:www\\.)?([^/]+)";
        Pattern pattern = Pattern.compile(domainRegex);
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            String urlDomain = matcher.group(1);
            for (String allowedDomain : allowedDomainsAndSites) {
                if (allowedDomain.contains(urlDomain)) {
                    return true;
                }
            }
            System.out.println("URL " + url + " could not be crawled because its domain "+urlDomain + " is not allowed.");

        }
        return false;
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
