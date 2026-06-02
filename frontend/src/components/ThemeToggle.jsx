import { Leaf, Moon } from 'lucide-react';
import { useTheme } from '../theme/useTheme';

export default function ThemeToggle() {
  const { isYoda, toggleTheme } = useTheme();

  return (
    <button
      type="button"
      onClick={toggleTheme}
      aria-pressed={isYoda}
      aria-label={isYoda ? 'Switch to Dark mode' : 'Switch to Yoda mode'}
      title={isYoda ? 'Yoda Mode active — switch to Dark' : 'Dark Mode active — switch to Yoda'}
      className="flex items-center gap-3 bg-surface-container-high px-4 py-2 rounded-full cursor-pointer hover:bg-surface-container-highest transition-colors"
    >
      <Moon className={`w-4 h-4 transition-colors ${isYoda ? 'text-on-surface-variant' : 'text-primary'}`} />
      <span className="hidden sm:inline text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
        {isYoda ? 'Yoda Mode' : 'Dark Mode'}
      </span>
      <div
        className={`w-10 h-5 rounded-full relative p-1 flex items-center transition-all duration-300 ${
          isYoda ? 'bg-primary justify-end' : 'bg-primary/20 justify-start border border-primary/30'
        }`}
      >
        <div className={`w-3 h-3 rounded-full shadow-sm transition-colors ${isYoda ? 'bg-surface-bright' : 'bg-primary'}`} />
      </div>
      <Leaf className={`w-4 h-4 transition-colors ${isYoda ? 'text-primary' : 'text-on-surface-variant'}`} />
    </button>
  );
}
