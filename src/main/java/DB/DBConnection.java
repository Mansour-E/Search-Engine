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
    private Connection connection;
    private Map<Integer, Integer> docIdToIndex = new HashMap<>();
    private Map<Integer, Integer> indexToDocId = new HashMap<>();
    public DBConnection(String dbName, String dbOwner, String dbPassword, boolean init) {
        String host = getEnvOrDefault("DB_HOST", "localhost");
        String port = getEnvOrDefault("DB_PORT", "5432");
        String name = getEnvOrDefault("DB_NAME", dbName);
        String user = getEnvOrDefault("DB_USER", dbOwner);
        String password = getEnvOrDefault("DB_PASSWORD", dbPassword);
        this.connection = connectToDb(host, port, name, user, password);
        if (init) {
            createTables();
            initializeSchema();
        }
    }
    private static String getEnvOrDefault(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }
    private Connection connectToDb(String host, String port, String dbName, String user, String password) {
        Connection conn = null;
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("DB connection established: " + url);
        } catch (Exception e) {
            System.err.println("DB connection failed: " + e.getMessage());
        }
        return conn;
    }
    public void createDocumentsTable() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS documents (" +
                "  docid SERIAL PRIMARY KEY," +
                "  url TEXT NOT NULL," +
                "  crawled_on_date CHAR(20) NOT NULL," +
                "  lang TEXT NOT NULL" +
                ")"
            );
            System.out.println("Table 'documents' ready.");
        } catch (SQLException e) {
            throw new RuntimeException("createDocumentsTable: " + e.getMessage(), e);
        }
    }
    public void createFeaturesTable() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS features (" +
                "  docid INT REFERENCES documents(docid)," +
                "  term TEXT NOT NULL," +
                "  term_frequency INT NOT NULL" +
                ")"
            );
            System.out.println("Table 'features' ready.");
        } catch (SQLException e) {
            throw new RuntimeException("createFeaturesTable: " + e.getMessage(), e);
        }
    }
    public void createLinksTable() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS links (" +
                "  from_docid INT REFERENCES documents(docid)," +
                "  to_docid INT REFERENCES documents(docid)" +
                ")"
            );
            System.out.println("Table 'links' ready.");
        } catch (SQLException e) {
            throw new RuntimeException("createLinksTable: " + e.getMessage(), e);
        }
    }
    public void crawledPagesQueueTable() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS crawledPagesQueueTable (" +
                "  id SERIAL PRIMARY KEY," +
                "  url TEXT NOT NULL," +
                "  depth INT NOT NULL," +
                "  state INT NOT NULL" +
                ")"
            );
            System.out.println("Table 'crawledPagesQueueTable' ready.");
        } catch (SQLException e) {
            throw new RuntimeException("crawledPagesQueueTable: " + e.getMessage(), e);
        }
    }
    public void createTables() {
        createDocumentsTable();
        createFeaturesTable();
        createLinksTable();
        crawledPagesQueueTable();
    }
    private void initializeSchema() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("ALTER TABLE features ADD COLUMN IF NOT EXISTS bm25 REAL DEFAULT 0");
            stmt.executeUpdate("ALTER TABLE features ADD COLUMN IF NOT EXISTS tf REAL DEFAULT 0");
            stmt.executeUpdate("ALTER TABLE features ADD COLUMN IF NOT EXISTS idf REAL DEFAULT 0");
            stmt.executeUpdate("ALTER TABLE features ADD COLUMN IF NOT EXISTS tfidf REAL DEFAULT 0");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_features_term ON features (term)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_features_docid ON features (docid)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_documents_url ON documents (url)");
        } catch (Exception e) {
            System.err.println("initializeSchema error: " + e.getMessage());
        }
    }
    public int insertDocument(String url, String crawledDate, String lang) {
        String sql = "INSERT INTO documents (url, crawled_on_date, lang) VALUES (?, ?, ?) RETURNING docid";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, url);
            ps.setString(2, crawledDate);
            ps.setString(3, lang);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("docid");
        } catch (SQLException e) {
            throw new RuntimeException("insertDocument: " + e.getMessage(), e);
        }
        return -1;
    }
    public void insertLink(int fromDocid, int toDocid) {
        String sql = "INSERT INTO links (from_docid, to_docid) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, fromDocid);
            ps.setInt(2, toDocid);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("insertLink: " + e.getMessage(), e);
        }
    }
    public void insertFeature(int docId, String term, int termFrequency) {
        String sql = "INSERT INTO features (docid, term, term_frequency) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, docId);
            ps.setString(2, term);
            ps.setInt(3, termFrequency);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("insertFeature: " + e.getMessage(), e);
        }
    }
    public List<URLDepthPair> getQueuedUrls() {
        List<URLDepthPair> list = new ArrayList<>();
        String sql = "SELECT id, url, depth FROM crawledPagesQueueTable WHERE state = 0 ORDER BY depth ASC";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new URLDepthPair(rs.getInt("id"), rs.getString("url"), rs.getInt("depth"), "Unknown"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    public Set<String> getAllURLS() {
        Set<String> urls = new HashSet<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT url FROM documents")) {
            while (rs.next()) urls.add(rs.getString("url"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return urls;
    }
    public Set<String> getVisitedUrls() {
        Set<String> visited = new HashSet<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT url FROM crawledPagesQueueTable WHERE state = 1")) {
            while (rs.next()) visited.add(rs.getString("url"));
        } catch (SQLException e) {
            throw new RuntimeException("getVisitedUrls: " + e.getMessage(), e);
        }
        return visited;
    }
    public void updateCrawledPageState(String url, int state) {
        String sql = "UPDATE crawledPagesQueueTable SET state = ? WHERE url = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, state);
            ps.setString(2, url);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void insertIntoCrawledPagesQueue(String url, int depth, int state) {
        String sql = "INSERT INTO crawledPagesQueueTable (url, depth, state) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, url);
            ps.setInt(2, depth);
            ps.setInt(3, state);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void calculateTF() {
        String sql = "UPDATE features SET tf = CASE WHEN term_frequency > 0 THEN 1 + LOG(term_frequency) ELSE 0 END";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("calculateTF error: " + e.getMessage());
        }
    }
    public void calculateIDF() {
        String sql =
            "UPDATE features " +
            "SET idf = LOG(? / (SELECT COUNT(DISTINCT docid) FROM features AS f2 WHERE f2.term = features.term))";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             Statement totalStmt = connection.createStatement()) {
            ResultSet rs = totalStmt.executeQuery("SELECT COUNT(*) FROM documents");
            int total = rs.next() ? rs.getInt(1) : 1;
            ps.setInt(1, total);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("calculateIDF error: " + e.getMessage());
        }
    }
    public void calculateTFIDF() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("UPDATE features SET tfidf = tf * idf");
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
            createViews();
            System.out.println("Scores recomputed: TF-IDF + BM25 + PageRank.");
        } catch (SQLException e) {
            System.err.println("reCompute failed: " + e.getMessage());
        }
    }
    public List<SearchResult> searchCrawling(String[] conjunctiveTerms, String[] disjunctiveTerms, int resultSize, List<String> languages, String scoreOption) {
        List<SearchResult> conjResults = conjuntiveCrawling(conjunctiveTerms, resultSize, languages, scoreOption);
        List<SearchResult> disjResults = disjunctiveCrawling(disjunctiveTerms, resultSize, languages, scoreOption);
        Set<String> seenUrls = new HashSet<>();
        return Stream.concat(conjResults.stream(), disjResults.stream())
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .filter(r -> seenUrls.add(r.getUrl()))
                .limit(resultSize)
                .collect(Collectors.toList());
    }
    public List<SearchResult> conjuntiveCrawling(String[] searchedTerms, int resultSize, List<String> languages, String scoreOption) {
        createViews();
        List<SearchResult> found = new ArrayList<>();
        if (searchedTerms.length == 0) return found;
        List<String> stemmed = stemAndCorrect(searchedTerms);
        String viewName = "BM25".equalsIgnoreCase(scoreOption) ? "features_bm25" : "features_tfidf";
        String placeholders = String.join(",", Collections.nCopies(stemmed.size(), "?"));
        String langPlaceholders = String.join(",", Collections.nCopies(languages.size(), "?"));
        String sql =
            "SELECT d.docid, d.url, f.score " +
            "FROM documents d " +
            "JOIN (SELECT docid, SUM(score) AS score FROM " + viewName +
            "      WHERE term IN (" + placeholders + ") GROUP BY docid HAVING COUNT(DISTINCT term) = ?) f " +
            "ON d.docid = f.docid " +
            "WHERE d.lang IN (" + langPlaceholders + ") " +
            "ORDER BY f.score DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int idx = 1;
            for (String t : stemmed) ps.setString(idx++, t);
            ps.setInt(idx++, stemmed.size());
            for (String l : languages) ps.setString(idx++, l);
            ps.setInt(idx, resultSize);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                found.add(new SearchResult(rs.getInt("docid"), rs.getString("url"), rs.getDouble("score")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("conjuntiveCrawling: " + e.getMessage(), e);
        }
        return found;
    }
    public List<SearchResult> disjunctiveCrawling(String[] searchedTerms, int resultSize, List<String> languages, String scoreOption) {
        List<SearchResult> found = new ArrayList<>();
        if (searchedTerms.length == 0) return found;
        List<String> stemmed = stemAndCorrect(searchedTerms);
        String viewName = "BM25".equalsIgnoreCase(scoreOption) ? "features_bm25" : "features_tfidf";
        String placeholders = String.join(",", Collections.nCopies(stemmed.size(), "?"));
        String langPlaceholders = String.join(",", Collections.nCopies(languages.size(), "?"));
        String sql =
            "SELECT d.docid, d.url, f.score " +
            "FROM documents d " +
            "JOIN (SELECT docid, SUM(score) AS score FROM " + viewName +
            "      WHERE term IN (" + placeholders + ") GROUP BY docid) f " +
            "ON d.docid = f.docid " +
            "WHERE d.lang IN (" + langPlaceholders + ") " +
            "ORDER BY f.score DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int idx = 1;
            for (String t : stemmed) ps.setString(idx++, t);
            for (String l : languages) ps.setString(idx++, l);
            ps.setInt(idx, resultSize);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                found.add(new SearchResult(rs.getInt("docid"), rs.getString("url"), rs.getDouble("score")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("disjunctiveCrawling: " + e.getMessage(), e);
        }
        return found;
    }
    private List<String> stemAndCorrect(String[] terms) {
        return Arrays.stream(terms)
                .map(t -> {
                    String stemmed = stemWord(t);
                    String corrected = suggestionCorrectionIfNecessary(stemmed, t);
                    return !corrected.isEmpty() ? corrected : stemmed;
                })
                .collect(Collectors.toList());
    }
    public JSONArray computeStat(String[] conjunctiveTerms, String[] disjunctiveTerms) {
        JSONArray statArray = new JSONArray();
        List<String> allTerms = Stream.concat(Arrays.stream(conjunctiveTerms), Arrays.stream(disjunctiveTerms)).collect(Collectors.toList());
        String sql = "SELECT COUNT(DISTINCT docid) AS df FROM features WHERE term = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (String term : allTerms) {
                String stemmed = stemWord(term);
                ps.setString(1, stemmed);
                ResultSet rs = ps.executeQuery();
                JSONObject obj = new JSONObject();
                obj.put("term", term);
                obj.put("df", rs.next() ? rs.getInt("df") : 0);
                statArray.put(obj);
            }
        } catch (SQLException e) {
            throw new RuntimeException("computeStat: " + e.getMessage(), e);
        }
        return statArray;
    }
    public int calcualteCW() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(DISTINCT term) FROM features")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("calcualteCW: " + e.getMessage(), e);
        }
    }
    public void extendDocumentsWithPagerankColumn() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("ALTER TABLE documents ADD COLUMN IF NOT EXISTS pagerank DOUBLE PRECISION DEFAULT 0");
        } catch (SQLException e) {
            throw new RuntimeException("extendDocumentsWithPagerankColumn: " + e.getMessage(), e);
        }
    }
    public Matrix createLinkMatrix(double teleportProb) {
        String docsQuery = "SELECT docid FROM documents ORDER BY docid";
        String linksQuery = "SELECT from_docid, to_docid FROM links";
        Map<Integer, List<Integer>> linkMap = new HashMap<>();
        int n = 0;
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(docsQuery)) {
            while (rs.next()) {
                int docId = rs.getInt("docid");
                docIdToIndex.put(docId, n);
                indexToDocId.put(n, docId);
                n++;
            }
        } catch (SQLException e) {
            throw new RuntimeException("createLinkMatrix (docs): " + e.getMessage(), e);
        }
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(linksQuery)) {
            while (rs.next()) {
                int from = rs.getInt("from_docid");
                int to = rs.getInt("to_docid");
                if (docIdToIndex.containsKey(from) && docIdToIndex.containsKey(to)) {
                    linkMap.computeIfAbsent(docIdToIndex.get(from), k -> new ArrayList<>()).add(docIdToIndex.get(to));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("createLinkMatrix (links): " + e.getMessage(), e);
        }
        Matrix matrix = new Basic2DMatrix(n, n);
        double tpProb = teleportProb / n;
        for (int i = 0; i < n; i++) {
            List<Integer> outLinks = linkMap.getOrDefault(i, Collections.emptyList());
            int outDegree = outLinks.size();
            if (outDegree == 0) {
                for (int j = 0; j < n; j++) matrix.set(i, j, tpProb);
            } else {
                double linkProb = (1 - teleportProb) / outDegree;
                for (int j = 0; j < n; j++) {
                    matrix.set(i, j, outLinks.contains(j) ? linkProb + tpProb : tpProb);
                }
            }
        }
        return matrix;
    }
    public void insertPageRanking(BasicVector rank) {
        extendDocumentsWithPagerankColumn();
        String sql = "UPDATE documents SET pagerank = ? WHERE docid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < rank.length(); i++) {
                ps.setDouble(1, rank.get(i));
                ps.setInt(2, indexToDocId.get(i));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("insertPageRanking: " + e.getMessage(), e);
        }
    }
    public void calculateBM25InDatabase() throws SQLException {
        new PageRank().calculatePageRanking(this);
        String sql =
            "WITH bm25_scores AS (" +
            "  SELECT f.docid, f.term," +
            "    (f.tf * LOG((SELECT COUNT(*) FROM documents)::double precision / NULLIF(df.cnt, 0))) AS bm25" +
            "  FROM features f" +
            "  JOIN (SELECT term, COUNT(DISTINCT docid) AS cnt FROM features GROUP BY term) df ON f.term = df.term" +
            ")," +
            "combined AS (" +
            "  SELECT b.docid, b.term, b.bm25 + COALESCE(d.pagerank, 0) AS combined_score" +
            "  FROM bm25_scores b LEFT JOIN documents d ON b.docid = d.docid" +
            ")" +
            "UPDATE features SET bm25 = combined.combined_score" +
            " FROM combined" +
            " WHERE features.docid = combined.docid AND features.term = combined.term";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }
    public void createViews() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE OR REPLACE VIEW features_bm25 AS SELECT docid, term, bm25 AS score FROM features");
            stmt.executeUpdate("CREATE OR REPLACE VIEW features_tfidf AS SELECT docid, term, tfidf AS score FROM features");
            System.out.println("Views features_bm25 + features_tfidf created.");
        } catch (SQLException e) {
            System.err.println("createViews error: " + e.getMessage());
        }
    }
    public void updateLanguageDocuments(String url, String lang) {
        String sql = "UPDATE documents SET lang = ? WHERE url = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, lang);
            ps.setString(2, url);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public String suggestionCorrectionIfNecessary(String searchedWord, String originalTerm) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT term FROM features WHERE term = ? LIMIT 1")) {
            ps.setString(1, searchedWord);
            if (ps.executeQuery().next()) return "";
        } catch (SQLException e) {
            throw new RuntimeException("suggestionCorrectionIfNecessary (check): " + e.getMessage(), e);
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE EXTENSION IF NOT EXISTS fuzzystrmatch");
        } catch (SQLException e) {
            throw new RuntimeException("fuzzystrmatch extension error: " + e.getMessage(), e);
        }
        String sql =
            "SELECT term, COUNT(*) AS frequency FROM features " +
            "WHERE levenshtein(term, ?) <= 1 " +
            "GROUP BY term ORDER BY frequency DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, searchedWord);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String corrected = rs.getString("term");
                System.out.println("Spell correction: '" + originalTerm + "' -> '" + corrected + "'");
                return corrected;
            }
        } catch (SQLException e) {
            throw new RuntimeException("suggestionCorrectionIfNecessary (levenshtein): " + e.getMessage(), e);
        }
        System.out.println("No correction found for: '" + originalTerm + "'");
        return "";
    }
}
