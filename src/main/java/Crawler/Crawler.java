package Crawler;

import DB.DBConnection;
import Indexer.Indexer;
import Sheet2.Classifier.Classifier;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Crawler {

    private final DBConnection db;
    private final int depthToCrawl;
    private final int nbrToCrawl;
    private final boolean allowToLeaveDomains;

    // Rate limiting: minimum ms between requests to the same domain
    private static final long CRAWL_DELAY_MS = 500;
    private final Map<String, Long> domainLastAccess = new HashMap<>();

    static List<String> allowedDomainsAndSites = Arrays.asList("cs.uni-kl.de", "cs.rptu.de");

    int crawledUrlCount = 0;
    Queue<URLDepthPair> urlQueue = new LinkedList<>();
    Set<String> visitedPages = new HashSet<>();
    Set<String> allUrlsInDB = new HashSet<>();
    ExecutorService threadPool;
    Classifier classifier;
    String[] rootUrls;

    public Crawler(DBConnection db, String[] rootUrls, int depthToCrawl, int nbrToCrawl, boolean allowToLeaveDomains) throws IOException {
        this.db = db;
        this.depthToCrawl = depthToCrawl;
        this.nbrToCrawl = nbrToCrawl;
        this.allowToLeaveDomains = allowToLeaveDomains;
        this.threadPool = Executors.newFixedThreadPool(10);
        this.classifier = new Classifier();
        this.rootUrls = rootUrls;

        loadVisitedURl();
        System.out.println("Visited pages loaded: " + visitedPages.size());

        loadNotVisitedURL();
        loadAllURlsInDB();

        if (urlQueue.isEmpty()) {
            for (String rootUrl : rootUrls) {
                urlQueue.add(new URLDepthPair(-1, rootUrl, 0, "Unknown"));
                db.insertIntoCrawledPagesQueue(rootUrl, 0, 0);
            }
        }
    }

    private void loadNotVisitedURL() {
        List<URLDepthPair> queuedUrls = db.getQueuedUrls();
        urlQueue.addAll(queuedUrls);
    }

    private void loadAllURlsInDB() {
        Set<String> existing = db.getAllURLS();
        allUrlsInDB.addAll(existing);
        allUrlsInDB.addAll(Arrays.asList(rootUrls));
    }

    private void loadVisitedURl() {
        Set<String> previousVisitedUrls = db.getVisitedUrls();
        visitedPages.addAll(previousVisitedUrls);
    }

    public void crawl() throws IOException {
        while (!urlQueue.isEmpty() && crawledUrlCount < nbrToCrawl) {
            URLDepthPair urlDepthPair = urlQueue.poll();
            String url = urlDepthPair.url;

            if (visitedPages.contains(url)) {
                System.out.println("Already visited, skipping: " + url);
                continue;
            } else if (urlDepthPair.depth > depthToCrawl) {
                System.out.println("Exceeds max depth, skipping: " + url);
                continue;
            } else if (!isUrlAllowedToCrawl(url)) {
                System.out.println("Domain not allowed, skipping: " + url);
                continue;
            }

            // Rate limiting per domain
            applyDomainDelay(url);

            db.updateCrawledPageState(url, 1);
            visitedPages.add(url);
            crawledUrlCount++;
            crawlPage(urlDepthPair);
        }
        db.reCompute();
    }

    private void applyDomainDelay(String url) {
        String domain = extractDomain(url);
        long now = System.currentTimeMillis();
        Long lastAccess = domainLastAccess.get(domain);
        if (lastAccess != null) {
            long elapsed = now - lastAccess;
            if (elapsed < CRAWL_DELAY_MS) {
                try {
                    Thread.sleep(CRAWL_DELAY_MS - elapsed);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        domainLastAccess.put(domain, System.currentTimeMillis());
    }

    private String extractDomain(String url) {
        Matcher m = Pattern.compile("^(?:https?://)?(?:www\\.)?([^/]+)").matcher(url);
        return m.find() ? m.group(1) : url;
    }

    private void crawlPage(URLDepthPair urlDepthPair) {
        int docId = urlDepthPair.id;
        String url = urlDepthPair.url;
        int depth = urlDepthPair.depth;
        String lang = urlDepthPair.lang;
        System.out.println("Crawling: " + url + " (depth=" + depth + ")");

        try {
            XhtmlConverter xhtmlConverter = new XhtmlConverter(url);
            String htmlContent = xhtmlConverter.convertToXHML();

            if (lang.equals("Unknown")) {
                lang = classifier.checkForLanguage(htmlContent);
                urlDepthPair.insertLang(lang);
            }

            String crawledDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            if (docId == -1) {
                docId = db.insertDocument(url, crawledDate, lang);
            } else {
                db.updateLanguageDocuments(url, lang);
            }

            Indexer indexer = new Indexer(db, htmlContent, docId, visitedPages, allUrlsInDB, lang);
            indexer.indexHTMlContent();

            HashMap<Integer, String> childElements = indexer.getLinks();
            for (Integer childId : childElements.keySet()) {
                String childUrl = childElements.get(childId);
                if (!visitedPages.contains(childUrl) && depth + 1 <= depthToCrawl && isUrlAllowedToCrawl(childUrl)) {
                    urlQueue.add(new URLDepthPair(childId, childUrl, depth + 1, "Unknown"));
                    db.insertIntoCrawledPagesQueue(childUrl, depth + 1, 0);
                }
            }
        } catch (Exception e) {
            System.err.println("Error crawling " + url + ": " + e.getMessage());
        }
    }

    public boolean isUrlAllowedToCrawl(String url) {
        if (allowToLeaveDomains) return true;

        Matcher matcher = Pattern.compile("^(?:https?://)?(?:www\\.)?([^/]+)").matcher(url);
        if (matcher.find()) {
            String urlDomain = matcher.group(1);
            for (String allowedDomain : allowedDomainsAndSites) {
                if (urlDomain.contains(allowedDomain) || allowedDomain.contains(urlDomain)) {
                    return true;
                }
            }
            System.out.println("Domain not allowed: " + urlDomain);
        }
        return false;
    }

    public static class URLDepthPair {
        int id;
        String url;
        int depth;
        String lang;

        public URLDepthPair(int id, String url, int depth, String lang) {
            this.id = id;
            this.url = url;
            this.depth = depth;
            this.lang = lang;
        }

        public void insertLang(String lang) {
            this.lang = lang;
        }
    }
}
