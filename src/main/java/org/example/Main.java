package org.example;

import Crawler.Crawler;
import DB.DBConnection;
import Sheet2.Classifier.Classifier;
import Sheet2.PageRank.PageRank;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import static CommandInterface.commandInterface.executeSearch;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {

        /*-----------------------------------------------------------------------------------------------------
        -----------------------sheet 1 -------------------------------------------------------------------------
        -------------------------------------------------------------------------------------------------------
         */

        // THIS PART BELONGS TO EXERCISE  1 + 2
        //

        // Start exercice1
        DBConnection db = new DBConnection("IS-Project", "postgres", "9157", true);

        String[] rootUrls = new String[]{"https://www.cs.rptu.de/en/studium/studiengaenge/bm-inf/sp.ma/", "https://rptu.de"};
        Crawler crawler = new Crawler( db, rootUrls , 2, 2, false );
        crawler.crawl();
        // String[] test = new String[]{"student", "study"};
        // executeSearch(db, test,false, 9 );

        // End exercice1



        // THIS PART BELONGS TO EXERCISE  3
        // Start exercice3
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
        // End exercice3


        /*-----------------------------------------------------------------------------------------------------
        -----------------------sheet 2 -------------------------------------------------------------------------
        -------------------------------------------------------------------------------------------------------
         */

        // THIS PART BELONGS TO EXERCISE  1
        // PageRank pr = new PageRank();
        // pr.calculatePageRanking(db);

    }

}