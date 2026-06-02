const BASE = '/api/market';

async function request(path) {
  const res = await fetch(`${BASE}${path}`);
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      if (body && body.error) message = body.error;
    } catch {
      // response had no JSON body
    }
    throw new Error(message);
  }
  return res.json();
}

const enc = encodeURIComponent;

// Alpha Vantage free tier allows ~1 request/second. Serialize AV calls and space
// them out so a dashboard load (history + overview) doesn't trip the burst limit.
const AV_MIN_INTERVAL = 1100;
let avQueue = Promise.resolve();
let lastAvCall = 0;

function avRequest(path) {
  const run = async () => {
    const wait = AV_MIN_INTERVAL - (Date.now() - lastAvCall);
    if (wait > 0) await new Promise((resolve) => setTimeout(resolve, wait));
    lastAvCall = Date.now();
    return request(path);
  };
  const result = avQueue.then(run, run);
  avQueue = result.catch(() => {});
  return result;
}

/** Finnhub: real-time quote -> GlobalQuoteDto (separate API, no AV throttle) */
export const getQuote = (symbol) => request(`/quote/${enc(symbol)}`);

/** Yahoo via backend (DB-cached): full 30-minute series -> MarketDataResponseDto.
 *  Resampled to intraday/weekly/monthly on the client. */
export const getSeries = (symbol) => request(`/history/${enc(symbol)}`);

/** Alpha Vantage: company overview -> CompanyOverviewDto */
export const getOverview = (symbol) => avRequest(`/overview/${enc(symbol)}`);

/** Alpha Vantage: ticker search -> SearchTickerDto[] */
export const searchSymbols = (query) => avRequest(`/search?q=${enc(query)}`);
