package Indexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StopwordsCleaner {
    private static final Set<String> stopwords = new HashSet<>();
    private static final String COMMENT_REGEX = "\\|.*";

    public StopwordsCleaner() throws IOException {
        // Load stopwords only once for all instances
        if (stopwords.isEmpty()) {
            System.out.println("load stopwords from stopwords.txt file");
            loadStopwords();
        }
    }

    // This function loads stopwords
    private void loadStopwords() throws IOException {
        try {
            List<String> lines = Files.readAllLines(Paths.get("src/main/java/Indexer/stopwords.txt"));
            for (String line : lines) {
                // Remove comments and trim whitespace
                line = line.replaceAll(COMMENT_REGEX, "").trim();
                if (!line.isEmpty()) {
                    stopwords.add(line.toLowerCase());
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading stopwords file: " + e.getMessage());
            throw e;
        }
    }

    // This function retain only alphabetic characters and convert to lowercase
    public String cleanWord(String word) {
        return word.replaceAll("[^a-zA-Z]", "").toLowerCase();
    }

    // This function checks if a word is a valid content word (not a stopword)
    public boolean isValid(String word) {
        return !word.isEmpty() && !stopwords.contains(word) && word.length() >= 2 && word.length() <= 10;
    }
}
