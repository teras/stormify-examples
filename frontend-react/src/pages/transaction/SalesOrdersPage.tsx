import AddRounded from "@mui/icons-material/AddRounded";
import TaskAltRounded from "@mui/icons-material/TaskAltRounded";
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import type { ColDef, ICellRendererParams, ValueFormatterParams } from "ag-grid-community";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useCallback, useState } from "react";
import { resources } from "../../api/resources";
import { OrderItemsEditor, type OrderLineForm } from "../../components/OrderItemsEditor";
import { PageCard } from "../../components/PageCard";
import { EnumFilter } from "../../components/EnumFilter";
import { numberFilterParams } from "../../components/SearchDataGrid";
import { RefLink } from "../../components/RefLink";
import { SearchDataGrid } from "../../components/SearchDataGrid";
import { useApiBaseUrl } from "../../contexts/ApiBaseUrlContext";
import { useReferenceData } from "../../features/referenceData";
import type { PageSpec } from "../../types/common";
import type { SalesOrderDetails, SalesOrderInput, SalesOrderListItem } from "../../types/domain";

const columns: ColDef<SalesOrderListItem>[] = [
  { field: "orderNumber", headerName: "Order", flex: 1 },
  {
    field: "customerName",
    headerName: "Customer",
    flex: 1.2,
    cellRenderer: (p: ICellRendererParams<SalesOrderListItem>) => p.data ? <RefLink type="customer" id={p.data.customerId} label={p.data.customerName} /> : null,
  },
  {
    field: "warehouseName",
    headerName: "Warehouse",
    flex: 1.1,
    cellRenderer: (p: ICellRendererParams<SalesOrderListItem>) => p.data ? <RefLink type="warehouse" id={p.data.warehouseId} label={p.data.warehouseName} /> : null,
  },
  {
    field: "status", headerName: "Status", flex: 0.8,
    filter: EnumFilter, filterParams: { options: ["DRAFT", "CONFIRMED", "SHIPPED", "CANCELLED"] },
  },
  { field: "orderedAt", headerName: "Ordered", flex: 1 },
  { field: "confirmedAt", headerName: "Confirmed", flex: 1 },
  { field: "totalAmount", headerName: "Total", flex: 0.8, filter: "agNumberColumnFilter", filterParams: numberFilterParams, valueFormatter: (p: ValueFormatterParams) => Number(p.value).toFixed(2) },
];

interface SalesOrderFormState {
  customerId: number;
  warehouseId: number;
  notes: string;
  items: OrderLineForm[];
}

function emptyForm(): SalesOrderFormState {
  return { customerId: 0, warehouseId: 0, notes: "", items: [] };
}

function toRequest(form: SalesOrderFormState): SalesOrderInput {
  return {
    customerId: form.customerId,
    warehouseId: form.warehouseId,
    notes: form.notes,
    items: form.items.map((item) => ({
      productId: item.productId,
      quantity: item.quantity,
      unitPrice: item.unitValue,
    })),
  };
}

function fromDetails(details: SalesOrderDetails): SalesOrderFormState {
  return {
    customerId: details.customer?.id ?? 0,
    warehouseId: details.warehouse?.id ?? 0,
    notes: details.notes,
    items: details.items.map((item) => ({
      productId: item.productId ?? 0,
      quantity: item.quantity,
      unitValue: item.unitPrice,
    })),
  };
}

