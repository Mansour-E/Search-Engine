package CommandInterface;
import DB.DBConnection;
import java.util.List;
public class commandInterface {
    public static List<SearchResult> executeSearch(DBConnection db, String[] searchTerms, boolean isConjunctive, int resultSize) {
        List<SearchResult> foundItems;
        String[] lang = new String[]{"English", "German"};
        if (isConjunctive) {
            foundItems = db.conjuntiveCrawling(searchTerms, resultSize, List.of(lang), "tfidf");
        } else {
            foundItems = db.disjunctiveCrawling(searchTerms, resultSize, List.of(lang), "tfidf");
        }
        if (foundItems.isEmpty()) {
            System.out.println("No terms found that match these words.");
        }
        for (int i = 0; i < foundItems.size(); i++) {
            SearchResult foundItem = foundItems.get(i);
            System.out.println("rank " + (i + 1) + ": " + foundItem.getUrl() + " (Score: " + foundItem.getScore() + ")");
        }
        return foundItems;
    }
}
