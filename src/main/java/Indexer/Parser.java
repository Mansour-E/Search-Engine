package Indexer;

import com.shekhargulati.urlcleaner.UrlCleaner;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class Parser {

    private Document doc;
    private StopwordsCleaner stopwordsCleaner;

    public Parser(String htmlContent) throws IOException {
        this.stopwordsCleaner = new StopwordsCleaner();
        this.doc = Jsoup.parse(htmlContent);
        cleanDocument();
    }

    private void cleanDocument() {
        // Remove problematic tags like <path>
        doc.select("path").remove();
    }

    public static String stemWord(String word) {
        Stemmer stemmer = new Stemmer();
        char[] charArray = word.toCharArray();
        stemmer.add(charArray, charArray.length);
        stemmer.stem();
        return stemmer.toString().toLowerCase();
    }

    public HashMap<String, Integer> parseContent(String lang) {
        HashMap<String, Integer> termWithCountElements = new HashMap<>();

        Elements bodyContent = doc.select("body");
        for (Element element : bodyContent) {
            String[] terms = element.text().split("\\W+");
            for (String term : terms) {
                if (lang.equals("English")) {
                    String cleanedWord = stopwordsCleaner.cleanEnglishWord(term);
                    if (stopwordsCleaner.isEnglishWordValid(cleanedWord)) {
                        String stemmedTerm = stemWord(term);
                        termWithCountElements.put(stemmedTerm, termWithCountElements.getOrDefault(stemmedTerm, 0) + 1);
                    }
                } else if (lang.equals("German")) {
                    String cleanedWord = stopwordsCleaner.cleanGermanWord(term);
                    if (stopwordsCleaner.isGermanWordValid(cleanedWord)) {
                        termWithCountElements.put(cleanedWord, termWithCountElements.getOrDefault(cleanedWord, 0) + 1);
                    }
                }
            }
        }
        return termWithCountElements;
    }

    public List<String> parseLinks(Set<String> visitedPages) {
        List<String> linkElements = new ArrayList<>();
        Elements linksContent = doc.select("a[href]");
        for (Element element : linksContent) {
            String link = element.attr("href");
            if (!visitedPages.contains(link) && isValidUrl(link)) {
                linkElements.add(link);
            }
        }
        return linkElements;
    }

    private boolean isValidUrl(String url) {
        String urlRegex = "^(https?:\\/\\/)?([\\w.-]+)\\.([a-z\\.]{2,6})([\\/\\w\\.-]*)*\\/?$";
        return Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE).matcher(url).matches();
    }
}
