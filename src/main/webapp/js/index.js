$(function() {
    function parseSearchQuery(query) {
        // Matches 'site:example.com'
        const siteRegex = /site:([^\s]+)/g;
        // Matches "term"
        const quotedRegex = /"([^"]+)"/g;

        const sites = [];
        const quotedTerms = [];
        const unquotedTerms = [];

        // Extract sites
        let match;
        while ((match = siteRegex.exec(query)) !== null) {
            sites.push(match[1]);
        }

        // Extract quoted terms
        while ((match = quotedRegex.exec(query)) !== null) {
            quotedTerms.push(match[1]);
        }

       // Filter out terms that are part of site or quoted regex matches
       for (let term of query.split(/\s+/)) {
           if (!term.startsWith('"') && !term.startsWith('site:') && term.trim() !== '') {
               unquotedTerms.push(term);
           }
       }

        console.log('sites' , sites)
        console.log('quotedTerms' , quotedTerms)
        console.log('unquotedTerms' , unquotedTerms)


        return {
            sites,
            quotedTerms,
            unquotedTerms,
        };
    }
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
        const parsedQuery = parseSearchQuery($("#searchQuery").val());
        event.preventDefault()
        const k = 20;
        let languages = $("#langBox .form-check-input:checked").map(function() {
                                 return $(this).val();
                             }).get()
        if (languages.length == 0) {
           languages = ['English', "German"]
        }

        let scoreOption = $("#scoreOptions .form-check-input:checked").val()
        let displayOption = $("#displayOptions .form-check-input:checked").val()

        const query = {
            conjuctiveSearchTerms: parsedQuery.quotedTerms,
            disjunctiveSearchTerms: parsedQuery.unquotedTerms,
            domainSiteTerms: parsedQuery.sites,
            scoreOption: scoreOption,
            languages: languages
        };
        $.ajax({
            method: 'GET',
            url: '/webCrowler/search',
            data: {query:  JSON.stringify(query), k:k},
            success: function(response) {
            if(displayOption === "jsonFile") {
                // Redirect to the result page with the response as a query parameter
                const encodedResult = encodeURIComponent(JSON.stringify(response));
                window.location.href = `/webCrowler/resultPage.html?data=${encodedResult}`;
            }else{
                displayResults(response)
            }

            },
            error: function(e) {
                console.log(e)
            }
        })

    });
});
