package Indexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StopwordsCleaner {
    Set<String> stopwords = new HashSet<>();
    String commentRgx = "\\|.*";

    public  StopwordsCleaner () throws IOException {
        List<String> lines = Files.readAllLines(Paths.get("src/main/java/Indexer/stopwords.txt"));

        for (String line : lines) {
            line = line.replaceAll(commentRgx, "").trim();

            if (!line.isEmpty()) {
                stopwords.add(line);
            }
        }

    }

    public List<String> cleanStopwords(List<String> wordsToBeCleaned) {
        List<String> cleanedWords = new ArrayList<>();

        for (String word : wordsToBeCleaned) {
            // Retain only alphabetic characters
            String cleanedWord = word.replaceAll("[^a-zA-Z]", "");

            if (!cleanedWord.isEmpty() && !stopwords.contains(cleanedWord.toLowerCase())) {
                cleanedWords.add(cleanedWord.toLowerCase());
            }
        }

        return cleanedWords;
    }
}
