const DASH = '—';

export function formatPrice(value) {
  if (value === null || value === undefined || Number.isNaN(value)) return DASH;
  return `$${Number(value).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
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
