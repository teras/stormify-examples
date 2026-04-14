import MenuRounded from "@mui/icons-material/MenuRounded";
import {
  AppBar,
  Box,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  ListSubheader,
  Stack,
  TextField,
  Toolbar,
  Typography,
} from "@mui/material";
import { useState, type ReactNode } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { useApiBaseUrl } from "../contexts/ApiBaseUrlContext";
import { navigationGroups, navigationItems } from "./navigation";

const drawerWidth = 280;

export function AppLayout({ children }: { children: ReactNode }) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const { baseUrl, setBaseUrl } = useApiBaseUrl();
  const location = useLocation();

  const drawer = (
    <Box sx={{ height: "100%", display: "flex", flexDirection: "column" }}>
      <Box sx={{ px: 3, py: 3 }}>
        <Typography variant="overline" color="primary.main">
          rest-server demo
        </Typography>
        <Typography variant="h5">Warehouse Console</Typography>
        <Typography variant="body2" color="text.secondary">
          Shared React client for both backend implementations.
        </Typography>
      </Box>
      <Divider />
      <List sx={{ px: 1.5, py: 1, flexGrow: 1 }} component="nav">
        {navigationGroups.map((group, groupIdx) => (
          <Box key={group.heading ?? `group-${groupIdx}`} sx={{ mb: 1.5 }}>
            {group.heading && (
              <ListSubheader
                disableSticky
                sx={{
                  bgcolor: "transparent",
                  px: 1.5,
                  lineHeight: 2,
                  fontSize: "0.7rem",
                  textTransform: "uppercase",
                  letterSpacing: 0.8,
                  color: "text.secondary",
                }}
              >
                {group.heading}
              </ListSubheader>
            )}
            {group.items.map((item) => {
              const Icon = item.icon;
              const selected = location.pathname === item.path;
              return (
                <ListItemButton
                  key={item.path}
                  component={NavLink}
                  to={item.path}
                  selected={selected}
                  onClick={() => setMobileOpen(false)}
                  sx={{ borderRadius: 2, mb: 0.25 }}
                >
                  <ListItemIcon>
                    <Icon color={selected ? "primary" : "inherit"} />
                  </ListItemIcon>
                  <ListItemText primary={item.label} />
                </ListItemButton>
              );
            })}
          </Box>
        ))}
      </List>
      <Divider />
      <Box sx={{ p: 2 }}>
        <TextField
          fullWidth
          label="Backend URL"
          value={baseUrl}
          onChange={(event) => setBaseUrl(event.target.value)}
          helperText="Point this UI to kotlin-rest or java-rest."
        />
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: "flex", minHeight: "100vh" }}>
      <AppBar
        position="fixed"
        color="inherit"
        elevation={0}
        sx={{
          width: { md: `calc(100% - ${drawerWidth}px)` },
          ml: { md: `${drawerWidth}px` },
          borderBottom: "1px solid rgba(22, 32, 43, 0.08)",
          backdropFilter: "blur(14px)",
          backgroundColor: "rgba(255,255,255,0.82)",
        }}
      >
        <Toolbar sx={{ minHeight: 76 }}>
          <IconButton
            edge="start"
            onClick={() => setMobileOpen((value) => !value)}
            sx={{ mr: 2, display: { md: "none" } }}
          >
            <MenuRounded />
          </IconButton>
          <Stack spacing={0.25}>
            <Typography variant="h6">
              {navigationItems.find((item) => item.path === location.pathname)?.label ?? "Warehouse Console"}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Admin-style demo for Stormify-backed REST services.
            </Typography>
          </Stack>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { md: drawerWidth }, flexShrink: { md: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: "block", md: "none" },
            "& .MuiDrawer-paper": { width: drawerWidth },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: "none", md: "block" },
            "& .MuiDrawer-paper": {
              width: drawerWidth,
              boxSizing: "border-box",
              border: 0,
              background: "linear-gradient(180deg, #f7fbff 0%, #fff6ea 100%)",
            },
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          px: { xs: 2, md: 4 },
          py: { xs: 2, md: 3 },
          mt: "76px",
        }}
      >
        {children}
      </Box>
    </Box>
  );
}
