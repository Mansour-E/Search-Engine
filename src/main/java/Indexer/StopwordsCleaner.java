package Indexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StopwordsCleaner {
    private static final Set<String> englishStopwords = new HashSet<>();
    private static final Set<String> germnaStopwords = new HashSet<>();
    private static final String COMMENT_REGEX = "\\|.*";

    public StopwordsCleaner() throws IOException {
        // Load stopwords only once for all instances
         if (englishStopwords.isEmpty()) {
            System.out.println("load english stopwords");
            loadStopwords();
        }
        // Load german  only once for all instances
        if (germnaStopwords.isEmpty()) {
            System.out.println("load german stopwords");
            loadGermanStopwords();
        }
    }

    // This function loads stopwords
    private void loadStopwords() throws IOException {
        try {
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources/englishStopwords.txt"));
            for (String line : lines) {
                // Remove comments and trim whitespace
                line = line.replaceAll(COMMENT_REGEX, "").trim();
                if (!line.isEmpty()) {
                    englishStopwords.add(line.toLowerCase());
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading stopwords file: " + e.getMessage());
            throw e;
        }
    }

    private void loadGermanStopwords() throws IOException {
        try {
            List<String> lines = Files.readAllLines(Paths.get("src/main/resources/germanStopwords.txt"));
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty()) {
                    germnaStopwords.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading german stopwords file " + e.getMessage());
            throw e;
        }
    }

    // This function retain only alphabetic characters and convert to lowercase
    public String cleanEnglishWord(String word) {
        return word.replaceAll("[^a-zA-Z]", "").toLowerCase();
    }

    // This function checks if a word is a valid content word (not a stopword)
    public boolean isEnglishWordValid(String word) {
        return !word.isEmpty() && !englishStopwords.contains(word) && word.length() >= 2 && word.length() <= 15;
    }

    // This function cleans a German word by retaining only valid characters
    public String cleanGermanWord(String word) {
        return word.replaceAll("[^a-zA-ZäöüÄÖÜß]", "").toLowerCase();
    }

    // This function checks if a german word is a valid content word (not a stopword)
    public boolean isGermanWordValid(String word) {
        return !word.isEmpty() && !germnaStopwords.contains(word) && word.length() >= 2 && word.length() <= 15;
    }

}
