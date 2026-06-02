// Client-side resampling of the cached series into the three views.
// Intraday uses the 10-min "fine" series; weekly/monthly use the 30-min "coarse" series.

const dayOf = (date) => (date || '').slice(0, 10);

function aggregate(bars) {
  if (!bars || bars.length === 0) return null;
  const first = bars[0];
  const last = bars[bars.length - 1];
  let high = -Infinity;
  let low = Infinity;
  let volume = 0;
  let hasVolume = false;
  for (const bar of bars) {
    if (bar.high != null) high = Math.max(high, bar.high);
    if (bar.low != null) low = Math.min(low, bar.low);
    if (bar.volume != null) {
      volume += bar.volume;
      hasVolume = true;
    }
  }
  return {
    date: last.date,
    open: first.open,
    high: Number.isFinite(high) ? high : last.close,
    low: Number.isFinite(low) ? low : last.close,
    close: last.close,
    volume: hasVolume ? volume : null,
  };
}

function groupByDay(bars) {
  const map = new Map();
  for (const bar of bars) {
    const key = dayOf(bar.date);
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(bar);
  }
  return map; // insertion order is chronological (bars arrive ascending)
}

/**
 * @param series { coarse: 30-min bars, fine: 10-min bars }
 * @param view 'INTRADAY' | 'WEEKLY' | 'MONTHLY'
 */
export function resampleSeries(series, view) {
  const coarse = series?.coarse ?? [];
  const fine = series?.fine ?? [];

  if (view === 'INTRADAY') {
    // Latest trading day's 10-minute candles, as-is.
    const byDay = groupByDay(fine);
    const days = [...byDay.keys()];
    return days.length ? byDay.get(days[days.length - 1]) ?? [] : [];
  }

  if (view === 'WEEKLY') {
    // Last 7 trading days, each split into 5 candles (~35 total).
    const byDay = groupByDay(coarse);
    const out = [];
    for (const day of [...byDay.keys()].slice(-7)) {
      const dayBars = byDay.get(day);
      const n = dayBars.length;
      for (let i = 0; i < 5; i += 1) {
        const slice = dayBars.slice(Math.floor((i * n) / 5), Math.floor(((i + 1) * n) / 5));
        const candle = aggregate(slice);
        if (candle) out.push(candle);
      }
    }
    return out;
  }

  // MONTHLY: last 30 trading days, one candle per day.
  const byDay = groupByDay(coarse);
  return [...byDay.keys()].slice(-30).map((day) => aggregate(byDay.get(day))).filter(Boolean);
}

/** Total volume of the most recent trading day (for the "Vol" stat), from the 30-min series. */
export function latestDayVolume(series) {
  const coarse = series?.coarse ?? [];
  if (coarse.length === 0) return null;
  const byDay = groupByDay(coarse);
  const days = [...byDay.keys()];
  const lastDay = byDay.get(days[days.length - 1]);
  let volume = 0;
  let hasVolume = false;
  for (const bar of lastDay) {
    if (bar.volume != null) {
      volume += bar.volume;
      hasVolume = true;
    }
  }
  return hasVolume ? volume : null;
}
