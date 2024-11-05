$(function() {
    const searchTerms = [];
    const searchSites = [];
    const searchResult =  document.getElementById('searchResult');
    const siteResult =  document.getElementById('siteResult');


    $("#searchQuery").on('keydown', (e) => {
      if (e.key === ' ' || e.key === 'Enter') {
          e.preventDefault();
          let input = e.target.value.trim();
          if (input) {
              searchTerms.push(input);
              e.target.value = '';
              const liTag = document.createElement("li");
              const textnode = document.createTextNode(input);
              liTag.appendChild(textnode);
              searchResult.appendChild(liTag);
              console.log('Current search terms:', searchTerms);

          }
      }
    });
     $("#siteQuery").on('keydown', (e) => {
      if (e.key === ' ' || e.key === 'Enter') {
          e.preventDefault();
          let input = e.target.value.trim();
          if (input) {
              searchSites.push(input);
              e.target.value = '';
              const liTag = document.createElement("li");
              const textnode = document.createTextNode(input);
              liTag.appendChild(textnode);
              siteResult.appendChild(liTag);
              console.log('Current search terms:', searchSites);

          }
      }
    });

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
            console.log($("#resultsContainer"))
        } else {
            $("#resultsContainer").append('<p class="text-muted">No results found.</p>');
        }
    }

    $("button").on("click", function(event) {
        event.preventDefault()
        const k = 20;
        const query = {
            searchTerms: searchTerms,
            domainSiteTerms: searchSites,
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