import React, { useState } from 'react';
import SearchPage from './components/SearchPage';
import ResultsPage from './components/ResultsPage';
import './App.css';

function App() {
  const [results, setResults] = useState(null);
  const [lastQuery, setLastQuery] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Rate limiter state (client-side, mirrors backend logic)
  const [requestTimestamps, setRequestTimestamps] = useState([]);

  const isRateLimited = () => {
    const now = Date.now();
    const recent = requestTimestamps.filter(t => now - t < 1000);
    return recent.length >= 10;
  };

  const handleSearch = async (queryParams) => {
    if (isRateLimited()) {
      setError('Rate limit exceeded. Please wait a moment before searching again.');
      return;
    }

    setLoading(true);
    setError(null);
    setLastQuery(queryParams);
    setRequestTimestamps(prev => [...prev.filter(t => Date.now() - t < 1000), Date.now()]);

    try {
      const query = {
        conjuctiveSearchTerms: queryParams.quotedTerms,
        disjunctiveSearchTerms: queryParams.unquotedTerms,
        domainSiteTerms: queryParams.sites,
        scoreOption: queryParams.scoreOption,
        languages: queryParams.languages,
      };

      const response = await fetch(
        `/is-project/search?query=${encodeURIComponent(JSON.stringify(query))}&k=20`,
        { method: 'GET' }
      );

      if (!response.ok) {
        throw new Error(`Server error: ${response.status}`);
      }

      const data = await response.json();
      setResults(data);
    } catch (err) {
      setError(err.message || 'An unexpected error occurred.');
      setResults(null);
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setResults(null);
    setLastQuery(null);
    setError(null);
  };

  return (
    <div className="app">
      {!results ? (
        <SearchPage onSearch={handleSearch} loading={loading} error={error} />
      ) : (
        <ResultsPage
          results={results}
          query={lastQuery}
          onSearch={handleSearch}
          onReset={handleReset}
          loading={loading}
          error={error}
        />
      )}
    </div>
  );
}

export default App;
