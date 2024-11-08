<h1>Task 1</h1>
<p><strong>To Run:</strong> Go to <code>main.js</code> and uncomment the code section between the comments <code>// Start exercice1</code> and <code>// End exercice 1</code>.</p>

<br>
<br>

<h2>DB/DBConnection.java</h2>
<p>This file contains all the database operations necessary for managing crawler state and storage.</p>

<h3>Table: <code>documents</code></h3>
<p>This table stores each document's unique URL and the date it was crawled.</p>

<h3>Table: <code>features</code></h3>
<p>This table stores the terms extracted from each document and their respective frequencies.</p>

<h3>Table: <code>features</code></h3>
<p>This table stores the terms extracted from each document and their respective frequencies.</p>

<h3>Table: <code>crawledPagesQueueTable</code></h3>
<p>This table stores all URLs encountered by the crawler, along with their depth and visit status. Each page is marked as visited or not visited to allow the crawler to resume from the last saved state in case of interruption.</p>

<br>
<hr>
<br>

<h3>Table: <code>crawledPagesQueueTable</code></h3>
<p>This table stores all URLs encountered by the crawler, along with their depth and visit status. Each page is marked as visited or not visited to allow the crawler to resume from the last saved state in case of interruption.</p>

<h2>Crawler/Crawler.java</h2>
<p>Our crawler starts from a given set of URLs, exploring pages in a breadth-first manner. It indexes each page, extracts links and terms, and saves them to the database.</p>

<h3>Crawler Parameters</h3>
<ul>
    <li><strong>rootUrls</strong>: Initial URLs from which crawling begins.</li>
    <li><strong>depthToCrawl</strong>: Maximum depth for crawling to control how deep links are followed.</li>
    <li><strong>nbrToCrawl</strong>: Maximum number of documents to crawl to limit the crawl session size.</li>
    <li><strong>allowToLeaveDomains</strong>: Boolean flag indicating if the crawler can leave the specified domains.</li>
</ul>

<h3>Crawler Methods</h3>
<ul>
    <li><strong>crawl()</strong>: Manages the main crawl loop, retrieving URLs from the queue and launching threads to process each URL.</li>
    <li><strong>crawlPage()</strong>: Processes each URL by indexing its content, marking it as visited, and adding any new child URLs to the queue and database.</li>
    <li><strong>loadNotVisitedURL()</strong>: Loads all unvisited URLs from the database into the crawl queue for resuming a previous crawl.</li>
    <li><strong>isUrlAllowedToCrawl()</strong>: Checks if a URL is within the allowed domains or depth based on the crawler’s parameters.</li>
</ul>

<h3>Crawler Usage</h3>
<ul>
    <li><strong>Initialize</strong> the crawler with the desired root URLs and parameters.</li>
    <li>The crawler loads any previously unvisited URLs from the database. If it’s a fresh crawl, it starts with the specified root URLs.</li>
    <li><strong>Start the crawling session</strong>, which continues until the crawler reaches the specified <code>nbrToCrawl</code> or <code>depthToCrawl</code> limits.</li>
    <li>During crawling, <strong>visited pages are marked</strong> in the <code>crawledPagesQueueTable</code> database table to ensure they are not revisited.</li>
    <li>The crawler can <strong>resume from its last state</strong> by reading the <code>state</code> of each URL in the database, ensuring uninterrupted progress.</li>
</ul>

<br>
<hr>
<br>

<h2>Indexer</h2>
<p>The <code>Indexer</code> class processes HTML content by extracting terms and links, then stores them in the database</p>

<h3>Indexer Parameters</h3>
<ul>
    <li><strong>htmlContent</strong>: The HTML content of the page being indexed.</li>
    <li><strong>rootDocID</strong>: The document ID of the current page being indexed, used as a parent-reference for links and terms.</li>
</ul>

<h3>Indexer Methods</h3>
<ul>
    <li><strong>indexHTMlContent()</strong>: This method performs the core indexing functionality by parsing terms and links from the HTML content and storing them in the database.
    </li>
</ul>

<h3>Indexer Usage</h3>
<ul>
    <li>Extracts terms from the page, calculates their frequencies, and stores them in the <code>features</code> table.</li>
    <li>Extracts links from the page, stores each link in the <code>documents</code> table, and inserts relationships between the current document and linked documents in the <code>links</code> table.</li>
