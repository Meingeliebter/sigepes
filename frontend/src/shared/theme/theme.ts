import { createTheme, alpha } from '@mui/material/styles';

/**
 * Paleta institucional SIGEPES — "vidrio sobre la carta náutica".
 *
 * La interfaz es un panel esmerilado puesto sobre una carta batimétrica (isobatas): cada
 * permiso, veda o faena se lee "sobre el agua" que regula. El vidrio claro mantiene la lectura
 * a pleno sol en la costa; el azul profundo ahumado queda para las superficies de autoridad
 * (barra superior, pie) y el turquesa es el único acento de acción.
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
  sea: {
    shallow: '#EEF7F7',
    lagoon: '#D3EBEE',
    reef: '#B5DCE1',
  },
  neutral: {
    900: '#0F2433',
    700: '#33495A',
    500: '#4E6474',
    300: '#A9BCC7',
    200: '#D3DFE5',
    100: '#E9F0F3',
    50: '#F4F8F9',
  },
};

/**
 * Recetas de vidrio. `backdrop-filter` desenfoca la carta que hay detrás; si el navegador no lo
 * soporta (o el usuario pide menos transparencia) el fondo se vuelve casi opaco para no perder
 * contraste.
 */
export const glass = {
  pane: {
    backgroundColor: 'rgba(255,255,255,0.64)',
    backdropFilter: 'blur(20px) saturate(170%)',
    WebkitBackdropFilter: 'blur(20px) saturate(170%)',
    border: '1px solid rgba(255,255,255,0.75)',
    boxShadow:
      'inset 0 1px 0 rgba(255,255,255,0.9), 0 1px 2px rgba(11,59,92,0.06), 0 12px 32px -8px rgba(11,59,92,0.18)',
  },
  paneStrong: {
    backgroundColor: 'rgba(255,255,255,0.82)',
    backdropFilter: 'blur(24px) saturate(180%)',
    WebkitBackdropFilter: 'blur(24px) saturate(180%)',
    border: '1px solid rgba(255,255,255,0.85)',
    boxShadow:
      'inset 0 1px 0 rgba(255,255,255,1), 0 2px 4px rgba(11,59,92,0.06), 0 24px 48px -12px rgba(7,30,48,0.28)',
  },
  smoked: {
    backgroundColor: 'rgba(7,30,48,0.74)',
    backdropFilter: 'blur(18px) saturate(160%)',
    WebkitBackdropFilter: 'blur(18px) saturate(160%)',
    borderBottom: '1px solid rgba(255,255,255,0.12)',
    boxShadow: 'inset 0 -1px 0 rgba(255,255,255,0.04), 0 8px 24px -12px rgba(7,30,48,0.5)',
  },
} as const;

