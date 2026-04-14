import { Paper, Stack, Typography, type PaperProps } from "@mui/material";
import type { ReactNode } from "react";

interface PageCardProps extends PaperProps {
  title?: string;
  subtitle?: string;
  actions?: ReactNode;
  children: ReactNode;
}

export function PageCard({ title, subtitle, actions, children, ...paperProps }: PageCardProps) {
  return (
    <Paper sx={{ p: 3 }} {...paperProps}>
      {(title || actions) && (
        <Stack
          direction={{ xs: "column", md: "row" }}
          spacing={2}
          alignItems={{ xs: "flex-start", md: "center" }}
          justifyContent="space-between"
          sx={{ mb: 2.5 }}
        >
          <Stack spacing={0.5}>
            {title && <Typography variant="h5">{title}</Typography>}
            {subtitle && (
              <Typography color="text.secondary" variant="body2">
                {subtitle}
              </Typography>
            )}
          </Stack>
          {actions}
        </Stack>
      )}
      {children}
    </Paper>
  );
}
