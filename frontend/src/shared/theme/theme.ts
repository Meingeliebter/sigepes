import { createTheme, alpha } from '@mui/material/styles';

/**
 * Paleta institucional SIGEPES.
 *
 * Azul profundo (autoridad, confianza) + turquesa/verde agua (mar, sostenibilidad) sobre fondos
 * claros y neutros. Deliberadamente sobria: nada de degradados, vidrio esmerilado ni colores muy
 * saturados — el lenguaje visual de un portal de gobierno, no el de una app de consumo.
 */
const palette = {
  navy: {
    darkest: '#071E30',
    dark: '#0B3B5C',
    main: '#0F4C75',
    light: '#3D6E92',
    tint: '#E7EEF3',
  },
  teal: {
    dark: '#0A5F67',
    main: '#0E7C86',
    light: '#4FA8AE',
    tint: '#E3F2F1',
  },
  neutral: {
    900: '#152935',
    700: '#3C4C57',
    500: '#5C6C77',
    300: '#B9C6CD',
    200: '#DDE5EA',
    100: '#EEF2F5',
    50: '#F6F8FA',
  },
};

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: palette.navy.dark,
      dark: palette.navy.darkest,
      light: palette.navy.light,
      contrastText: '#FFFFFF',
    },
    secondary: {
      main: palette.teal.main,
      dark: palette.teal.dark,
      light: palette.teal.light,
      contrastText: '#FFFFFF',
    },
    background: { default: palette.neutral[50], paper: '#FFFFFF' },
    text: { primary: palette.neutral[900], secondary: palette.neutral[500] },
    divider: palette.neutral[200],
    error: { main: '#B3261E' },
    warning: { main: '#A85A00' },
    success: { main: '#1B6B3F' },
    info: { main: '#0B6E86' },
  },
  typography: {
    fontFamily: '"Public Sans", "Segoe UI", Roboto, system-ui, -apple-system, sans-serif',
    h1: { fontWeight: 700, letterSpacing: -0.5 },
    h2: { fontWeight: 700, letterSpacing: -0.5 },
    h3: { fontWeight: 700 },
    h4: { fontWeight: 700 },
    h5: { fontWeight: 700 },
    h6: { fontWeight: 700 },
    subtitle1: { fontWeight: 600 },
    subtitle2: { fontWeight: 600 },
    button: { textTransform: 'none', fontWeight: 600 },
    overline: { fontWeight: 700, letterSpacing: 1.2 },
  },
  shape: { borderRadius: 8 },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        ':focus-visible': {
          outline: `3px solid ${palette.teal.main}`,
          outlineOffset: 2,
        },
        // Utilidad de accesibilidad: enlaces "saltar al contenido", visibles solo al enfocarse.
        '.skip-link': {
          position: 'absolute',
          left: 8,
          top: -48,
          zIndex: 2000,
          background: palette.navy.darkest,
          color: '#fff',
          padding: '10px 16px',
          borderRadius: 6,
          fontWeight: 600,
          transition: 'top .15s ease-in-out',
        },
        '.skip-link:focus': { top: 8 },
      },
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { minHeight: 44, borderRadius: 6, paddingInline: 20 },
        sizeLarge: { minHeight: 52, fontSize: '1rem' },
        outlined: { borderWidth: 1.5, '&:hover': { borderWidth: 1.5 } },
      },
    },
    MuiTextField: {
      defaultProps: { fullWidth: true },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 6,
          background: '#FFFFFF',
          '& .MuiOutlinedInput-notchedOutline': { borderColor: palette.neutral[300] },
          '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: palette.navy.light },
          '&.Mui-focused .MuiOutlinedInput-notchedOutline': { borderColor: palette.teal.main, borderWidth: 2 },
        },
      },
    },
    MuiInputLabel: { styleOverrides: { root: { fontWeight: 600 } } },
    MuiFormHelperText: { styleOverrides: { root: { marginLeft: 2 } } },
    MuiCard: {
      defaultProps: { variant: 'outlined' },
      styleOverrides: {
        root: {
          border: `1px solid ${palette.neutral[200]}`,
          borderRadius: 10,
          backgroundColor: '#FFFFFF',
          boxShadow: '0 1px 2px rgba(15,76,117,.06)',
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: { backgroundImage: 'none' },
        outlined: { borderColor: palette.neutral[200] },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          background: palette.navy.darkest,
          boxShadow: 'none',
          borderBottom: `1px solid ${alpha('#FFFFFF', 0.1)}`,
        },
      },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: { border: 'none', borderRight: `1px solid ${palette.neutral[200]}`, boxShadow: 'none' },
      },
    },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: 6,
          '&.Mui-selected': {
            backgroundColor: alpha(palette.navy.dark, 0.08),
            borderLeft: `3px solid ${palette.navy.dark}`,
            paddingLeft: 13,
            color: palette.navy.dark,
            '& .MuiListItemIcon-root': { color: palette.navy.dark },
            '&:hover': { backgroundColor: alpha(palette.navy.dark, 0.12) },
          },
        },
      },
    },
    MuiListSubheader: {
      styleOverrides: {
        root: {
          fontWeight: 700,
          fontSize: '.7rem',
          letterSpacing: 1,
          textTransform: 'uppercase',
          color: palette.neutral[500],
          lineHeight: '32px',
          background: 'transparent',
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: { fontWeight: 600, borderRadius: 6 },
        outlined: { borderWidth: 1.5 },
      },
    },
    MuiAlert: {
      styleOverrides: {
        root: { borderRadius: 8, border: '1px solid transparent' },
        standardInfo: { borderColor: alpha(palette.teal.dark, 0.3) },
      },
    },
    MuiTableHead: {
      styleOverrides: {
        root: {
          backgroundColor: palette.neutral[100],
          '& .MuiTableCell-root': {
            fontWeight: 700,
            fontSize: '.78rem',
            letterSpacing: 0.3,
            textTransform: 'uppercase',
            color: palette.neutral[700],
            borderBottom: `2px solid ${palette.neutral[200]}`,
          },
        },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: {
          '&:hover': { backgroundColor: alpha(palette.navy.main, 0.04) },
          '&:last-of-type .MuiTableCell-root': { borderBottom: 'none' },
        },
      },
    },
    MuiTableCell: { styleOverrides: { root: { borderBottomColor: palette.neutral[200] } } },
    MuiDialogTitle: { styleOverrides: { root: { fontWeight: 700 } } },
    MuiDialog: { styleOverrides: { paper: { borderRadius: 10 } } },
    MuiTooltip: { styleOverrides: { tooltip: { backgroundColor: palette.neutral[900], fontSize: '.75rem' } } },
    MuiLink: { defaultProps: { underline: 'hover' } },
  },
});

/** Tokens crudos, por si un componente necesita el color exacto fuera del theme de MUI. */
export const institutionalPalette = palette;
