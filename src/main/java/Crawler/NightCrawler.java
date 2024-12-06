package Crawler;

import DB.DBConnection;
import Indexer.Indexer;
import Sheet2.Classifier.Classifier;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import Crawler.Crawler.URLDepthPair;

public class NightCrawler {
    DBConnection db;
    int depthToCrawl;
    int nbrToCrawl;
    Boolean allowToLeaveDomains;
    int crawlingCounter = 0;

    static List<String> allowedDomainsAndSites = Arrays.asList("cs.uni-kl.de", "cs.rptu.de");
    int crawledUrlCount = 0;
    Queue<URLDepthPair> urlQueue = new LinkedList<>();
    Set<String> visitedPages = new HashSet<>();
    Set<String> allUrlsInDB = new HashSet<>();
    Classifier classifier ;
    String[] rootUrls;

    public NightCrawler(DBConnection db, String[] rootUrls, Boolean allowToLeaveDomains ) throws IOException {
        this.db = db;
        this.classifier = new Classifier();
        this.rootUrls = rootUrls;
        this.allowToLeaveDomains = allowToLeaveDomains;
        this.crawlingCounter = 0;


        // Load visited Pages
        loadVisitedURl();
        System.out.println("visitedPages" + visitedPages);

        // Load existing state from the database if the crawler was interrupted
        loadNotVisitedURL();

        // getAllURls
        loadAllURlsInDB();

        // If the system was not interrupted, initialize the queue with root URLs
        if (urlQueue.isEmpty()) {
            for (String rootUrl : rootUrls) {
                urlQueue.add(new URLDepthPair(-1, rootUrl, 0, "Unknown"));
                // Initial state set to 0 (not visited)
                db.insertIntoCrawledPagesQueue(rootUrl, 0, 0);
            }
        }

    }

    private void loadNotVisitedURL () {
        List<URLDepthPair> queuedUrls = db.getQueuedUrls();
        urlQueue.addAll(queuedUrls);
    }

    private void loadAllURlsInDB () {
        Set<String> notVisitedURLSButExist = db.getAllURLS();
        allUrlsInDB.addAll(notVisitedURLSButExist);
        // this could be improved
        allUrlsInDB.addAll(Arrays.asList(rootUrls));
    }

    private void loadVisitedURl() {
        Set<String> previousVisitedUrls = db.getVisitedUrls();
        visitedPages.addAll(previousVisitedUrls);
    }

    public void crawl() throws IOException {
        while (!urlQueue.isEmpty()) {
            this.crawlingCounter++;
            URLDepthPair urlDepthPair = urlQueue.poll();
            String url = urlDepthPair.url;
            // Check if page is already visited
            if (visitedPages.contains(url)) {
                System.out.println("Crawler.java: URL " + url + " is already visited. Skipping.");
                continue;
            }
            // Check if the URL is allowed to crawl
            else if (!isUrlAllowedToCrawl(url)) {
                System.out.println("Crawler.java: " + url + " not allowed to crawl. Skipping.");
                continue;
            }else{
                db.updateCrawledPageState(url, 1);
                visitedPages.add(url);
                crawledUrlCount++;
                crawlPage(urlDepthPair);
            }
            if(crawlingCounter == 10) {
                db.reCompute();
                this.crawlingCounter = 0;
            }

        }
        db.reCompute();
    }

    private void crawlPage(URLDepthPair urlDepthPair) throws IOException {
        int docId = urlDepthPair.id;
        String url = urlDepthPair.url;
        int depth = urlDepthPair.depth;
        String lang = urlDepthPair.lang;
        System.out.println("Crawling URL: " + url + " at depth: " + depth);

        try {
            // Fetch the page content
            XhtmlConverter xhtmlConverter = new XhtmlConverter(url);
            String htmlContent = xhtmlConverter.convertToXHML();

            // check for the document language
            if(lang.equals("Unknown")) {
                lang = classifier.checkForLanguage(htmlContent);
                urlDepthPair.insertLang(lang);
                System.out.println("url " + url + " hass the language"  + lang);
            }


            // Create document in the database In case of root url
            String crawledDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            if (docId == -1 ) {
                // Insert the document With the correct lang
                System.out.println("crawler.js: insert url " + url + " with lang " + lang);
                docId = db.insertDocument(url, crawledDate, lang);
            }else {
                // Update document language
                db.updateLanguageDocuments(url, lang);
            }
            // Index the page using the Indexer
            Indexer indexer = new Indexer(db, htmlContent, docId, visitedPages, allUrlsInDB, lang);
            indexer.indexHTMlContent();

            // Add child links to the queue for further crawling
            HashMap<Integer, String> childElements = indexer.getLinks();

            for (Integer childId : childElements.keySet()) {
                String childUrl = childElements.get(childId);
                // Check if page is already visited and Check if the URL exceeds depth and Check if the URL is allowed to crawl
                if (!visitedPages.contains(childUrl)  && isUrlAllowedToCrawl(url)) {
                    urlQueue.add(new URLDepthPair(childId, childUrl, depth + 1, "Unknown"));
                    db.insertIntoCrawledPagesQueue(childUrl, depth + 1, 0);

                }
            }
        }catch (Exception e) {
            System.out.println("hjbvkjskjvcsd");
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

}
