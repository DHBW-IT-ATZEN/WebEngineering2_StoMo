import { useLanguage } from '../i18n/useLanguage';

/** Language switch (EN/DE) shared by the landing page and the in-app header. */
export default function LanguageSelect() {
  const { language, setLanguage } = useLanguage();
  return (
    <select
      value={language}
      onChange={(event) => setLanguage(event.target.value)}
      title="Language"
      aria-label="Language"
      className="bg-surface-container-high text-on-surface text-xs font-bold uppercase rounded-lg px-2 py-2.5 border border-outline-variant/30 cursor-pointer hover:text-primary focus:border-primary outline-none transition-colors"
    >
      <option value="en">EN</option>
      <option value="de">DE</option>
    </select>
  );
}
