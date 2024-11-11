package DB;

import java.sql.*;
import java.util.*;

public class DBConnection {

    private Connection connection ;

    public DBConnection(String dbName, String dbOwner, String dbPassword ) {
        this.connection = this.connectToDb(dbName, dbOwner, dbPassword);
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

}