export function SalesOrdersPage() {
  const { baseUrl } = useApiBaseUrl();
  const refs = useReferenceData();
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [refreshToken, setRefreshToken] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<SalesOrderFormState>(emptyForm());



  const detailsQuery = useQuery({
    queryKey: ["sales-order-details", baseUrl, selectedId],
    queryFn: () => resources.salesOrders.get(baseUrl, selectedId!),
    enabled: selectedId != null,
  });

  const saveMutation = useMutation({
    mutationFn: async (value: SalesOrderFormState) =>
      editingId == null
        ? resources.salesOrders.create(baseUrl, toRequest(value))
        : resources.salesOrders.update(baseUrl, editingId, toRequest(value)),
    onSuccess: (details) => {
      setDialogOpen(false);
      setEditingId(details.id);
      setSelectedId(details.id);
      setRefreshToken((t) => t + 1);
      void queryClient.invalidateQueries({ queryKey: ["sales-order-details"] });
    },
  });

  const confirmMutation = useMutation({
    mutationFn: (id: number) => resources.salesOrders.confirm(baseUrl, id),
    onSuccess: (details) => {
      setSelectedId(details.id);
      setRefreshToken((t) => t + 1);
      void queryClient.invalidateQueries({ queryKey: ["sales-order-details"] });
    },
  });

  const fetchPage = useCallback(
    (spec: PageSpec) => resources.salesOrders.search(baseUrl, spec),
    [baseUrl],
  );

  const openCreate = () => {
    setEditingId(null);
    setForm({
      customerId: refs.customers[0]?.id ?? 0,
      warehouseId: refs.warehouses[0]?.id ?? 0,
      notes: "",
      items: refs.products[0] ? [{ productId: refs.products[0].id, quantity: 1, unitValue: 0 }] : [],
    });
    setDialogOpen(true);
  };

  const openEdit = async (id: number) => {
    const details = await resources.salesOrders.get(baseUrl, id);
    setEditingId(id);
    setForm(fromDetails(details));
    setDialogOpen(true);
  };


  return (
    <Stack spacing={3}>
      <PageCard
        title="Sales Orders"
        subtitle="Outbound commercial flow with stock reservation on confirmation."
        actions={
          <Button startIcon={<AddRounded />} variant="contained" onClick={openCreate}>
            New Sales Order
          </Button>
        }
      >
        <Stack spacing={2.5}>
          <SearchDataGrid<SalesOrderListItem>
            columns={columns}
            fetchPage={fetchPage}
            refreshToken={refreshToken}
            exportFileName="sales-orders"
            exportUrl={`${baseUrl}/api/sales-orders/export`}
            onRowClick={(row) => setSelectedId(row.id)}
          />
        </Stack>
      </PageCard>

      {detailsQuery.data && (
        <PageCard
          title={`Order ${detailsQuery.data.orderNumber}`}
          subtitle={`${detailsQuery.data.customer?.label ?? "Unknown customer"} · ${detailsQuery.data.status}`}
          actions={
            <Stack direction="row" spacing={1}>
              <Button onClick={() => void openEdit(detailsQuery.data!.id)}>Edit</Button>
              <Button
                startIcon={<TaskAltRounded />}
                variant="contained"
                disabled={detailsQuery.data.status !== "DRAFT" || confirmMutation.isPending}
                onClick={() => confirmMutation.mutate(detailsQuery.data!.id)}
              >
                Confirm Order
              </Button>
            </Stack>
          }
        >
          <Stack spacing={2}>
            {confirmMutation.error instanceof Error && <Alert severity="error">{confirmMutation.error.message}</Alert>}
            <Paper variant="outlined" sx={{ p: 2 }}>
              <Typography variant="subtitle2" color="text.secondary">
                Notes
              </Typography>
              <Typography>{detailsQuery.data.notes || "No notes"}</Typography>
            </Paper>
            <MiniItems
              rows={detailsQuery.data.items.map((item) => ({
                key: item.id,
                primary: `${item.productSku ?? "N/A"} · ${item.productName ?? "Unknown"}`,
                secondary: `Qty ${item.quantity} · Price ${item.unitPrice.toFixed(2)} · Total ${item.lineTotal.toFixed(2)}`,
              }))}
            />
          </Stack>
        </PageCard>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="lg">
        <DialogTitle>{editingId == null ? "Create Sales Order" : "Edit Sales Order"}</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={2.5} sx={{ pt: 1 }}>
            <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
              <FormControl fullWidth>
                <InputLabel>Customer</InputLabel>
                <Select
                  label="Customer"
                  value={form.customerId || ""}
                  onChange={(event) => setForm((prev) => ({ ...prev, customerId: Number(event.target.value) }))}
                >
                  {refs.customers.map((item) => (
                    <MenuItem key={item.id} value={item.id}>
                      {item.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl fullWidth>
                <InputLabel>Warehouse</InputLabel>
                <Select
                  label="Warehouse"
                  value={form.warehouseId || ""}
                  onChange={(event) => setForm((prev) => ({ ...prev, warehouseId: Number(event.target.value) }))}
                >
                  {refs.warehouses.map((item) => (
                    <MenuItem key={item.id} value={item.id}>
                      {item.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Stack>
            <TextField
              label="Notes"
              multiline
              minRows={3}
              value={form.notes}
              onChange={(event) => setForm((prev) => ({ ...prev, notes: event.target.value }))}
            />
            <Box>
              <Typography variant="h6" sx={{ mb: 1.5 }}>
                Line Items
              </Typography>
              <OrderItemsEditor
                lines={form.items}
                products={refs.products.map((item) => ({ id: item.id, label: `${item.sku} · ${item.name}` }))}
                unitLabel="Unit Price"
                onChange={(items) => setForm((prev) => ({ ...prev, items }))}
              />
            </Box>
            {saveMutation.error instanceof Error && <Alert severity="error">{saveMutation.error.message}</Alert>}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button onClick={() => saveMutation.mutate(form)} variant="contained" disabled={saveMutation.isPending}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}

function MiniItems({ rows }: { rows: Array<{ key: number; primary: string; secondary: string }> }) {
  return (
    <Stack spacing={1.25}>
      {rows.map((row) => (
        <Paper key={row.key} variant="outlined" sx={{ p: 1.5 }}>
          <Typography fontWeight={700}>{row.primary}</Typography>
          <Typography variant="body2" color="text.secondary">
            {row.secondary}
          </Typography>
        </Paper>
      ))}
    </Stack>
  );
}
