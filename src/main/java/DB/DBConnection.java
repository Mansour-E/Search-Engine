package DB;
import CommandInterface.SearchResult;
import Crawler.Crawler.URLDepthPair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.la4j.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.vector.dense.BasicVector;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static Indexer.Parser.stemWord;


public class DBConnection {

    private Connection connection ;
    private Map<Integer, Integer> docIdToIndex = new HashMap<>();
    private Map<Integer, Integer> indexToDocId = new HashMap<>();

    public DBConnection(String dbName, String dbOwner, String dbPassword, Boolean init ) {
        this.connection = this.connectToDb(dbName, dbOwner, dbPassword);
        if(init) {
            createTables();
            initializeSchema();
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

    //                     "\tlang TEXT NOT NULL CHECK (lang IN ('en', 'ge')),\n" +
    public void createDocumentsTable() {
        Statement statement;
        try {
            String query = "CREATE TABLE IF NOT EXISTS documents(\n" +
                    "\tdocid SERIAL PRIMARY KEY,\n" +
                    "\turl TEXT NOT NULL,\n" +
                    "\tcrawled_on_date CHAR(20) NOT NULL,\n" +
                    "\tlang TEXT NOT NULL\n" +
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

    public void crawledPagesQueueTable() {
        Statement statement;
        try {
            String query = "CREATE TABLE IF NOT EXISTS crawledPagesQueueTable (\n" +
                    "\tid SERIAL PRIMARY KEY,\n" +
                    "\turl TEXT NOT NULL,\n" +
                    "\tdepth INT NOT NULL,\n" +
                    "\tstate INT NOT NULL \n" +
                    ");";
            statement = connection.createStatement();
            statement.executeUpdate(query);
            System.out.println("crawledPagesQueueTable Table is created");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createTables() {
        this.createDocumentsTable();
        this.createFeaturesTable();
        this.createLinksTable();
        this.crawledPagesQueueTable();
    }

    public int insertDocument(String url, String crawledDate, String lang) {
        String insertQuery = "INSERT INTO documents (url, crawled_on_date, lang) VALUES (?, ?, ?) RETURNING docid;";
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setString(1, url);
            preparedStatement.setString(2, crawledDate);
            preparedStatement.setString(3, lang);


            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int docid = resultSet.getInt("docid");
                // System.out.println("Document " + docid + " is inserted");
                reCompute(); // Aktualisiere TF, IDF und TF*IDF nach dem Hinzufügen eines neuen Dokuments
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
    public ResultSet executeQuery(String query) {
        try {
            Statement stmt = connection.createStatement();
            return stmt.executeQuery(query);
        } catch (SQLException e) {
            throw new RuntimeException("Error executing query: " + query, e);
        }
    }
    public DBConnection() {
        this.connection = connection;
    }
    public Connection getConnection() {
        return connection;
    }

    public List<URLDepthPair> getQueuedUrls() {
        List<URLDepthPair> queuedUrls = new ArrayList<>();
        String query = "SELECT id, url, depth FROM crawledPagesQueueTable WHERE state = 0 ORDER BY depth ASC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String url = rs.getString("url");
                int depth = rs.getInt("depth");
                // Unknown because they have not yet crawled
                queuedUrls.add(new URLDepthPair(id, url, depth, "Unknown"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return queuedUrls;
    }

    public  Set<String> getVisitedUrls() {
        Set<String> visitedPages = new HashSet<>();
        String query = "Select url From crawledPagesQueueTable WHERE state = 1";
        try (Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String url = rs.getString("url");
                visitedPages.add(url);

            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return visitedPages;
    }

    public void updateCrawledPageState(String url, int state) {
        String query = "UPDATE crawledPagesQueueTable SET state = ? WHERE url = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, state);
            ps.setString(2, url);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertIntoCrawledPagesQueue(String url, int depth, int state) {
        String query = "INSERT INTO crawledPagesQueueTable (url, depth, state) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, url);
            ps.setInt(2, depth);
            ps.setInt(3, state);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
        } catch (SQLException e) {
            System.err.println("Fehler bei der Berechnung des TF-Werts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void calculateIDF() {
        String updateIDFQuery = """
        UPDATE features
        SET idf = LOG(? / (SELECT COUNT(DISTINCT docid) FROM features WHERE term = features.term))
    """;

        try (PreparedStatement stmt = connection.prepareStatement(updateIDFQuery);
             Statement totalDocsStmt = connection.createStatement()) {

            // Berechne die Gesamtzahl der Dokumente
            ResultSet rs = totalDocsStmt.executeQuery("SELECT COUNT(*) AS total_documents FROM documents");
            int totalDocuments = rs.next() ? rs.getInt("total_documents") : 1; // Vermeide Division durch Null

            stmt.setInt(1, totalDocuments);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Fehler bei der Berechnung des IDF-Werts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void calculateTFIDF() {
        String updateTFIDFQuery = """
        UPDATE features
        SET tfidf = tf * idf
    """;

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(updateTFIDFQuery);
        } catch (SQLException e) {
            System.err.println("Fehler bei der Berechnung des TF*IDF-Werts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void reCompute() {
        // Berechne TF, IDF und TF*IDF neu
        calculateTF();       // Berechne Term Frequency
        calculateIDF();      // Berechne Inverse Document Frequency
        calculateTFIDF();    // Berechne TF*IDF
    }


    // Queries For Exercise 3
    public List<SearchResult> conjuntiveCrawling (String[] searchedTerms, int resultSize, List<String> languages) {
        int searchedTermsCount = searchedTerms.length;
        List<SearchResult> foundItems = new ArrayList<>();
        List<String> stemmedSearchedTerms = Arrays.stream(searchedTerms)
                .map(term -> stemWord(term))
                .collect(Collectors.toList());

        String insertedSearchedTerms = String.join(",", Collections.nCopies(searchedTermsCount, "?"));
        String insertedLanguages = String.join(",", Collections.nCopies(languages.size(), "?"));

        String conjunctiveQuery =
                "SELECT d.docid, d.url, f.score AS score " +
                        "FROM documents d " +
                        "JOIN (" +
                        "   SELECT docid, SUM(tfidf) AS score " +
                        "   FROM features " +
                        "   WHERE term IN (" + insertedSearchedTerms + ") " +
                        "   GROUP BY docid " +
                        "   HAVING COUNT(DISTINCT term) = ? " +
                        ") f " +
                        "ON d.docid = f.docid " +
                        "WHERE d.lang IN (" + insertedLanguages + ") " +
                        "ORDER BY f.score DESC " +
                        "LIMIT ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(conjunctiveQuery)) {
            int parameterIndex = 1;
            for (String term : stemmedSearchedTerms) {
                preparedStatement.setString(parameterIndex++, term);
            }

            preparedStatement.setInt(parameterIndex++, searchedTermsCount);
            for (String lang : languages) {
                preparedStatement.setString(parameterIndex++, lang);
            }
            preparedStatement.setInt(parameterIndex, resultSize);



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


    public List<SearchResult> disjunctiveCrawling(String[] searchedTerms, int resultSize, List<String> languages) {
        List<SearchResult> foundItems = new ArrayList<>();
        int searchedTermsCount = searchedTerms.length;
        List<String> stemmedSearchedTerms = Arrays.stream(searchedTerms)
                .map(term -> stemWord(term))
                .collect(Collectors.toList());

        String insertedSearchedTerms = String.join(",", Collections.nCopies(searchedTermsCount, "?"));
        String insertedLanguages = String.join(",", Collections.nCopies(languages.size(), "?"));

        String disjunctiveQuery =
                "SELECT d.docid, d.url, f.score AS score " +
                        "FROM documents d " +
                        "JOIN (" +
                        "   SELECT docid, SUM(tfidf) AS score " +
                        "   FROM features " +
                        "   WHERE term IN (" + insertedSearchedTerms + ") " +
                        "   GROUP BY docid " +
                        ") f " +
                        "ON d.docid = f.docid " +
                        "WHERE d.lang IN (" + insertedLanguages + ") " + // Use IN clause here
                        "ORDER BY f.score DESC " +
                        "LIMIT ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(disjunctiveQuery)) {
            int parameterIndex = 1;

            for (String term : stemmedSearchedTerms) {
                preparedStatement.setString(parameterIndex++, term);
            }
            for (String lang : languages) {
                preparedStatement.setString(parameterIndex++, lang);
            }

            preparedStatement.setInt(parameterIndex, resultSize);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int foundDocid = resultSet.getInt("docid");
                String foundDocURL = resultSet.getString("url");
                int foundDocScore = resultSet.getInt("score");

                foundItems.add(new SearchResult(foundDocid, foundDocURL, foundDocScore));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return foundItems;
    }



    // Queries For Exercise 4
    public JSONArray computeStat(String[] searchedTerms ) {
        JSONArray statArray = new JSONArray();
        String query = "SELECT  COUNT(docid) AS df FROM features WHERE term = ? ";

        List<String> stemmedSearchedTerms = Arrays.stream(searchedTerms)
                .map(term -> stemWord(term))
                .collect(Collectors.toList());

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);

            for (int i = 0; i < stemmedSearchedTerms.size(); i++) {
                preparedStatement.setString(1, stemmedSearchedTerms.get(i));
                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    JSONObject termObject = new JSONObject();
                    termObject.put("term", searchedTerms[i]);
                    termObject.put("df", resultSet.getInt("df"));
                    statArray.put(termObject);
                } else {
                    // If the term does not exist in any document, set df to 0
                    JSONObject termObject = new JSONObject();
                    termObject.put("term", searchedTerms[i]);
                    termObject.put("df", 0);
                    statArray.put(termObject);
                }
                resultSet.close();
            }
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return statArray;

    }

    public int calcualteCW() {
        String query = "SELECT count(DISTINCT term) from features\n";

        try( Statement stmt = connection.createStatement() ;
            ResultSet rs = stmt.executeQuery(query)) {
                rs.next();
                return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


/*-----------------------------------------------------------------------------------------------------
-----------------------sheet 2 -------------------------------------------------------------------------
-------------------------------------------------------------------------------------------------------
 */

// Exercise 1

    public void extendDocumentsWithPagerankColumn() {
        Statement statement;
        try {
            String query = "ALTER TABLE Documents " +
                    "ADD COLUMN IF NOT EXISTS pagerank DOUBLE PRECISION;";

            statement = connection.createStatement();
            statement.executeUpdate(query);
            System.out.println("pagerank column is added");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Matrix createLinkMatrix(double tp) {
        String uniqueDocsQuery = "SELECT DISTINCT docid FROM documents ORDER BY docid";
        /*
        String uniqueDocsQuery = "SELECT DISTINCT docid FROM (" +
                "SELECT from_docid AS docid FROM links2 " +
                "UNION " +
                "SELECT to_docid AS docid FROM links2" +
                ") AS unique_docs";

         */

        String linksQuery = "SELECT from_docid, to_docid FROM links";
        Map<Integer, List<Integer>> linkMap = new HashMap<>();
        int n = 0;

        // Map docids to indexes
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(uniqueDocsQuery)) {
            while (rs.next()) {
                int docId = rs.getInt("docid");
                docIdToIndex.put(docId, n);
                indexToDocId.put(n,docId);
                n++;

            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        // Create a map of outgoing links for each page
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(linksQuery)) {
            while (rs.next()) {
                int fromDocId = rs.getInt("from_docid");
                int toDocId = rs.getInt("to_docid");
                int fromIndex = docIdToIndex.get(fromDocId);
                int toIndex = docIdToIndex.get(toDocId);

                linkMap.computeIfAbsent(fromIndex, k -> new ArrayList<>()).add(toIndex);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Create the initial matrix
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

    public void insertPageRanking(BasicVector rank) {

        String query = "UPDATE documents SET pagerank = ? WHERE docid = ?";
        this.extendDocumentsWithPagerankColumn();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            for (int i = 0; i < rank.length(); i++) {
                int docId = indexToDocId.get(i);
                double pageRank = rank.get(i);
                preparedStatement.setDouble(1, pageRank);
                preparedStatement.setInt(2, docId);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

// Exercise 2

    public void calculateBM25InDatabase() {
        String calculateBM25SQL = """
        INSERT INTO features_bm25 (document_id, term, bm25_score)
        SELECT 
            tf.document_id,
            tf.term,
            idf.idf * (tf.term_frequency * (1.5 + 1)) / 
            (tf.term_frequency + 1.5 * (1 - 0.75 + 0.75 * (doc_lengths.document_length / avgdl.avgdl)))
        FROM 
            (SELECT document_id, term, COUNT(*) AS term_frequency 
             FROM inverted_index GROUP BY document_id, term) AS tf
        JOIN 
            (SELECT term, LOG((CAST(total_docs AS FLOAT) - doc_count + 0.5) / (doc_count + 0.5)) AS idf
             FROM (
                 SELECT term, COUNT(DISTINCT document_id) AS doc_count, 
                        (SELECT COUNT(DISTINCT document_id) FROM inverted_index) AS total_docs
                 FROM inverted_index
                 GROUP BY term
             ) AS term_stats) AS idf
        ON tf.term = idf.term
        JOIN 
            (SELECT document_id, COUNT(*) AS document_length 
             FROM inverted_index GROUP BY document_id) AS doc_lengths
        ON tf.document_id = doc_lengths.document_id
        CROSS JOIN 
            (SELECT AVG(document_length) AS avgdl 
             FROM (SELECT document_id, COUNT(*) AS document_length 
                   FROM inverted_index GROUP BY document_id) AS doc_lengths) AS avgdl;
    """;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(calculateBM25SQL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<SearchResult> searchWithView(String[] searchTerms, int resultSize, String viewName) throws SQLException {
        List<SearchResult> results = new ArrayList<>();
        String placeholders = String.join(",", Collections.nCopies(searchTerms.length, "?"));

        String sql = String.format("""
        SELECT id, url, rank, score
        FROM %s
        WHERE term IN (%s)
        ORDER BY score DESC
        LIMIT ?
    """, viewName, placeholders);

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int index = 1;
            for (String term : searchTerms) {
                pstmt.setString(index++, term);
            }
            pstmt.setInt(index, resultSize);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String url = rs.getString("url");
                    int rank = rs.getInt("rank");
                    results.add(new SearchResult(id, url, rank));
                }
            }
        }
        return results;
    }

    public void createBM25Tables() { //zum aufgabe 2 sheet2
        try (Statement stmt = connection.createStatement()) {
            // BM25-Statistiken für Dokumente (Document Length, Average Length, usw.)
            String createBM25StatsTable = """
            CREATE TABLE IF NOT EXISTS bm25_stats (
                docid INT REFERENCES documents(docid),
                document_length INT DEFAULT 0,
                average_length REAL DEFAULT 0
            );
        """;
            stmt.executeUpdate(createBM25StatsTable);
            System.out.println("BM25-Stats Table created");

            // Sicherstellen, dass die Features-Tabelle über eine zusätzliche BM25-Spalte verfügt
            String addBM25Column = """
            ALTER TABLE features
            ADD COLUMN IF NOT EXISTS bm25_score REAL DEFAULT 0;
        """;
            stmt.executeUpdate(addBM25Column);
            System.out.println("BM25-Score column added to Features Table");
        } catch (SQLException e) {
            System.err.println("Fehler beim Erstellen der BM25-Tabellen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void createViews() {//zum aufgabe 2 sheet2
        try (Statement stmt = connection.createStatement()) {
            // View zur schnellen Berechnung der Document Length (Summe der Term-Frequenzen)
            String createDocumentLengthView = """
            CREATE OR REPLACE VIEW document_length_view AS
            SELECT
                docid,
                SUM(term_frequency) AS document_length
            FROM features
            GROUP BY docid;
        """;
            stmt.executeUpdate(createDocumentLengthView);
            System.out.println("Document Length View created");

            // View zur Berechnung der durchschnittlichen Dokumentenlänge
            String createAverageLengthView = """
            CREATE OR REPLACE VIEW average_length_view AS
            SELECT
                AVG(document_length) AS average_length
            FROM document_length_view;
        """;
            stmt.executeUpdate(createAverageLengthView);
            System.out.println("Average Length View created");

            // View zur Berechnung der BM25-Score für jedes Dokument
            String createBM25View = """
            CREATE OR REPLACE VIEW bm25_view AS
            SELECT
                f.docid,
                f.term,
                (f.tfidf / (f.tfidf + 1)) * 
                (1.2 + 1) /
                (1.2 * (1 - 0.75 + 0.75 * dl.document_length / al.average_length) + f.tfidf) AS bm25_score
            FROM features f
            JOIN document_length_view dl ON f.docid = dl.docid
            JOIN average_length_view al ON 1=1;
        """;
            stmt.executeUpdate(createBM25View);
            System.out.println("BM25 View created");
        } catch (SQLException e) {
            System.err.println("Fehler beim Erstellen der Views: " + e.getMessage());
            e.printStackTrace();
        }
    }

// Exercise 3
    public void updateLanguageDocuments(String url, String lang) {
        String query = "UPDATE documents SET lang = ? WHERE url = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, lang);
            ps.setString(2, url);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}