const RADIUS = 14;

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
    background: { default: palette.sea.shallow, paper: 'rgba(255,255,255,0.88)' },
    text: { primary: palette.neutral[900], secondary: palette.neutral[500] },
    divider: alpha(palette.navy.dark, 0.12),
    error: { main: '#B3261E' },
    warning: { main: '#A85A00' },
    success: { main: '#1B6B3F' },
    info: { main: '#0B6E86' },
  },
  typography: {
    fontFamily: '"Public Sans", "Segoe UI", Roboto, system-ui, -apple-system, sans-serif',
    h1: { fontWeight: 800, letterSpacing: '-0.025em' },
    h2: { fontWeight: 800, letterSpacing: '-0.025em' },
    h3: { fontWeight: 800, letterSpacing: '-0.02em' },
    h4: { fontWeight: 800, letterSpacing: '-0.015em' },
    h5: { fontWeight: 700, letterSpacing: '-0.01em' },
    h6: { fontWeight: 700 },
    subtitle1: { fontWeight: 600 },
    subtitle2: { fontWeight: 600 },
    button: { textTransform: 'none', fontWeight: 650, letterSpacing: 0.1 },
    overline: { fontWeight: 700, letterSpacing: 1.2 },
  },
  shape: { borderRadius: 10 },
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
          borderRadius: 8,
          fontWeight: 600,
          transition: 'top .15s ease-in-out',
        },
        '.skip-link:focus': { top: 8 },
      },
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: {
          minHeight: 44,
          borderRadius: 999,
          paddingInline: 22,
          transition: 'background-color .2s, box-shadow .2s, transform .2s cubic-bezier(.16,1,.3,1)',
          '&:active': { transform: 'translateY(1px)' },
        },
        sizeLarge: { minHeight: 52, fontSize: '1rem', paddingInline: 28 },
        sizeSmall: { minHeight: 34, paddingInline: 14 },
        containedPrimary: {
          boxShadow: `inset 0 1px 0 ${alpha('#fff', 0.18)}, 0 6px 16px -6px ${alpha(palette.navy.dark, 0.55)}`,
        },
        containedSecondary: {
          boxShadow: `inset 0 1px 0 ${alpha('#fff', 0.22)}, 0 6px 16px -6px ${alpha(palette.teal.dark, 0.6)}`,
        },
        outlined: {
          borderWidth: 1.5,
          backgroundColor: alpha('#fff', 0.45),
          backdropFilter: 'blur(8px)',
          '&:hover': { borderWidth: 1.5, backgroundColor: alpha('#fff', 0.7) },
        },
        text: { '&:hover': { backgroundColor: alpha(palette.navy.dark, 0.06) } },
      },
    },
    MuiIconButton: {
      styleOverrides: { root: { transition: 'background-color .2s' } },
    },
    MuiTextField: {
      defaultProps: { fullWidth: true },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          background: 'rgba(255,255,255,0.78)',
          transition: 'background-color .2s, box-shadow .2s',
          '& .MuiOutlinedInput-notchedOutline': { borderColor: alpha(palette.navy.dark, 0.22) },
          '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: palette.navy.light },
          '&.Mui-focused': {
            background: '#FFFFFF',
            boxShadow: `0 0 0 4px ${alpha(palette.teal.main, 0.14)}`,
          },
          '&.Mui-focused .MuiOutlinedInput-notchedOutline': { borderColor: palette.teal.main, borderWidth: 2 },
          '&.Mui-disabled': { background: 'rgba(255,255,255,0.4)' },
        },
      },
    },
    MuiInputLabel: { styleOverrides: { root: { fontWeight: 600 } } },
    MuiFormHelperText: { styleOverrides: { root: { marginLeft: 2 } } },
    MuiCard: {
      defaultProps: { variant: 'outlined' },
      styleOverrides: {
        root: { ...glass.pane, borderRadius: RADIUS },
      },
    },
    MuiCardActionArea: {
      styleOverrides: {
        focusHighlight: { backgroundColor: palette.teal.main },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: { backgroundImage: 'none' },
        outlined: { ...glass.pane, borderRadius: RADIUS },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: { ...glass.smoked, backgroundImage: 'none', color: '#fff' },
      },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: { ...glass.paneStrong, borderRadius: 0, border: 'none' },
      },
    },
    MuiDialog: {
      styleOverrides: {
        paper: { ...glass.paneStrong, borderRadius: 18 },
      },
    },
    MuiBackdrop: {
      styleOverrides: {
        root: {
          '&:not(.MuiBackdrop-invisible)': {
            backgroundColor: alpha(palette.navy.darkest, 0.35),
            backdropFilter: 'blur(4px)',
          },
        },
      },
    },
    MuiPopover: { styleOverrides: { paper: { ...glass.paneStrong, borderRadius: 12 } } },
    MuiMenu: { styleOverrides: { paper: { ...glass.paneStrong, borderRadius: 12 } } },
    MuiAutocomplete: { styleOverrides: { paper: { ...glass.paneStrong, borderRadius: 12 } } },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: 999,
          color: palette.neutral[700],
          transition: 'background-color .2s, color .2s',
          '& .MuiListItemIcon-root': { color: palette.neutral[500], transition: 'color .2s' },
          '&:hover': { backgroundColor: alpha(palette.navy.dark, 0.06) },
          '&.Mui-selected': {
            backgroundColor: palette.navy.dark,
            color: '#fff',
            boxShadow: `inset 0 1px 0 ${alpha('#fff', 0.16)}, 0 6px 14px -6px ${alpha(palette.navy.dark, 0.6)}`,
            '& .MuiListItemIcon-root': { color: palette.teal.light },
            '& .MuiListItemText-primary': { fontWeight: 650 },
            '&:hover': { backgroundColor: palette.navy.main },
          },
        },
      },
    },
    MuiListSubheader: {
      styleOverrides: {
        root: {
          fontWeight: 700,
          fontSize: '.72rem',
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
        root: { fontWeight: 650, borderRadius: 999 },
        outlined: { borderWidth: 1.5, backgroundColor: alpha('#fff', 0.5) },
      },
    },
    MuiAlert: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          border: '1px solid transparent',
          backdropFilter: 'blur(12px) saturate(160%)',
          WebkitBackdropFilter: 'blur(12px) saturate(160%)',
        },
        standardInfo: { backgroundColor: alpha('#E3F2F5', 0.82), borderColor: alpha(palette.teal.dark, 0.25) },
        standardSuccess: { backgroundColor: alpha('#E6F3EA', 0.85), borderColor: alpha('#1B6B3F', 0.25) },
        standardWarning: { backgroundColor: alpha('#FFF3E0', 0.88), borderColor: alpha('#A85A00', 0.28) },
        standardError: { backgroundColor: alpha('#FDECEA', 0.88), borderColor: alpha('#B3261E', 0.28) },
      },
    },
    MuiTableContainer: {
      styleOverrides: { root: { borderRadius: RADIUS } },
    },
    MuiTableHead: {
      styleOverrides: {
        root: {
          backgroundColor: alpha(palette.navy.dark, 0.05),
          '& .MuiTableCell-root': {
            fontWeight: 700,
            fontSize: '.75rem',
            letterSpacing: 0.4,
            textTransform: 'uppercase',
            color: palette.neutral[700],
            borderBottom: `1px solid ${alpha(palette.navy.dark, 0.14)}`,
          },
        },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: {
          transition: 'background-color .15s',
          '&:hover': { backgroundColor: alpha('#fff', 0.55) },
          '&:last-of-type .MuiTableCell-root': { borderBottom: 'none' },
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderBottomColor: alpha(palette.navy.dark, 0.09),
          fontVariantNumeric: 'tabular-nums',
          // En pantallas angostas la tabla se desplaza en horizontal en vez de partir cada celda palabra por palabra.
          '@media (max-width: 899.95px)': { whiteSpace: 'nowrap' },
        },
      },
    },
    MuiDialogTitle: { styleOverrides: { root: { fontWeight: 800 } } },
    MuiTabs: {
      styleOverrides: {
        indicator: { height: 3, borderRadius: 3, backgroundColor: palette.teal.main },
      },
    },
    MuiTab: { styleOverrides: { root: { textTransform: 'none', fontWeight: 650 } } },
    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          backgroundColor: alpha(palette.navy.darkest, 0.88),
          backdropFilter: 'blur(8px)',
          fontSize: '.75rem',
          borderRadius: 8,
        },
      },
    },
    MuiBottomNavigation: {
      styleOverrides: {
        root: {
          ...glass.paneStrong,
          height: 64,
          paddingBottom: 'env(safe-area-inset-bottom)',
          borderRadius: '18px 18px 0 0',
          borderBottom: 'none',
        },
      },
    },
    MuiBottomNavigationAction: {
      styleOverrides: {
        root: {
          color: palette.neutral[500],
          minWidth: 0,
          '&.Mui-selected': { color: palette.navy.dark },
        },
        label: {
          fontWeight: 600,
          fontSize: '.7rem',
          whiteSpace: 'nowrap',
          '&.Mui-selected': { fontSize: '.72rem' },
        },
      },
    },
    MuiLinearProgress: {
      styleOverrides: {
        root: { borderRadius: 4, backgroundColor: alpha(palette.navy.dark, 0.1) },
      },
    },
    MuiLink: { defaultProps: { underline: 'hover' } },
  },
});

/** Tokens crudos, por si un componente necesita el color exacto fuera del theme de MUI. */
export const institutionalPalette = palette;
