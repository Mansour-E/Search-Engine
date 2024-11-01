document.getElementById('search-form').addEventListener('submit', async (event) => {
    event.preventDefault(); // Verhindert das Neuladen der Seite

    const query = document.getElementById('query').value;
    const response = await fetch(`/my-search-engine-project/SearchServlet?query=${encodeURIComponent(query)}`);
    const results = await response.json();

    displayResults(results);
});

function displayResults(results) {
    const resultsContainer = document.getElementById('results');
    resultsContainer.innerHTML = ''; // Alte Ergebnisse löschen

    results.slice(0, 20).forEach(result => {
        const resultItem = document.createElement('div');
        resultItem.innerHTML = `
            <h3><a href="${result.url}" target="_blank">${result.url}</a></h3>
            <p>Score: ${result.score}</p>
        `;
        resultsContainer.appendChild(resultItem);
    });
}