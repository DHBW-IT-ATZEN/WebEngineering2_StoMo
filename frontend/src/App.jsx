import { ThemeProvider } from './theme/ThemeProvider';
import { YodaTextProvider } from './theme/YodaTextProvider';
import { AuthProvider } from './auth/AuthProvider';
import AppShell from './components/AppShell';

function App() {
  return (
    <ThemeProvider>
      <YodaTextProvider>
        <AuthProvider>
          <AppShell />
        </AuthProvider>
      </YodaTextProvider>
    </ThemeProvider>
  );
}

export default App;
