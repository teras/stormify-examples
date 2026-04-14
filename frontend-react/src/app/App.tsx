import { CssBaseline, ThemeProvider } from "@mui/material";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { ApiBaseUrlProvider } from "../contexts/ApiBaseUrlContext";
import { AppLayout } from "../layout/AppLayout";
import { DashboardPage } from "../pages/DashboardPage";
import { CategoriesPage } from "../pages/master/CategoriesPage";
import { CustomersPage } from "../pages/master/CustomersPage";
import { ProductsPage } from "../pages/master/ProductsPage";
import { SuppliersPage } from "../pages/master/SuppliersPage";
import { WarehousesPage } from "../pages/master/WarehousesPage";
import { StockPage } from "../pages/StockPage";
import { PurchaseOrdersPage } from "../pages/transaction/PurchaseOrdersPage";
import { SalesOrdersPage } from "../pages/transaction/SalesOrdersPage";
import { ShipmentsPage } from "../pages/transaction/ShipmentsPage";
import { appTheme } from "../theme/theme";

export function App() {
  const [queryClient] = useState(() => new QueryClient());

  return (
    <ThemeProvider theme={appTheme}>
      <CssBaseline />
      <QueryClientProvider client={queryClient}>
        <ApiBaseUrlProvider>
          <BrowserRouter>
            <AppLayout>
              <Routes>
                <Route path="/" element={<DashboardPage />} />
                <Route path="/categories" element={<CategoriesPage />} />
                <Route path="/suppliers" element={<SuppliersPage />} />
                <Route path="/customers" element={<CustomersPage />} />
                <Route path="/warehouses" element={<WarehousesPage />} />
                <Route path="/products" element={<ProductsPage />} />
                <Route path="/stock" element={<StockPage />} />
                <Route path="/purchase-orders" element={<PurchaseOrdersPage />} />
                <Route path="/sales-orders" element={<SalesOrdersPage />} />
                <Route path="/shipments" element={<ShipmentsPage />} />
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </AppLayout>
          </BrowserRouter>
        </ApiBaseUrlProvider>
      </QueryClientProvider>
    </ThemeProvider>
  );
}
