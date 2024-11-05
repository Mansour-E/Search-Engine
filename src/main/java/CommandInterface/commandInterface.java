package CommandInterface;

import DB.DBConnection;

import java.util.List;


public class commandInterface {


    public static List<SearchResult> executeSearch (DBConnection db, String[] searchTerms, boolean isConjuctive, int resultSize ) {
        List<SearchResult> foundItems;
        if(isConjuctive) {
            foundItems = db.conjuntiveCrawling (searchTerms,resultSize);
        }else {
            foundItems = db.disjunctiveCrawling(searchTerms,resultSize);
        }


        if(foundItems.isEmpty()) {
            System.out.println("there are noterms that match these words");
        }
        for (int i = 0; i < foundItems.size(); i++) {
            SearchResult foundItem = foundItems.get(i);
            System.out.println("rank " + (i+1) + ": " + foundItem.getUrl() + " (Score: " + foundItem.getScore() + ")");
        }

        return foundItems;
    }

}
