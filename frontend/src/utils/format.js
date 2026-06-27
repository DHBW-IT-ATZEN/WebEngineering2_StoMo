const DASH = '—';

/**
 * Format a price that is already expressed in `currency`, localised for `locale`. Uses the
 * currency's natural precision (2 decimals for USD/EUR, 0 for JPY, …). Falls back gracefully
 * for unknown currency codes. Conversion between currencies is the CurrencyProvider's job.
 */
export function formatPrice(value, currency, locale = 'en-US') {
  if (value === null || value === undefined || Number.isNaN(value)) return DASH;
  const code = currency || 'USD';
  try {
    return new Intl.NumberFormat(locale, { style: 'currency', currency: code }).format(Number(value));
  } catch {
    return `${Number(value).toLocaleString(locale)} ${code}`;
  }
}

/** Format an index level as points — a localised number with NO currency symbol (indices aren't money). */
export function formatPoints(value, locale = 'en-US') {
  if (value === null || value === undefined || Number.isNaN(value)) return DASH;
  return new Intl.NumberFormat(locale, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(value));
}

export function formatPercent(value) {
  if (value === null || value === undefined || Number.isNaN(value)) return DASH;
  const sign = value > 0 ? '+' : '';
  return `${sign}${Number(value).toFixed(2)}%`;
}

export function formatNumber(value, digits = 2) {
  if (value === null || value === undefined || Number.isNaN(value)) return DASH;
  return Number(value).toFixed(digits);
}

function compact(value, prefix = '') {
  const abs = Math.abs(value);
  const units = [
    { limit: 1e12, suffix: 'T' },
    { limit: 1e9, suffix: 'B' },
    { limit: 1e6, suffix: 'M' },
    { limit: 1e3, suffix: 'K' },
  ];
  for (const { limit, suffix } of units) {
    if (abs >= limit) return `${prefix}${(value / limit).toFixed(2)}${suffix}`;
  }
  return `${prefix}${value.toFixed(0)}`;
}

export function formatMarketCap(value) {
  if (value === null || value === undefined || Number.isNaN(value)) return DASH;
  return compact(Number(value), '$');
}

export function formatVolume(value) {
  if (value === null || value === undefined || Number.isNaN(value)) return DASH;
  return compact(Number(value));
}

/** Alpha Vantage returns DividendYield as a fraction (0.0052 = 0.52%). */
export function formatYield(value) {
  if (value === null || value === undefined || Number.isNaN(value)) return DASH;
  return `${(Number(value) * 100).toFixed(2)}%`;
}

// Yahoo prefixes market indices with a caret (e.g. ^DJI, ^GSPC). That caret is required for
// data lookups but cryptic in the UI, so map the common ones to names and drop the caret on
// anything else. Display-only — the raw symbol is still used for routing and API calls.
const INDEX_NAMES = {
  '^GSPC': 'S&P 500',
  '^IXIC': 'Nasdaq',
  '^DJI': 'Dow Jones',
  '^GDAXI': 'DAX',
  '^N225': 'Nikkei 225',
  '^RUT': 'Russell 2000',
  '^FTSE': 'FTSE 100',
  '^FCHI': 'CAC 40',
  '^HSI': 'Hang Seng',
  '^STOXX50E': 'Euro Stoxx 50',
};

/** Human-friendly label for a ticker: index name when known, otherwise the symbol sans caret. */
export function displaySymbol(symbol) {
  if (!symbol) return '';
  return INDEX_NAMES[symbol] ?? (symbol.startsWith('^') ? symbol.slice(1) : symbol);
}
