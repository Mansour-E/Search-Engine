package Sheet2.Classifier;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Classifier {

    private static final Set<String> englishWords = new HashSet<>();
    private static final Set<String> germanWords = new HashSet<>();

    private String englishWordsFilePath = "src/main/java/Sheet2/Classifier/EnglishWords";
    private String germanWordsFilePath = "src/main/java/Sheet2/Classifier/GermanWords";

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
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            for (String line : lines) {
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

    public boolean isEnglishWord(String word) {
        return englishWords.contains(word);
    }

    public boolean isGermanWord(String word) {
        return germanWords.contains(word);
    }
}
