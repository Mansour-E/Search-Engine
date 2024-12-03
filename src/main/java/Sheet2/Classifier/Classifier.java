package Sheet2.Classifier;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Classifier {

    private static final Set<String> englishWords = new HashSet<>();
    private static final Set<String> germanWords = new HashSet<>();
    private String englishWordsFilePath = "EnglishWords.txt";
    private String germanWordsFilePath = "GermanWords.txt";

    // Initial number of terms to analyze
    private static final int MIN_TERMS = 20;
    // 75% match confidence
    private static final double CONFIDENCE_THRESHOLD = 0.75;

    public Classifier() throws IOException {
        // Load English words only once for all instances
        if (englishWords.isEmpty()) {
            synchronized (englishWords) {
                if (englishWords.isEmpty()) {
                    System.out.println("Loading English dictionary...");
                    loadWordsFromFile(englishWordsFilePath, "en");
                }
            }
        }

        // Load German words only once for all instances
        if (germanWords.isEmpty()) {
            synchronized (germanWords) {
                if (germanWords.isEmpty()) {
                    System.out.println("Loading German dictionary...");
                    loadWordsFromFile(germanWordsFilePath, "de");
                }
            }
        }
    }

    private void loadWordsFromFile(String filePath, String lang) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("File not found in resources: " + filePath);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    if (lang.equals("en")) {
                        englishWords.add(line);
                    } else if (lang.equals("de")) {
                        germanWords.add(line);
                    } else {
                        throw new IllegalArgumentException("Unsupported language code: " + lang);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading dictionary file: " + filePath + " - " + e.getMessage());
            throw e;
        }
    }


    public String checkForLanguage(String htmlContent) {

        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return "Unknown";
        }

        int englishMatches = 0;
        int germanMatches = 0;

        Document doc = Jsoup.parse(htmlContent);
        Elements bodyContent = doc.selectXpath("/html/body");

        for (Element element : bodyContent) {
            String[] terms = element.text().split("\\W+");
            for (String term : terms) {
                if (englishWords.contains(term)) {
                    englishMatches++;
                }
                if (germanWords.contains(term)) {
                    germanMatches++;
                }
            }

            // Calculate confidence
            double totalMatches = englishMatches + germanMatches;
            if (totalMatches > 0) {
                double englishConfidence = (double) englishMatches / totalMatches;
                double germanConfidence = (double) germanMatches / totalMatches;

                // Break early if a confident classification is achieved
                if (englishConfidence >= CONFIDENCE_THRESHOLD || germanConfidence >= CONFIDENCE_THRESHOLD) {
                    break;
                }
            }
        }

        // Final classification
        if (englishMatches > germanMatches) {
            return "English";
        } else if (germanMatches > englishMatches) {
            return "German";
        }
        return "Unknown";
    }

}
