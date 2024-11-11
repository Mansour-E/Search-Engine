package DB;

import org.la4j.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;

import java.sql.*;
import java.util.*;

public class DBConnection {

    private Connection connection ;

    public DBConnection(String dbName, String dbOwner, String dbPassword ) {
        this.connection = this.connectToDb(dbName, dbOwner, dbPassword);
    }

    // Queries For Exercise 1
    public Connection connectToDb(String dbName, String dbOwner, String dbPassword) {
        Connection connection = null;
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/" +dbName, dbOwner, dbPassword);
            if (connection != null) {
                System.out.println("Connection Established");
            } else {
                System.out.println("Connection Failed");
            }

        } catch (Exception e) {
            System.out.println(e);
        }
        return connection;
    }

    public void extendDocumentTable() {
        Statement statement;
        try {
            String query = "ALTER TABLE Documents \n" +
                    "\tADD COLUMN pagerank DOUBLE\n;";

            statement = connection.createStatement();
            statement.executeUpdate(query);
            System.out.println("pagerank column is added");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * calcualte the total nbr of documents
     * create a hashmap of outgoing links for each page
     * create the matrix
     */

    public int calculateCountDocuments() {
        String query = "SELECT COUNT (DISTINCT id) from (\n" +
                "SELECT from_docid AS id FROM links\n" +
                "UNION \n" +
                "SELECT to_docid AS id FROM links\n" +
                ") AS uniquesIDS";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)){
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Matrix createLinkMatrix(double tp) {

        String query = "SELECT from_docid, to_docid FROM links";
        Map<Integer, List<Integer>> linkMap = new HashMap<>();

        // Create a map of outgoing links for each page
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                int from = rs.getInt("from_docid") - 1;
                int to = rs.getInt("to_docid") - 1;
                linkMap.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Create the initial matrix
        int n = this.calculateCountDocuments();
        Matrix linkMatrix = new Basic2DMatrix(n, n);

        double teleportationProb = tp / n;

        for (int i = 0; i < n; i++) {
            List<Integer> outgoingLinks = linkMap.getOrDefault(i, new ArrayList<>());
            int outDegree = outgoingLinks.size();

            if (outDegree == 0) {
                // Dangling node: distribute teleportation probability equally across all nodes
                for (int j = 0; j < n; j++) {
                    linkMatrix.set(j, i, teleportationProb);
                }
            } else {
                // Page with outgoing links: apply both link probability and teleportation probability
                double linkProb = (1 - tp) / outDegree;
                for (int j = 0; j < n; j++) {
                    if (outgoingLinks.contains(j)) {
                        System.out.println("i " + i + " j " + j);

                        // Link probability + teleportation probability
                        linkMatrix.set(j, i, linkProb + teleportationProb);
                    } else {
                        // Teleportation probability only
                        linkMatrix.set(j, i, teleportationProb);
                    }
                }
            }
        }
        return linkMatrix;
    }


}




