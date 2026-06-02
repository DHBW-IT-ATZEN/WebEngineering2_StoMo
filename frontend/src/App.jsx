import { ThemeProvider } from './theme/ThemeProvider';
import { YodaTextProvider } from './theme/YodaTextProvider';
import Dashboard from './components/Dashboard';

function App() {
  return (
    <ThemeProvider>
      <YodaTextProvider>
        <Dashboard />
      </YodaTextProvider>
    </ThemeProvider>
  );
}

export default App;
