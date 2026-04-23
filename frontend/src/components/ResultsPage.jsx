import React, { useState } from 'react';
import { parseSearchQuery } from '../utils/queryParser';
import styles from './ResultsPage.module.css';

const SCORE_OPTIONS = [
  { value: 'tfidf', label: 'TF-IDF' },
  { value: 'BM25',  label: 'BM25'   },
];

const LANGUAGE_OPTIONS = [
  { value: 'English', label: 'English' },
  { value: 'German',  label: 'Deutsch' },
];

function ResultsPage({ results, query, onSearch, onReset, loading, error }) {
  const [inputValue, setInputValue] = useState(
    [...(query?.quotedTerms?.map(t => `"${t}"`) || []),
     ...(query?.unquotedTerms || []),
     ...(query?.sites?.map(s => `site:${s}`) || [])
    ].join(' ')
  );
  const [scoreOption, setScoreOption] = useState(query?.scoreOption || 'BM25');
  const [languages, setLanguages] = useState(query?.languages || ['English', 'German']);

  const toggleLanguage = (lang) => {
    setLanguages(prev =>
      prev.includes(lang) ? prev.filter(l => l !== lang) : [...prev, lang]
    );
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!inputValue.trim()) return;
    const parsed = parseSearchQuery(inputValue);
    onSearch({ ...parsed, scoreOption, languages: languages.length ? languages : ['English', 'German'] });
  };

  const resultList = results?.resultList || [];
  const stat       = results?.stat       || [];
  const cw         = results?.cw         ?? '—';

  return (
    <div className={styles.page}>
      {/* Top bar */}
      <header className={styles.header}>
        <button className={styles.logoBtn} onClick={onReset} title="Back to home">
          <span className={styles.logoR}>R</span>
          <span className={styles.logoP}>P</span>
          <span className={styles.logoT}>T</span>
          <span className={styles.logoU}>U</span>
        </button>

        <form className={styles.headerForm} onSubmit={handleSubmit}>
          <div className={styles.inputWrapper}>
            <svg className={styles.searchIcon} xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
              <path d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>
            </svg>
            <input
              className={styles.input}
              type="text"
              value={inputValue}
              onChange={e => setInputValue(e.target.value)}
              autoFocus
            />
            <button type="submit" className={styles.goBtn} disabled={loading}>
              {loading ? '…' : 'Search'}
            </button>
          </div>

          {/* Inline options */}
          <div className={styles.optionsRow}>
            <div className={styles.optionGroup}>
              {SCORE_OPTIONS.map(opt => (
                <label key={opt.value} className={styles.radioLabel}>
                  <input
                    type="radio"
                    name="score"
                    value={opt.value}
                    checked={scoreOption === opt.value}
                    onChange={() => setScoreOption(opt.value)}
                  />
                  {opt.label}
                </label>
              ))}
            </div>
            <div className={styles.optionGroup}>
              {LANGUAGE_OPTIONS.map(opt => (
                <label key={opt.value} className={styles.checkLabel}>
                  <input
                    type="checkbox"
                    checked={languages.includes(opt.value)}
                    onChange={() => toggleLanguage(opt.value)}
                  />
                  {opt.label}
                </label>
              ))}
            </div>
          </div>
        </form>
      </header>

      {/* Main content */}
      <main className={styles.main}>
        {/* Error */}
        {error && <div className={styles.error}>⚠ {error}</div>}

        {/* Stats bar */}
        {!error && (
          <div className={styles.statsBar}>
            <span>{resultList.length} result{resultList.length !== 1 ? 's' : ''}</span>
            <span className={styles.dot}>·</span>
            <span>Collection size: <strong>{cw.toLocaleString()}</strong> unique terms</span>
            {stat.map(s => (
              <span key={s.term} className={styles.termStat}>
                <span className={styles.dot}>·</span>
                <code>{s.term}</code>: {s.df} docs
              </span>
            ))}
          </div>
        )}

        {/* Result list */}
        {resultList.length > 0 ? (
          <ol className={styles.resultList}>
            {resultList.map((item) => (
              <ResultItem key={item.rank} item={item} />
            ))}
          </ol>
        ) : !loading && !error ? (
          <div className={styles.noResults}>
            <p>No results found for your query.</p>
            <p className={styles.noResultsHint}>
              Try different keywords, or remove quotes/filters.
            </p>
          </div>
        ) : null}
      </main>
    </div>
  );
}

function ResultItem({ item }) {
  const displayUrl = item.url.length > 70 ? item.url.slice(0, 70) + '…' : item.url;
  const domain = (() => {
    try { return new URL(item.url).hostname; } catch { return item.url; }
  })();

  return (
    <li className={styles.resultItem}>
      <div className={styles.resultDomain}>{domain}</div>
      <a className={styles.resultLink} href={item.url} target="_blank" rel="noreferrer">
        {displayUrl}
      </a>
      <div className={styles.resultMeta}>
        <span className={styles.rank}>#{item.rank}</span>
        <span className={styles.score}>Score: {Number(item.score).toFixed(4)}</span>
      </div>
    </li>
  );
}

export default ResultsPage;
