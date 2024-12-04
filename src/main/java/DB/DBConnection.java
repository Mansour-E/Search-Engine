package DB;
import CommandInterface.SearchResult;
import Crawler.Crawler.URLDepthPair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.la4j.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.vector.dense.BasicVector;
import Sheet2.PageRank.PageRank;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public Set<String> getAllURLS() {
        Set<String> queuedUrls = new HashSet<>();
        String query = "SELECT docid FROM documents";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String url = rs.getString("docid");
                queuedUrls.add(url);
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
            stmt.executeUpdate("ALTER TABLE features ADD COLUMN IF NOT EXISTS bm25 REAL DEFAULT 0");
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
            ResultSet rs = totalDocsStmt.executeQuery("SELECT COUNT(*) AS total_documents FROM documents");
            int totalDocuments = rs.next() ? rs.getInt("total_documents") : 1;

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
            e.printStackTrace();
        }
    }
    public void reCompute() {
        try {
            calculateTF();
            calculateIDF();
            calculateTFIDF();
            calculateBM25InDatabase();
        } catch (SQLException e) {
            System.err.println("Failed to Recomputing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Queries For Exercise 3
    public List<SearchResult> searchCrawling(String[] conjunctiveSearchedTerms, String[] disjunctiveSearchedTerms, int resultSize, List<String> languages, String scoreOption) {
        List<SearchResult> foundConjunctiveSearchedTerms = this.conjuntiveCrawling(conjunctiveSearchedTerms, resultSize, languages, scoreOption);
        List<SearchResult> foundDisjunctiveSearchedTerms = this.disjunctiveCrawling(disjunctiveSearchedTerms, resultSize, languages, scoreOption);
        List<SearchResult> totalSearchResult = Stream.concat(foundConjunctiveSearchedTerms.stream(), foundDisjunctiveSearchedTerms.stream())
                .collect(Collectors.toList());
        List<SearchResult> sortedSearchResult = totalSearchResult.stream()
                .sorted((result1, result2) -> Double.compare(result2.getScore(), result1.getScore()))
                .collect(Collectors.toList());
        Set<String> seenUrls = new HashSet<>();
        List<SearchResult> finalSearchResults = new ArrayList<>();
        for (SearchResult result : sortedSearchResult) {
            if (seenUrls.size() >= resultSize) {
                break;
            }
            if (!seenUrls.contains(result.getUrl())) {
                seenUrls.add(result.getUrl());
                finalSearchResults.add(result);
            }
        }
        return finalSearchResults;
    }
    public List<SearchResult> conjuntiveCrawling (String[] searchedTerms, int resultSize, List<String> languages, String scoreOption) {
        List<SearchResult> foundItems = new ArrayList<>();
        if(searchedTerms.length != 0) {
            int searchedTermsCount = searchedTerms.length;
            List<String> stemmedSearchedTerms = Arrays.stream(searchedTerms)
                    .map(term -> {
                        String stemmedWord = stemWord(term);
                        // Check for corrections
                        String correctedTerm = suggestionCorrectionIfNecessary(stemmedWord, term);
                        // If correctedTerm is not empty, it means the word was corrected
                        return !correctedTerm.isEmpty() ? correctedTerm : stemmedWord;
                    })
                    .collect(Collectors.toList());


            String insertedSearchedTerms = String.join(",", Collections.nCopies(searchedTermsCount, "?"));
            String insertedLanguages = String.join(",", Collections.nCopies(languages.size(), "?"));
            String scoreExpression = "SUM(tfidf)";
            if ("BM25".equalsIgnoreCase(scoreOption)) {
                scoreExpression = "SUM(bm25)";
            }

            String conjunctiveQuery =
                    "SELECT d.docid, d.url, f.score AS score " +
                            "FROM documents d " +
                            "JOIN (" +
                            "   SELECT docid, " + scoreExpression+ " AS score " +
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
                    Double foundDocScore = resultSet.getDouble("score");

                    foundItems.add(new SearchResult(foundDocid,foundDocURL, foundDocScore ));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        return foundItems;
    }
    public List<SearchResult> disjunctiveCrawling(String[] searchedTerms, int resultSize, List<String> languages, String scoreOption) {
        List<SearchResult> foundItems = new ArrayList<>();
        if(searchedTerms.length != 0) {
            int searchedTermsCount = searchedTerms.length;
            List<String> stemmedSearchedTerms = Arrays.stream(searchedTerms)
                    .map(term -> {
                        String stemmedWord = stemWord(term);
                        // Check for corrections
                        String correctedTerm = suggestionCorrectionIfNecessary(stemmedWord, term);
                        // If correctedTerm is not empty, it means the word was corrected
                        return !correctedTerm.isEmpty() ? correctedTerm : stemmedWord;
                    })
                    .collect(Collectors.toList());

            String insertedSearchedTerms = String.join(",", Collections.nCopies(searchedTermsCount, "?"));
            String insertedLanguages = String.join(",", Collections.nCopies(languages.size(), "?"));
            String scoreExpression = "SUM(tfidf)";
            if ("BM25".equalsIgnoreCase(scoreOption)) {
                scoreExpression = "SUM(bm25)";
            }

            String disjunctiveQuery =
                    "SELECT d.docid, d.url, f.score AS score " +
                            "FROM documents d " +
                            "JOIN (" +
                            "   SELECT docid, " + scoreExpression + " AS score " +
                            "   FROM features " +
                            "   WHERE term IN (" + insertedSearchedTerms + ") " +
                            "   GROUP BY docid " +
                            ") f " +
                            "ON d.docid = f.docid " +
                            "WHERE d.lang IN (" + insertedLanguages + ") " +
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

        }
        return foundItems;
}

    // Queries For Exercise 4
    public JSONArray computeStat(String[] conjunctiveSearchedTerms, String[] disjunctiveSearchedTerms ) {
        JSONArray statArray = new JSONArray();
        String query = "SELECT  COUNT(docid) AS df FROM features WHERE term = ? ";

        List<String> totalSearchResult = Stream.concat(
                Arrays.stream(conjunctiveSearchedTerms),
                Arrays.stream(disjunctiveSearchedTerms)
        ).collect(Collectors.toList());


        List<String> stemmedSearchedTerms = totalSearchResult.stream()
                .map(term -> stemWord(term))
                .collect(Collectors.toList());

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);

            for (int i = 0; i < stemmedSearchedTerms.size(); i++) {
                preparedStatement.setString(1, stemmedSearchedTerms.get(i));
                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    JSONObject termObject = new JSONObject();
                    termObject.put("term", totalSearchResult.get(i));
                    termObject.put("df", resultSet.getInt("df"));
                    statArray.put(termObject);
                } else {
                    // If the term does not exist in any document, set df to 0
                    JSONObject termObject = new JSONObject();
                    termObject.put("term", totalSearchResult.get(i));
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
        String getDocsQuery = "SELECT docid FROM documents ORDER BY docid";
        String linksQuery = "SELECT from_docid, to_docid FROM links";
        Map<Integer, List<Integer>> linkMap = new HashMap<>();
        int n = 0;

        // Map docids to indexes
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(getDocsQuery)) {
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
                    linkMatrix.set(i, j, teleportationProb);
                }
            } else {
                // Page with outgoing links: apply both link probability and teleportation probability
                double linkProb = (1 - tp) / outDegree;
                for (int j = 0; j < n; j++) {
                    if (outgoingLinks.contains(j)) {
                        // Link probability + teleportation probability
                        linkMatrix.set(i, j, linkProb + teleportationProb);
                    } else {
                        // Teleportation probability only
                        linkMatrix.set(i, j, teleportationProb);
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
    public void calculateBM25InDatabase() throws SQLException {
        //Step 1: Calculate the PageRank value using calculatePageRanking()
        System.out.println("Calculate PageRank values...");
        PageRank pr = new PageRank();
        pr.calculatePageRanking(this);

        // Step 2: Calculate and update BM25 values
        System.out.println("Calculate BM25 values and combine with PageRank...");

        String bm25UpdateQuery = """
        WITH bm25_scores AS (
           SELECT\s
               f.docid,
               f.term,
               (f.tf * LOG((SELECT COUNT(*) FROM documents)::double precision / df)) AS bm25
           FROM features f
           JOIN (
               SELECT\s
                   term,
                   COUNT(DISTINCT docid) AS df
               FROM features
               GROUP BY term
           ) term_df ON f.term = term_df.term
       ),
       combined_scores AS (
           SELECT\s
               bm25.docid,
               bm25.term,
               bm25.bm25 + COALESCE(d.pagerank, 0) AS combined_score  
           FROM bm25_scores bm25
           LEFT JOIN documents d ON bm25.docid = d.docid  
       )
       UPDATE features
       SET bm25 = combined_scores.combined_score
       FROM combined_scores
       WHERE features.docid = combined_scores.docid
         AND features.term = combined_scores.term;
    """;

        try (PreparedStatement ps = connection.prepareStatement(bm25UpdateQuery)) {
            ps.executeUpdate();
        }

        System.out.println("BM25 values successfully combined with PageRank and updated.");
    }
    public void createViews() {
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

// Exercise 4
    // This function update the lang column with the corresponding language of the document
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
    // This function suggests a correction for a misspelled word
    public String suggestionCorrectionIfNecessary(String searchedWord, String term) {
        // Query to check if the word exists in the database
        String checkExistenceQuery = "SELECT term FROM features WHERE term = ?";
        // If the searchedWord exists in the features table
        try (PreparedStatement stmt = connection.prepareStatement(checkExistenceQuery)) {
            stmt.setString(1, searchedWord);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Word exists, no correction needed
                    return "";
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking word existence: " + searchedWord, e);
        }

        // If the word doesn't exist, find the most similar word using Levenshtein distance
        try (Statement createExtensionStmt = connection.createStatement()) {
            createExtensionStmt.execute("CREATE EXTENSION IF NOT EXISTS fuzzystrmatch;");
        } catch (SQLException e) {
            throw new RuntimeException("Error ensuring fuzzystrmatch extension exists", e);
        }
        String correctionQuery =
                "SELECT term, COUNT(*) AS frequency " +
                        "FROM features " +
                        "WHERE levenshtein(term, ?) <= 1 " +
                        "GROUP BY term " +
                        "ORDER BY frequency DESC " +
                        "LIMIT 1";

        try (PreparedStatement stmt = connection.prepareStatement(correctionQuery)) {
            stmt.setString(1, searchedWord);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Return the most similar word
                    String correctedWord = rs.getString("term");
                    System.out.println("The word '" + term + "' is misspelled but has been corrected and replaced with the stemmed word: '" + correctedWord + "'.");
                    return correctedWord;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding correction for word: " + searchedWord, e);
        }

        // No correction found
        System.out.println("The word '" + term + "' is misspelled and could not be corrected.");
        return "";
    }
}