</ul>

<br>
<hr>
<br>

<h2>Parser</h2>
<p>The <code>Parser</code> class is responsible for extracting and processing content and links from HTML documents. It cleans terms, removes stopwords, stems words, and verifies link validity before returning the data for indexing.</p>

<h2>Parser Parameters</h2>
<ul>
    <li><strong>doc</strong>: Parsed HTML document using Jsoup, representing the HTML content.</li>
</ul>

<h2>Parser Methods</h2>
<ul>
    <li><strong>stemWord</strong>: Static method that applies stemming to a given word using the <code>Stemmer</code> class.</li>
    <li><strong>parseContent()</strong>: Extracts terms from the HTML document’s body content. The terms are cleaned, filtered for stopwords, stemmed, and counted, returning a map of terms with their frequencies.</li>
    <li><strong>parseLinks()</strong>: Extracts links from <code>&lt;a&gt;</code> tags within the HTML document. Valid URLs are added to the <code>linkElements</code> list, ensuring only legitimate links are returned.</li>
    <li><strong>isValidUrl(String url)</strong>: Checks if a given URL matches a standard URL pattern, ensuring only valid URLs are processed.</li>
</ul>

<br>
<br>
<h1>Task 2</h1>
<br>
<br>


<br>
<br>
<h1>Task 3</h1>
<p><strong>To Run:</strong> Go to <code>main.js</code> and uncomment the code section between the comments <code>// Start exercice3</code> and <code>// End exercice 3</code>.</p>
<br>
<br>

<p>In this task, we implemented a CLI interface (<code>main.js</code>) to interact with the database and perform search operations.</p>

<h2>Search Functions</h2>
<p>These functions handle the search logic for conjunctive and disjunctive queries. Both functions take an array of search terms and a maximum result size, and they return a list of matching documents ranked by relevance score.</p>

<h3>Function: <code>conjunctiveCrawling(String[] searchedTerms, int resultSize)</code></h3>
<p>This function performs a conjunctive search, where only documents containing <strong>all</strong> of the search terms are returned.</p>

<ul>
    <li>Each search term is stemmed to enhance matching consistency.</li>
    <li>A conjunctive SQL query is built using the stemmed terms:
        <ul>
            <li>The query selects documents where the count of distinct matching terms equals the number of search terms (ensuring all terms are present).</li>
            <li>Results are ordered by relevance score (highest first) and limited to the specified <code>resultSize</code>.</li>
        </ul>
    </li>
</ul>

<h3>Function: <code>disjunctiveCrawling(String[] searchedTerms, int resultSize)</code></h3>
<p>This function performs a disjunctive search, where documents containing <strong>any</strong> of the search terms are returned, ranked by relevance.</p>

<ul>
    <li>Each search term is stemmed for consistent matching.</li>
    <li>A disjunctive SQL query is built using the stemmed terms:
        <ul>
            <li>The query selects documents where at least one of the search terms is present.</li>
            <li>Results are ordered by relevance score and limited to the specified <code>resultSize</code>.</li>
        </ul>
    </li>
    <li>The function prepares and executes the query, then collects results into a list of <code>SearchResult</code> objects.</li>
</ul>

<hr>
<h2>CLI Workflow</h2>
<ul>
    <li>When you run <code>main.js</code>, you will first be prompted to enter your database connection details:
        <ul>
            <li><strong>Database name</strong>: The name of the database.</li>
            <li><strong>Database owner</strong>: The username or owner of the database.</li>
            <li><strong>Database password</strong>: The password for the database owner.</li>
        </ul>
    </li>
    <li>After connecting to the database, you will be prompted to enter search terms (separate multiple terms with spaces).</li>
    <li>You will then specify the <strong>result size</strong>, which indicates the maximum number of URLs you want in the search results.</li>
    <li>Next, you will specify if the search should be <strong>conjunctive</strong> (all terms must be present) or <strong>disjunctive</strong> (any term can be present) by entering <code>true</code> or <code>false</code>.</li>
    <li>This process will repeat until you type <code>exit</code> to quit the program.</li>
</ul>



