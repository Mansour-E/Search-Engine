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

<h1>Task 4 a </h1>
<br>
<br>

<p>In this task, we implement a classifier that identifies the language assigned to each document.

<p> First, we create two files containing the most popular English and German words.
<p> Then, we define a satisfaction confidence threshold and loop over each scope in the body of the document.
<p> We calculate the number of English and German words represented in the document.
<p> If we reach our satisfaction threshold, we assign the document to the corresponding language.
<p> Otherwise, we loop through the entire document and assign the language with the higher word count.