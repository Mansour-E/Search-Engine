package DB;
import java.sql.*;
import java.util.List;


public class DBConnection {

    public Connection connect_to_db(String dbname, String user, String pass) {

        Connection connection = null;
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/" + dbname, user, pass);
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

    public void createDocumentsTable(Connection connection)  {
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

    public void createFeaturesTable(Connection connection)  {
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

    public void createLinksTable(Connection connection)  {
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

    public void createTables(Connection connection) {
        this.createDocumentsTable(connection);
        this.createFeaturesTable(connection);
        this.createLinksTable(connection);
    }

    public int insertDocument(Connection connection, String url, String crawledDate) {
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

    public void insertLink(Connection connection, int fromDocid, int toDocid) {
        String insertQuery = "INSERT INTO links (from_docid, to_docid) VALUES (?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            preparedStatement.setInt(1, fromDocid);
            preparedStatement.setInt(2, toDocid);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void insertFeature(Connection connection, int docId, String term , int termFrequuency) {
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


}


