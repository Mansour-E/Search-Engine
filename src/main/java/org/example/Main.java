package org.example;

import Crawler.Crawler;
import DB.DBConnection;

import java.io.IOException;
import java.sql.*;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {

        DBConnection db = new DBConnection();
        Connection connection = db.connect_to_db("IS-Project", "postgres", "***");

        db.createTables(connection);

        Crawler crawler = new Crawler( db, connection, "https://jtidy.sourceforge.net/", 3, 1, true );
        crawler.crawl();
    }
}