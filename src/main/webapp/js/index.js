$(function() {


    function displayResults(response) {
        console.log("displayResults", response)
        $("#resultsContainer").empty();

        if (response.resultList && response.resultList.length > 0) {
            const resultList = $('<ul class="list-group"></ul>');

            response.resultList.forEach(item => {
                const listItem = $(`
                    <li class="list-group-item">
                        <strong>Rank:</strong> ${item.rank} -
                        <a href="${item.url}" target="_blank">${item.url}</a>
                        <span class="badge badge-info ml-2">${item.score}</span>
                    </li>
                `);
                resultList.append(listItem);

            });

            $("#resultsContainer").append(resultList);
        } else {
            $("#resultsContainer").append('<p class="text-muted">No results found.</p>');
        }
    }

    $("button").on("click", function(event) {
        event.preventDefault()
        const k = 20;

        const query = {
            searchTerms: $("#searchQuery").val().trim().split(/\s+/),
            domainSiteTerms: $("#siteQuery").val().trim().split(/\s+/),
            isConjunctive: $("#conjunctiveCheck").is(":checked")
        };

        $.ajax({
            method: 'GET',
            url: '/webCrowler/search',
            data: {query:  JSON.stringify(query), k:k},
            success: function(response) {
                displayResults(response);
            },
            error: function(e) {
                console.log(e)
            }
        })

    });
});