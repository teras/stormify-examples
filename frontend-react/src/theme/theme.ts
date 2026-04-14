import { createTheme } from "@mui/material/styles";

export const appTheme = createTheme({
  palette: {
    mode: "light",
    primary: {
      main: "#184b73",
      dark: "#113452",
      light: "#537ea0",
    },
    secondary: {
      main: "#ca7b1f",
    },
    background: {
      default: "#eff4fa",
      paper: "#ffffff",
    },
    success: {
      main: "#2f7f51",
    },
    warning: {
      main: "#cb8c2f",
    },
    error: {
      main: "#b13a3a",
    },
  },
  shape: {
    borderRadius: 14,
  },
  typography: {
    fontFamily: "\"IBM Plex Sans\", \"Segoe UI\", sans-serif",
    h4: {
      fontWeight: 700,
      letterSpacing: "-0.02em",
    },
    h5: {
      fontWeight: 700,
    },
    h6: {
      fontWeight: 700,
    },
  },
  components: {
    MuiPaper: {
      styleOverrides: {
        root: {
          boxShadow: "0 16px 40px rgba(24, 45, 71, 0.08)",
        },
      },
    },
  },
});
