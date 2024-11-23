package CommandInterface;

public class SearchResult {
    private final int docID;
    private final String url;
    private final int score;
    private double combinedScore;


    public SearchResult(int docID, String url, int score ) {
        this.docID = docID;
        this.url = url;
        this.score = score;
    }

    public int getDocID() {
        return docID;
    }

    public String getUrl() {
        return url;
    }

    public int getScore() {
        return score;
    }

    public double getCombinedScore() {
        return combinedScore;
    }

    public void setCombinedScore(double combinedScore) {
        this.combinedScore = combinedScore;
    }

}
