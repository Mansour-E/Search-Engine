import React, { useState } from 'react';
import { parseSearchQuery } from '../utils/queryParser';
import styles from './SearchPage.module.css';

const SCORE_OPTIONS = [
  { value: 'tfidf', label: 'TF-IDF' },
  { value: 'BM25',  label: 'BM25'   },
];

const LANGUAGE_OPTIONS = [
  { value: 'English', label: 'English' },
  { value: 'German',  label: 'Deutsch' },
];

function SearchPage({ onSearch, loading, error }) {
  const [query, setQuery] = useState('');
  const [scoreOption, setScoreOption] = useState('BM25');
  const [languages, setLanguages] = useState(['English', 'German']);

  const toggleLanguage = (lang) => {
    setLanguages(prev =>
      prev.includes(lang) ? prev.filter(l => l !== lang) : [...prev, lang]
    );
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!query.trim()) return;
    const parsed = parseSearchQuery(query);
    onSearch({ ...parsed, scoreOption, languages: languages.length ? languages : ['English', 'German'] });
  };

  return (
    <div className={styles.page}>
      <div className={styles.container}>
        {/* Logo */}
        <div className={styles.logo}>
          <span className={styles.logoR}>R</span>
          <span className={styles.logoP}>P</span>
          <span className={styles.logoT}>T</span>
          <span className={styles.logoU}>U</span>
          <span className={styles.logoSearch}> Search</span>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          {/* Search input */}
          <div className={styles.inputWrapper}>
            <svg className={styles.searchIcon} xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
              <path d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/>
            </svg>
            <input
              className={styles.input}
              type="text"
              value={query}
              onChange={e => setQuery(e.target.value)}
              placeholder='Search... Use "term" for exact match, site:domain.com for site filter'
              autoFocus
            />
            {query && (
              <button type="button" className={styles.clearBtn} onClick={() => setQuery('')}>✕</button>
            )}
          </div>

          {/* Options row */}
          <div className={styles.optionsRow}>
            {/* Scoring */}
            <div className={styles.optionGroup}>
              <span className={styles.optionLabel}>Scoring:</span>
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

            {/* Language */}
            <div className={styles.optionGroup}>
              <span className={styles.optionLabel}>Language:</span>
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

          {/* Buttons */}
          <div className={styles.buttonRow}>
            <button
              type="submit"
              className={styles.searchBtn}
              disabled={loading || !query.trim()}
            >
              {loading ? 'Searching...' : 'Search'}
            </button>
          </div>

          {/* Hint */}
          <p className={styles.hint}>
            Tip: Use <code>"word"</code> for exact match (conjunctive) · <code>site:cs.rptu.de</code> to filter by domain
          </p>
        </form>

        {/* Error */}
        {error && <div className={styles.error}>⚠ {error}</div>}
      </div>
    </div>
  );
}

export default SearchPage;
