package Sheet2.PageRank;

import DB.DBConnection;
import org.la4j.Matrix;
import org.la4j.vector.dense.BasicVector;

public class PageRank {

    private static final double TELEPORT_PROBABILITY = 0.1;
    private static final double CONVERGENCE_THRESHOLD = 1e-6;
    private static final int MAX_ITERATIONS = 100;

    public void calculatePageRanking(DBConnection db) {
        Matrix linkMatrix = db.createLinkMatrix(TELEPORT_PROBABILITY);
        int n = linkMatrix.rows();

        if (n == 0) {
            System.out.println("PageRank: No documents found, skipping.");
            return;
        }

        BasicVector rank = BasicVector.constant(n, 1.0 / n);

        for (int t = 0; t < MAX_ITERATIONS; t++) {
            BasicVector newRank = (BasicVector) linkMatrix.multiply(rank);

            double delta = 0.0;
            for (int i = 0; i < n; i++) {
                delta += Math.abs(newRank.get(i) - rank.get(i));
            }
            rank = newRank;

            if (delta < CONVERGENCE_THRESHOLD) {
                System.out.println("PageRank converged after " + (t + 1) + " iterations.");
                break;
            }
        }

        db.insertPageRanking(rank);
    }
}
