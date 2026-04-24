package Indexer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StopwordsCleaner {
    private static final Set<String> englishStopwords = new HashSet<>();
    private static final Set<String> germanStopwords = new HashSet<>();
    private static final String COMMENT_REGEX = "\\|.*";

    public StopwordsCleaner() throws IOException {
        if (englishStopwords.isEmpty()) {
            System.out.println("Loading English stopwords...");
            loadStopwords("englishStopwords.txt", englishStopwords);
        }
        if (germanStopwords.isEmpty()) {
            System.out.println("Loading German stopwords...");
            loadStopwords("germanStopwords.txt", germanStopwords);
        }
    }

    private void loadStopwords(String filename, Set<String> stopwords) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (inputStream == null) {
                throw new IOException(filename + " not found in JAR or classpath.");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                List<String> lines = reader.lines().collect(Collectors.toList());
                for (String line : lines) {
                    line = line.replaceAll(COMMENT_REGEX, "").trim();
                    if (!line.isEmpty()) {
                        stopwords.add(line.toLowerCase());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading stopwords file: " + e.getMessage());
            throw e;
        }
    }

    public String cleanEnglishWord(String word) {
        return word.replaceAll("[^a-zA-Z]", "").toLowerCase();
    }

    public boolean isEnglishWordValid(String word) {
        return !word.isEmpty() && !englishStopwords.contains(word) && word.length() >= 2 && word.length() <= 15;
    }

    public String cleanGermanWord(String word) {
        return word.replaceAll("[^a-zA-ZäöüÄÖÜß]", "").toLowerCase();
    }

    public boolean isGermanWordValid(String word) {
        return !word.isEmpty() && !germanStopwords.contains(word) && word.length() >= 2 && word.length() <= 15;
    }
}
