package CommandInterface;

public class SearchResult {
    private final int docID;
    private final String url;
    private final double score;

    public SearchResult(int docID, String url, double score) {
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

    public double getScore() {
        return score;
    }


}
