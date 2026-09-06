import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#0A2540' },
    secondary: { main: '#02A896' },
    background: { default: '#F4F7F9', paper: '#FFFFFF' },
    error: { main: '#C62828' },
    warning: { main: '#ED6C02' },
    success: { main: '#1E7A46' },
    info: { main: '#0A6572' },
  },
  typography: {
    fontFamily: 'Inter, system-ui, Segoe UI, sans-serif',
    h4: { fontWeight: 800 },
    h5: { fontWeight: 800 },
    h6: { fontWeight: 700 },
    button: { textTransform: 'none', fontWeight: 700 },
  },
  shape: { borderRadius: 12 },
  components: {
    MuiButton: { defaultProps: { disableElevation: true }, styleOverrides: { root: { minHeight: 44 } } },
    MuiTextField: { defaultProps: { size: 'small', fullWidth: true } },
    MuiCard: { styleOverrides: { root: { border: '1px solid #E2E8F0', boxShadow: '0 4px 16px rgba(10,37,64,.06)' } } },
    MuiChip: { styleOverrides: { root: { fontWeight: 700 } } },
  },
});
