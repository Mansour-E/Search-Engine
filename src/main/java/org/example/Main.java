package org.example;

import Crawler.Crawler;
import DB.DBConnection;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import static CommandInterface.commandInterface.executeSearch;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {


        // THIS PART BELONGS TO EXERCISE  1 + 2

        DBConnection db = new DBConnection("IS-Project", "postgres", "Yessin.10");

        Crawler crawler = new Crawler( db, "https://en.wikipedia.org/wiki/Parnassia_palustris", 3, 3, true );
        crawler.crawl();

        // THIS PART BELONGS TO EXERCISE  3
        /*
        Scanner scanner = new Scanner(System.in);
        try {
            // create the connection with Db
            System.out.print("Enter the Database name:");
            String dbUrl = scanner.nextLine();
            System.out.print("Enter Database Owner: ");
            String dbOwner = scanner.nextLine();
            System.out.print("Enter Databse password: ");
            String dbPassword = scanner.nextLine();

            DBConnection db = new DBConnection(dbUrl,dbOwner, dbPassword );

            while (true) {
                System.out.print("Enter search terms (or type 'exit' to quit): ");
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("exit")) break;

                String[] searchedTerms = input.split("\\s+");
                System.out.print("Enter result size: ");
                int resultSize = scanner.nextInt();
                System.out.print("Conjunctive mode (true/false)? ");
                boolean isConjunctive = scanner.nextBoolean();
                scanner.nextLine();
                executeSearch(db, searchedTerms,isConjunctive, resultSize );

            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
        */
    }

}