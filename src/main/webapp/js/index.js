$(function() {
    // RateLimiter Logic
    const RateLimiter = (() => {
        // Max 10 requests per second globally
        const MAX_GLOBAL_REQUESTS_PER_SECOND = 10;
        // Max 1 request per second per IP
        const MAX_REQUESTS_PER_SECOND_PER_IP = 1;

        const globalState = {
            globalRequestCount: 0,
            globalLastResetTime: Date.now(),
        };

        const ipState = {};

        function isAllowed(ip) {
            const currentTime = Date.now();

            // Reset global counters if 1 second has passed
            if (currentTime - globalState.globalLastResetTime >= 1000) {
                globalState.globalRequestCount = 0;
                globalState.globalLastResetTime = currentTime;
            }

            // Check global rate limit
            if (globalState.globalRequestCount >= MAX_GLOBAL_REQUESTS_PER_SECOND) {
                return false;
            }

            // Reset IP-specific counters if 1 second has passed
            if (!ipState[ip]) {
                ipState[ip] = { count: 0, lastResetTime: currentTime };
            } else if (currentTime - ipState[ip].lastResetTime >= 1000) {
                ipState[ip].count = 0;
                ipState[ip].lastResetTime = currentTime;
            }

            // Check IP-specific rate limit
            if (ipState[ip].count >= MAX_REQUESTS_PER_SECOND_PER_IP) {
                return false;
            }

            // Increment counters
            globalState.globalRequestCount++;
            ipState[ip].count++;

            return true;
        }

        return { isAllowed };
    })();


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

    // Version 1
    /*
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
     */


    // Version 2
    // AJAX with Rate-Limiter applied
    $("button").on("click", function(event) {
            const clientIp = "dummy-ip"; // Replace with a real method to get the client IP
            event.preventDefault();

            if (!RateLimiter.isAllowed(clientIp)) {
                alert("Rate limit exceeded. Please try again later.");
                return;
            }

            const parsedQuery = parseSearchQuery($("#searchQuery").val());
            const k = 20;

            let languages = $("#langBox .form-check-input:checked").map(function() {
                return $(this).val();
            }).get();

            if (languages.length === 0) {
                languages = ['English', "German"];
            }

            const scoreOption = $("#scoreOptions .form-check-input:checked").val();
            const displayOption = $("#displayOptions .form-check-input:checked").val();

            const query = {
                conjuctiveSearchTerms: parsedQuery.quotedTerms,
                disjunctiveSearchTerms: parsedQuery.unquotedTerms,
                domainSiteTerms: parsedQuery.sites,
                scoreOption: scoreOption,
                languages: languages,
            };

            $.ajax({
                method: 'GET',
                url: '/webCrowler/search',
                data: { query: JSON.stringify(query), k: k },
                success: function(response) {
                    if (displayOption === "jsonFile") {
                        const encodedResult = encodeURIComponent(JSON.stringify(response));
                        window.location.href = `/webCrowler/resultPage.html?data=${encodedResult}`;
                    } else {
                        displayResults(response);
                    }
                },
                error: function(e) {
                    console.log(e);
                }
            });
        });
});
