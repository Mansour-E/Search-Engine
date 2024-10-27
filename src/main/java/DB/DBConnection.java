package DB;
import CommandInterface.SearchResult;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static Indexer.Parser.stemWord;


public class DBConnection {

    private Connection connection ;

    public DBConnection(String dbName, String dbOwner, String dbPassword) {
        this.connection = this.connectToDb(dbName, dbOwner, dbPassword);
        createTables();
        initializeSchema();
    }
    // Queries for Exercise 2
    private void initializeSchema() {
        try (Statement stmt = connection.createStatement()) {
            // Add new columns with default values if they don't already exist
            stmt.executeUpdate("ALTER TABLE features ADD COLUMN IF NOT EXISTS tf REAL DEFAULT 0");
            stmt.executeUpdate("ALTER TABLE features ADD COLUMN IF NOT EXISTS idf REAL DEFAULT 0");
            stmt.executeUpdate("ALTER TABLE features ADD COLUMN IF NOT EXISTS tfidf REAL DEFAULT 0");

            // Create index on term for efficient TF*IDF calculations
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_term_features ON features (term)");
        } catch (Exception e) {
            System.err.println("Error initializing database schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Queries For Exercise 1
    public Connection connectToDb( String dbName, String dbOwner, String dbPassword) {

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

    //zum 2te Aufgabe
    public void calculateTF() {
        String updateTFQuery = """
            UPDATE features
            SET tf = CASE
                WHEN term_frequency > 0 THEN 1 + LOG(term_frequency)
                ELSE 0
            END
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(updateTFQuery);
            System.out.println("TF-Werte berechnet und aktualisiert.");
        } catch (SQLException e) {
            System.err.println("Fehler bei der Berechnung des TF-Werts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void createDocumentsTable() {
        Statement statement;
        try {
            String query = "CREATE TABLE IF NOT EXISTS documents(\n" +
                    "\tdocid SERIAL PRIMARY KEY,\n" +
                    "\turl TEXT NOT NULL,\n" +
                    "\tcrawled_on_date CHAR(20) NOT NULL\n" +
                    ");";
            statement = connection.createStatement();
            statement.executeUpdate(query);
            System.out.println("Documents Table is created");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createFeaturesTable() {
        Statement statement;
        try {
            String query = "CREATE TABLE IF NOT EXISTS features (\n" +
                    "\tdocid INT REFERENCES documents(docid),\n" +
                    "\tterm TEXT NOT NULL,\n" +
                    "\tterm_frequency INT NOT NULL\n" +
                    ");";
            statement = connection.createStatement();
            statement.executeUpdate(query);
            System.out.println("Features Table is created");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createLinksTable() {
        Statement statement;
        try {
            String query = "CREATE TABLE IF NOT EXISTS links (\n" +
                    "\tfrom_docid INT REFERENCES documents(docid),\n" +
                    "  \tto_docid INT REFERENCES documents(docid)\n" +
                    ")";
            statement = connection.createStatement();
            statement.executeUpdate(query);
            System.out.println("Links Table is created");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createTables() {
        this.createDocumentsTable();
        this.createFeaturesTable();
        this.createLinksTable();
    }

    public int insertDocument(String url, String crawledDate) {
        String insertQuery = "INSERT INTO documents (url, crawled_on_date) VALUES (?, ?) RETURNING docid;";
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setString(1, url);
            preparedStatement.setString(2, crawledDate);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int docid = resultSet.getInt("docid");
                // System.out.println("Document " + docid + " is inserted");
                return docid;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    public void insertLink(int fromDocid, int toDocid) {
        String insertQuery = "INSERT INTO links (from_docid, to_docid) VALUES (?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setInt(1, fromDocid);
            preparedStatement.setInt(2, toDocid);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void insertFeature(int docId, String term, int termFrequuency) {
        String insertQuery = "INSERT INTO features (docid, term, term_frequency) VALUES (?, ?, ? )";
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setInt(1, docId);
            preparedStatement.setString(2, term);
            preparedStatement.setInt(3, termFrequuency);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    // Queries For Exercise 3
    public List<SearchResult> conjuntiveCrawling (String[] searchedTerms, int resultSize )  {
        int searchedTermsCount = searchedTerms.length;
        List<SearchResult> foundItems = new ArrayList<>();


        List<String> stemmedSearchedTerms = Arrays.stream(searchedTerms)
                .map(term -> stemWord(term))  // Apply stemming to each term
                .collect(Collectors.toList());

        // TODO change SUM(term_frequency) with SUM(frequency_score) * 2 !!!!
        String insertedSearchedTerms = String.join(",", Collections.nCopies(searchedTermsCount, "?"));
        String conjunctiveQuery = "CREATE INDEX IF NOT EXISTS idx_term_docid ON features (term, docid); \n" +
                "SELECT d.docid, d.url, f.score FROM documents as d\n" +
                "jOIN (" +
                    "SELECT docid , SUM(term_frequency) as score FROM features WHERE term IN (" + insertedSearchedTerms + ")\n" +
                    "GROUP BY docid HAVING COUNT(DISTINCT term) = ? \n" +
                    "ORDER BY SUM(term_frequency) DESC\n" +
                    "LIMIT ? ) as f\n" +
                "ON d.docid = f.docid" ;


        try (PreparedStatement preparedStatement = connection.prepareStatement(conjunctiveQuery)) {
            for (int i = 0; i < searchedTermsCount; i++) {
                preparedStatement.setString(i + 1, stemmedSearchedTerms.get(i));
            }

            preparedStatement.setInt(searchedTermsCount + 1, searchedTermsCount);
            preparedStatement.setInt(searchedTermsCount + 2, resultSize);


            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int foundDocid = resultSet.getInt("docid");
                String foundDocURL = resultSet.getString("url");
                int foundDocScore = resultSet.getInt("score");

                foundItems.add(new SearchResult(foundDocid,foundDocURL, foundDocScore ));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return foundItems;
    }

    public List<SearchResult> disjunctiveCrawling(String[] searchedTerms, int resultSize) {
        List<SearchResult> foundItems = new ArrayList<>();

        int searchedTermsCount = searchedTerms.length;

        // TODO change SUM(term_frequency) with SUM(frequency_score)
        String insertedSearchedTerms = String.join(",", Collections.nCopies(searchedTermsCount, "?"));
        String disjunctiveQuery = "SELECT d.docid, d.url, f.score FROM documents as d\n" +
                "jOIN (" +
                    "SELECT docid , SUM(term_frequency) as score FROM features WHERE term IN (" + insertedSearchedTerms + ")\n" +
                    "GROUP BY docid \n" +
                    "ORDER BY SUM(term_frequency) DESC\n" +
                    "LIMIT ? ) as f\n" +
                "ON d.docid = f.docid" ;

        try (PreparedStatement preparedStatement = connection.prepareStatement(disjunctiveQuery)) {
            for (int i = 0; i < searchedTermsCount; i++) {
                preparedStatement.setString(i + 1, searchedTerms[i]);

            }
            preparedStatement.setInt(searchedTermsCount + 1, resultSize);


            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int foundDocid = resultSet.getInt("docid");
                String foundDocURL = resultSet.getString("url");
                int foundDocScore = resultSet.getInt("score");

                foundItems.add(new SearchResult(foundDocid,foundDocURL, foundDocScore ));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return foundItems;
    }

}




