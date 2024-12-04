package org.example;

import CommandInterface.SearchResult;
import Crawler.Crawler;
import DB.DBConnection;
import Sheet2.Classifier.Classifier;
import Sheet2.PageRank.PageRank;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static CommandInterface.commandInterface.executeSearch;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {

        /*-----------------------------------------------------------------------------------------------------
        -----------------------sheet 1 -------------------------------------------------------------------------
        -------------------------------------------------------------------------------------------------------
         */

        DBConnection db = new DBConnection("IS-Project", "postgres", "Yessin.10", true);


        String[] rootUrls = new String[]{"https://www.cs.rptu.de/en/studium/studiengaenge/bm-inf/sp.ma/", "https://rptu.de"};
        Crawler crawler = new Crawler( db, rootUrls , 2, 2, false );
        // crawler.crawl();

        String[] conjuctiveSearchTerms = new String[]{"study"};
        String[] disjunctiveSearchTerms = new String[]{"student"};

        List<String> languages = List.of("English");
        // List<SearchResult> results = db.disjunctiveCrawling(test, 5, languages,"BM25");
        List<SearchResult> results =  db.searchCrawling(conjuctiveSearchTerms, disjunctiveSearchTerms,5, languages, "BM25");


        for (SearchResult result : results) {
            System.out.printf("DocID: %d, URL: %s, Score: %.4f%n", result.getDocID(), result.getUrl(), result.getScore());
        }


        /*-----------------------------------------------------------------------------------------------------
        -----------------------sheet 2 -------------------------------------------------------------------------
        -------------------------------------------------------------------------------------------------------
         */

        // THIS PART BELONGS TO EXERCISE  1
        PageRank pr = new PageRank();
        pr.calculatePageRanking(db);

    }

}