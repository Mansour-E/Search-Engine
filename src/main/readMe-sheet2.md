<h1>Task 1</h1>

<br>
<br>

<p> The aim of this exercise is to calculate the PageRank for each document using the Power Iteration Method <br>
<p> The first step is to create the link matrix, which represents the probabilities of moving from one page (document) to another.
<ul> The function createLinkMatrix performs this initialization:
  <li> It queries the database to fetch all documents and their outgoing links.
  <li> For each document, it maps its unique ID to an index in the matrix.
 <li> If a document has no outgoing links, its teleportation probability is evenly distributed across all other nodes.
 <li> Otherwise, it calculates the link probability for each outgoing link and combines it with the teleportation probability.
</ul>

<p>After creating the link matrix, the rank vector is initialized. This vector represents the rank scores for all pages, starting with equal values for every page.</p>
<p>Using the initialized rank vector, the program performs matrix-vector multiplications trials times to iteratively refine the rank scores.

<br>
<hr>
<br>

<h1>Task 2.1 and 2.2: Advanced Scoring Models</h1>
    <br>
    <p>In this task, I worked on implementing and integrating **BM25** and combining it with other scoring models like TF-IDF.</p>
    <ul>
        <li>i implemented calculateBM25InDatabase() to calculate the BM25 score and use that in recomputing and in feature table </li>
        <li>I updated the database schema to include columns for <code>bm25</code>, <code>tfidf</code>, and other scoring values in the <code>features</code> table.</li>
        <li>Created a dynamic selection mechanism to switch between scoring models (e.g., BM25 or TF-IDF) based on user input.</li>
        <li>The <code>conjunctiveCrawling</code> and <code>disjunctiveCrawling</code> methods were extended to support the following:</li>
        <ul>
            <li>Fetching results using the selected scoring method (TF-IDF or BM25).</li>
            <li>Calculating scores based on the user’s selected language filter (e.g., English or German).</li>
            <li>Returning ranked results limited by a user-defined size.</li>
        </ul>
        <li>The front-end interface (<code>index.html</code>) was updated to include two checkboxes for score selection, one for <code>TF-IDF</code> and another for <code>BM25</code>.</li>
        <li>Developed a method to recompute BM25 values after each crawling session to keep the scoring model updated.</li>
    </ul>
    <p>Overall, this task enhances the retrieval system’s ability to rank results using advanced scoring methods, ensuring better search accuracy for the user.</p>
    <br>
    <hr>
    <br>
    <h1>Task 3: Deployment</h1>
    <br>
    <p>The goal of this task was to deploy the search engine application on a virtual machine and configure it for public access.</p>
    <ul>
        <li>Installed and configured the necessary software components on the server, including:</li>
        <ul>
            <li>PostgreSQL database.</li>
            <li>Java Development Kit (JDK).</li>
            <li>Apache Tomcat for hosting the application.</li>
        </ul>
        <li>Deployed the WAR file (named as <code>is-project.war</code>) to the Tomcat server, making the application accessible at the root URL.</li>
        <li>Implemented rate-limiting mechanisms to handle queries:</li>
        <li>i make cron job in server and wrote 2 bashes to crawling 1 min in the night </li>
        <li>index.js is changed for blocking 2 time per second from same IP or 10 time global requests</li>
        <li>i set firewall for in university reachablity</li>
        <ul>
            <li>Global limit of 10 queries per second.</li>
            <li>Per-IP limit of 1 query per second.</li>
        </ul>
        <li>i implemened in server log files to check every nights happening and automatic crawling with jar file</li>
        <li>Set up endpoints for the application:</li>
        <ul>
            <li>HTML Interface: <code>/is-project/index.html</code>.</li>
            <li>JSON API: <code>/is-project/json?query=keywords&k=K&score=S</code>.</li>
        </ul>
    </ul>
    <p>The deployment was successful, and the system is now accessible at <a href="http://isproj-vm03.cs.uni-kl.de:8080/">http://isproj-vm03.cs.uni-kl.de:8080/</a>.</p>
    <br>
    <hr>
    <br>
<br>
<hr>
<br>
<h1>Task 4 a </h1>
<br>
<br>

<p>In this task, we implement a classifier that identifies the language assigned to each document.

<p> First, we create two files containing the most popular English and German words.
<p> Then, we define a satisfaction confidence threshold and loop over each scope in the body of the document.
<p> We calculate the number of English and German words represented in the document.
<p> If we reach our satisfaction threshold, we assign the document to the corresponding language.
<p> Otherwise, we loop through the entire document and assign the language with the higher word count.

<h1>Task 4 b </h1>
<br>
<br>

In this task, I implemented the suggestionCorrectionIfNecessary function to check whether a given word exists in the database and suggest a correction if necessary.

First, I check if the word is present in the term column of the features table. 
If the word exists, no correction is made, and the function simply returns an empty string.
If the word does not exist, I use the Levenshtein distance to find similar terms in the database.
Finally, I retrieve the closest match, prioritizing terms with the highest frequency, and return it as the suggested correction.

