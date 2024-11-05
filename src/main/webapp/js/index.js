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

    $("button").on("click", function(event) {
        event.preventDefault()
        const k = 20;
        const query = {
            searchTerms: searchTerms,
            domainSiteTerms: searchSites,
            isConjunctive: false
        };

        const queryString = JSON.stringify(query);

        console.log(queryString)
        $.ajax({
            method: 'GET',
            url: '/webCrowler/search',
            data: {query:  JSON.stringify(query), k:k},
            success: function(response) {
                console.log(response);
            },
            error: function(e) {
                console.log(e)
            }
        })
    });
});