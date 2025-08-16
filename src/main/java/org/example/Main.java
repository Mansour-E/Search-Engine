package org.example;

import CommandInterface.SearchResult;
import Crawler.Crawler;
import Crawler.NightCrawler;

import DB.DBConnection;
import Sheet2.Classifier.Classifier;
import Sheet2.PageRank.PageRank;

import java.io.IOException;
import java.sql.*;
import java.util.List;


import static CommandInterface.commandInterface.executeSearch;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {

        /*-----------------------------------------------------------------------------------------------------
        -----------------------sheet 1 -------------------------------------------------------------------------
        -------------------------------------------------------------------------------------------------------
         */

        DBConnection db = new DBConnection("IS-Project", "postgres", "9157", true);


        String[] rootUrls = new String[]{"http://sci.cs.uni-kl.de/",
                "http://dekanat.cs.rptu.de/en/",  "https://rptu.de"};
        Crawler crawler = new Crawler( db, rootUrls , 2, 10, true );
        //crawler.crawl();


        // NightCrawler nightCrawler = new NightCrawler( db, rootUrls, true  );
        // nightCrawler.crawl();



        String[] conjuctiveSearchTerms = new String[]{"student"};
        String[] disjunctiveSearchTerms = new String[]{""};

        List<String> languages = List.of("English");
        // List<SearchResult> results = db.disjunctiveCrawling(test, 5, languages,"BM25");
        List<SearchResult> results =  db.searchCrawling(conjuctiveSearchTerms, disjunctiveSearchTerms,5, languages, "BM25");


        for (SearchResult result : results) {
            System.out.printf("DocID: %d, URL: %s, Score: %.4f%n", result.getDocID(), result.getUrl(), result.getScore());
        }
        /*
         */


        /*-----------------------------------------------------------------------------------------------------
        -----------------------sheet 2 -------------------------------------------------------------------------
        -------------------------------------------------------------------------------------------------------
         */

        // THIS PART BELONGS TO EXERCISE  1
        PageRank pr = new PageRank();
        pr.calculatePageRanking(db);

    }

}
