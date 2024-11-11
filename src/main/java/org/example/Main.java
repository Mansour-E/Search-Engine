package org.example;
import DB.DBConnection;
import PageRank.PageRank;

public class Main {
    public static void main(String[] args) {

        DBConnection db = new DBConnection("IS-Project", "postgres", "Yessin.10");

        PageRank pr = new PageRank();
        pr.calculatePageRanking(db);
    }
}