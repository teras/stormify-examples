import { Box, Paper, Stack, Typography } from "@mui/material";
import { useQueries } from "@tanstack/react-query";
import { resources } from "../api/resources";
import { PageCard } from "../components/PageCard";
import { useApiBaseUrl } from "../contexts/ApiBaseUrlContext";
import type { PageSpec } from "../types/common";

const firstPage: PageSpec = { page: 0, pageSize: 8, filters: {}, sorts: {}, caseSensitive: {} };

function MetricCard({ label, value }: { label: string; value: string | number }) {
  return (
    <Paper sx={{ p: 2.5, height: "100%" }}>
      <Typography variant="overline" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="h4">{value}</Typography>
    </Paper>
  );
}

export function DashboardPage() {
  const { baseUrl } = useApiBaseUrl();
  const [products, customers, purchases, sales, lowStock] = useQueries({
    queries: [
      { queryKey: ["dash-products", baseUrl], queryFn: () => resources.products.search(baseUrl, firstPage) },
      { queryKey: ["dash-customers", baseUrl], queryFn: () => resources.customers.search(baseUrl, firstPage) },
      { queryKey: ["dash-purchases", baseUrl], queryFn: () => resources.purchaseOrders.search(baseUrl, firstPage) },
      { queryKey: ["dash-sales", baseUrl], queryFn: () => resources.salesOrders.search(baseUrl, firstPage) },
      {
        queryKey: ["dash-low-stock", baseUrl],
        queryFn: () =>
          resources.stock.search(baseUrl, {
            ...firstPage,
            filters: { belowReorder: "true" },
          }),
      },
    ],
  });

  return (
    <Stack spacing={3}>
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: {
            xs: "1fr",
            md: "repeat(2, minmax(0, 1fr))",
            xl: "repeat(4, minmax(0, 1fr))",
          },
          gap: 2.5,
        }}
      >
        <Box>
          <MetricCard label="Products" value={products.data?.totalItems ?? "…"} />
        </Box>
        <Box>
          <MetricCard label="Customers" value={customers.data?.totalItems ?? "…"} />
        </Box>
        <Box>
          <MetricCard label="Purchase Orders" value={purchases.data?.totalItems ?? "…"} />
        </Box>
        <Box>
          <MetricCard label="Sales Orders" value={sales.data?.totalItems ?? "…"} />
        </Box>
      </Box>

      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: {
            xs: "1fr",
            lg: "repeat(2, minmax(0, 1fr))",
          },
          gap: 2.5,
        }}
      >
        <Box>
          <PageCard title="Recent Purchase Orders">
            <Stack spacing={1.25}>
              {purchases.data?.items.map((item) => (
                <Paper key={item.id} variant="outlined" sx={{ p: 1.5 }}>
                  <Typography fontWeight={700}>{item.orderNumber}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {item.supplierName} · {item.status} · {item.totalAmount.toFixed(2)}
                  </Typography>
                </Paper>
              ))}
            </Stack>
          </PageCard>
        </Box>
        <Box>
          <PageCard title="Low Stock">
            <Stack spacing={1.25}>
              {lowStock.data?.items.map((item) => (
                <Paper key={item.id} variant="outlined" sx={{ p: 1.5 }}>
                  <Typography fontWeight={700}>{item.productName}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {item.warehouseName} · available {item.availableQuantity} / reorder {item.reorderLevel}
                  </Typography>
                </Paper>
              ))}
            </Stack>
          </PageCard>
        </Box>
      </Box>
    </Stack>
  );
}
