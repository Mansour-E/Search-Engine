package Indexer;

import com.shekhargulati.urlcleaner.UrlCleaner;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.text.html.HTMLDocument;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    Document doc;
    StopwordsCleaner stopwordsCleaner;
    List<String> linkElements= new ArrayList<String>();

    public Parser(String htmlContent) throws IOException {
        this.stopwordsCleaner =  new StopwordsCleaner();
        doc = Jsoup.parse(htmlContent);
    }


    // Function to stem a word
    public static String stemWord(String word) {
        Stemmer stemmer = new Stemmer();
        char[] charArray = word.toCharArray();
        stemmer.add(charArray, charArray.length);
        stemmer.stem();
        return stemmer.toString().toLowerCase();
    }

    // Function to extract HTML content and return terms with their frequency
    public HashMap<String, Integer> parseContent(String lang) throws IOException {
        HashMap<String, Integer> termWithCountElements = new HashMap<>();

        // Extract content from <body> only
        Elements bodyContent = doc.selectXpath("/html/body");
        for (Element element : bodyContent) {
            String[] terms = element.text().split("\\W+");
            for (String term : terms) {
                if(lang.equals("English")) {
                    // Clean the word
                    String cleanedWord = stopwordsCleaner.cleanEnglishWord(term);
                    // Remove word if it is a stopword
                    if (stopwordsCleaner.isEnglishWordValid(cleanedWord)) {
                        // Apply stemming
                        String stemmedTerm = stemWord(term);
                        // Regroup terms
                        termWithCountElements.put(stemmedTerm, termWithCountElements.getOrDefault(stemmedTerm, 0) + 1);
                    }
                } else if (lang.equals("German")) {
                    // Clean the word
                    String cleanedWord = stopwordsCleaner.cleanGermanWord(term);
                    // Remove word if it is a stopword
                    if (stopwordsCleaner.isGermanWordValid(cleanedWord)) {
                        // Regroup terms
                        termWithCountElements.put(cleanedWord, termWithCountElements.getOrDefault(cleanedWord, 0) + 1);
                    }
                }

            }
        }

        return termWithCountElements;
    }

    // Function to extract and clean links from HTML content
    public List<String> parseLinks(Set<String> visitedPages) {
        List<String> linkElements = new ArrayList<>();

        // Extract only <a> tags with href attribute
        Elements linksContent = doc.selectXpath("/html/body//a");
        for (Element element : linksContent) {
            String link = element.attr("href");
            if (isValidUrl(link) && !visitedPages.contains(link) ) {
                // Clean the Link: I remove it, because it takes a lot of time.
                // String cleanedLink = UrlCleaner.unshortenUrl(link);
                linkElements.add(link);
            }else {
                System.out.println("Parser.java: URl " + link + " is already visited or not valid. Skipping");
            }
        }

        return linkElements;
    }

    // This function to check if the given URL is valid
    private boolean isValidUrl(String url) {
        String urlRegex = "^(https?:\\/\\/)?([\\w.-]+)\\.([a-z\\.]{2,6})([\\/\\w\\.-]*)*\\/?$";
        Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(url);

        return matcher.matches();
    }

}
