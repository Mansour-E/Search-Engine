package CommandInterface;

import DB.DBConnection;

import java.util.List;

/*
//*  from input console:
    connection with DB , take DBName, DBOwner, DBPassword
    take the terms search and if the query should be conjuctive/disjunctive
    display the result (top5, rank, url, achieved score)
*/
public class commandInterface {


    public static List<SearchResult> executeSearch (DBConnection db, String[] searchTerms, boolean isConjuctive, int resultSize ) {
        List<SearchResult> foundItems;
        if(isConjuctive) {
            foundItems = db.conjuntiveCrawling (searchTerms,resultSize);
        }else {
            foundItems = db.disjunctiveCrawling(searchTerms,resultSize);
        }


        if(foundItems.size() == 0) {
            System.out.println("there are noterms that match these words");
        }
        for (int i = 0; i < foundItems.size(); i++) {
            SearchResult foundItem = foundItems.get(i);
            System.out.println("rank " + (i+1) + ": " + foundItem.getUrl() + " (Score: " + foundItem.getScore() + ")");
        }

        return foundItems;
    }

}
