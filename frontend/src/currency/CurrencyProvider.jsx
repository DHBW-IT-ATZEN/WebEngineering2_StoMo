import { useCallback, useEffect, useMemo, useState } from 'react';
import { CurrencyContext } from './currencyContext';
import { getFxRates } from '../api/marketData';
import { formatPoints, formatPrice } from '../utils/format';
import { useLanguage } from '../i18n/useLanguage';

const STORAGE_KEY = 'stomo-currency';
// Currencies offered before /api/market/fx responds.
const DEFAULT_OPTIONS = ['USD', 'EUR', 'GBP', 'JPY', 'CHF'];

function getInitialCurrency() {
  return window.localStorage.getItem(STORAGE_KEY) || 'USD';
}

/**
 * Holds the user's chosen display currency and the start-of-day FX rates (USD per unit) from
 * /api/market/fx. `formatMoney(amount, nativeCurrency)` converts native -> USD -> display and
 * formats it. Until rates load (or if a rate is missing) it shows the amount in its native
 * currency, so prices are never blank or mislabeled.
 */
export function CurrencyProvider({ children }) {
  const { locale } = useLanguage();
  const [displayCurrency, setDisplayCurrencyState] = useState(getInitialCurrency);
  const [rates, setRates] = useState({ USD: 1 });
  const [options, setOptions] = useState(DEFAULT_OPTIONS);

  useEffect(() => {
    let active = true;
    getFxRates()
      .then((data) => {
        if (!active || !data) return;
        if (data.rates && typeof data.rates === 'object') setRates(data.rates);
        if (Array.isArray(data.displayOptions) && data.displayOptions.length > 0) {
          setOptions(data.displayOptions);
        }
      })
      .catch(() => { /* keep defaults; formatMoney falls back to the native currency */ });
    return () => { active = false; };
  }, []);

  const setDisplayCurrency = useCallback((code) => {
    setDisplayCurrencyState(code);
    window.localStorage.setItem(STORAGE_KEY, code);
  }, []);

  const formatMoney = useCallback(
    (amount, fromCurrency, type) => {
      // An index level is points, not money — no currency symbol, no FX conversion.
      if (type === 'INDEX') return formatPoints(amount, locale);
      const from = fromCurrency || 'USD';
      const rateFrom = rates[from];
      const rateTo = rates[displayCurrency];
      if (amount != null && !Number.isNaN(amount) && rateFrom && rateTo) {
        return formatPrice((amount * rateFrom) / rateTo, displayCurrency, locale);
      }
      return formatPrice(amount, from, locale); // can't convert -> show native (or dash)
    },
    [rates, displayCurrency, locale],
  );

  const value = useMemo(
    () => ({ displayCurrency, setDisplayCurrency, options, rates, formatMoney }),
    [displayCurrency, setDisplayCurrency, options, rates, formatMoney],
  );

  return <CurrencyContext.Provider value={value}>{children}</CurrencyContext.Provider>;
}
