/**
 * Parst eine Suchanfrage in:
 *  - sites:        site:example.com Teile
 *  - quotedTerms:  "term"           Teile (konjunktiv)
 *  - unquotedTerms: normale Wörter  (disjunktiv)
 */
export function parseSearchQuery(rawQuery) {
  const siteRegex = /site:([^\s]+)/g;
  const quotedRegex = /"([^"]+)"/g;

  const sites = [];
  const quotedTerms = [];
  const unquotedTerms = [];

  let match;
  while ((match = siteRegex.exec(rawQuery)) !== null) {
    sites.push(match[1]);
  }
  while ((match = quotedRegex.exec(rawQuery)) !== null) {
    quotedTerms.push(match[1]);
  }

  // Alles, was nicht site: oder "..." ist
  const cleaned = rawQuery
    .replace(/site:[^\s]+/g, '')
    .replace(/"[^"]+"/g, '')
    .trim();

  for (const term of cleaned.split(/\s+/)) {
    if (term.length > 0) {
      unquotedTerms.push(term);
    }
  }

  return { sites, quotedTerms, unquotedTerms };
}
