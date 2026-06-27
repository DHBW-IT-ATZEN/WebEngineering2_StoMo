/**
 * Master switch for the paid/limited APIs.
 *
 *   true  -> normal: Finnhub (quote) + Alpha Vantage (overview, search) are called.
 *   false -> "API saving" mode: those calls are skipped on the client.
 *            The chart still works because history comes from Yahoo (keyless).
 *
 * Flip this back to true to re-enable.
 */
export const LIVE_APIS = true;
