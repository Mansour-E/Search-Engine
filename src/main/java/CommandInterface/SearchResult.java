package CommandInterface;

public class SearchResult {
    private final int docID;
    private final String url;
    private final int score;

    public SearchResult(int docID, String url, int score) {
        this.docID = docID;
        this.url = url;
        this.score = score;
    }

}
