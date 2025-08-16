package Sheet2.PageRank;

import DB.DBConnection;
import org.la4j.Matrix;
import org.la4j.vector.dense.BasicVector;

import java.util.Vector;

public class PageRank {

    private static final double TELEPORT_PROBABILITY = 0.1;
    private static final int trials = 2 ;

    public void calculatePageRanking(DBConnection db) {
        Matrix linkMatrix = db.createLinkMatrix(TELEPORT_PROBABILITY);


        int n = linkMatrix.rows();
        BasicVector rank = BasicVector.constant(n, 1.0 / n);

        for (int t = 0; t < trials; t++) {
            rank = (BasicVector) linkMatrix.multiply(rank);
        }

        // For Debugging
        /*
        Vector firstRow = linkMatrix.getRow(0);
        for (int i = 0; i < firstRow.length(); i++) {
            // System.out.println(firstRow.get(i));
        }
           for (int i = 0; i < rank.length(); i++) {
            System.out.printf("rank" + rank.get(i));
        }
         */

        db.insertPageRanking(rank);

    }

}

