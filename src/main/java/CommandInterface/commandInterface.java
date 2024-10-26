package CommandInterface;

import DB.DBConnection;
import java.util.ArrayList;
import java.util.List;

/*
//*  from input console:
    connection with DB , take DBName, DBOwner, DBPassword
    take the terms search and if the query should be conjuctive/disjunctive
    display the result (top5, rank, url, achieved score)
*/
public class commandInterface {


    public static void executeSearch (DBConnection db, String[] searchTerms, boolean isConjuctive, int resultSize ) {
        List<SearchResult> foundItemms = new ArrayList<>();
        if(isConjuctive) {
            foundItemms = db.conjuntiveCrawling (searchTerms,resultSize);
        }else {
            foundItemms = db.disjunctiveCrawling(searchTerms,resultSize);
        }

    }

}
