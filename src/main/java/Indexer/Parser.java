package Indexer;

import com.shekhargulati.urlcleaner.UrlCleaner;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.text.html.HTMLDocument;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Parser {

    StopwordsCleaner stopwordsCleaner;
    List<String> linkElements= new ArrayList<String>();

    public Parser() throws IOException {
        this.stopwordsCleaner =  new StopwordsCleaner();
    }


    public static List<String> stemWords(List<String> words) {
        Stemmer stemmer = new Stemmer();
        List<String> stemmedWords = new ArrayList<>();

        for (String word : words) {
            char[] charArray = word.toCharArray();
            stemmer.add(charArray, charArray.length);
            stemmer.stem();
            stemmedWords.add(stemmer.toString());
        }

        return stemmedWords;
    }

    public HashMap<String, Integer> parseContent(String xhtmlString) throws IOException {
        //extract text content(Remove a Tags and other Tags)
        String noLinksContent = xhtmlString.replaceAll("<a\\s+[^>]*>(.*?)</a>", "");
        String termsContent = noLinksContent.replaceAll("<[^>]*>", "").trim();

        // split words
        List<String> termWords = new ArrayList<>(Arrays.asList(termsContent.split("\\s+")));

        // Remove stopwords
        List<String> cleanedWords = stopwordsCleaner.cleanStopwords(termWords);

        // Perform stemming
        List<String> stemmedWords = stemWords(cleanedWords);

        // Regroup terms
        HashMap<String, Integer> termWithCountElements = new HashMap<>();
        for (String word : stemmedWords) {
            if (termWithCountElements.containsKey(word)) {
                termWithCountElements.put(word, termWithCountElements.get(word) + 1);
            } else {
                termWithCountElements.put(word, 1);
            }
        }

        return termWithCountElements;
    }

    public List<String> parseLinks(String xhtmlString) throws IOException {

        List<String> linkElements = new ArrayList<>();
        String linkRegex = "<a\\s+(?:[^>]*?\\s+)?href=\"(https?://[^\"]*)\"";
        Pattern pattern = Pattern.compile(linkRegex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(xhtmlString);

        while (matcher.find()) {
            String linkHref = matcher.group(1);
            try {
                // clean the Link
                String cleanedLink = UrlCleaner.unshortenUrl(linkHref);
                linkElements.add( cleanedLink);

            } catch (IOException e) {
                System.err.println("Error unshortening URL: " + e.getMessage());
                // throw e;
            }
        }

        return linkElements;
    }


}
