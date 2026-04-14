import AddRounded from "@mui/icons-material/AddRounded";
import MoveToInboxRounded from "@mui/icons-material/MoveToInboxRounded";
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
import type { PurchaseOrderDetails, PurchaseOrderInput, PurchaseOrderListItem } from "../../types/domain";

const columns: ColDef<PurchaseOrderListItem>[] = [
  { field: "orderNumber", headerName: "Order", flex: 1 },
  {
    field: "supplierName",
    headerName: "Supplier",
    flex: 1.2,
    cellRenderer: (p: ICellRendererParams<PurchaseOrderListItem>) => p.data ? <RefLink type="supplier" id={p.data.supplierId} label={p.data.supplierName} /> : null,
  },
  {
    field: "warehouseName",
    headerName: "Warehouse",
    flex: 1.1,
    cellRenderer: (p: ICellRendererParams<PurchaseOrderListItem>) => p.data ? <RefLink type="warehouse" id={p.data.warehouseId} label={p.data.warehouseName} /> : null,
  },
  {
    field: "status", headerName: "Status", flex: 0.8,
    filter: EnumFilter, filterParams: { options: ["DRAFT", "SUBMITTED", "RECEIVED", "CANCELLED"] },
  },
  { field: "orderedAt", headerName: "Ordered", flex: 1 },
  { field: "expectedAt", headerName: "Expected", flex: 1 },
  {
    field: "totalAmount",
    headerName: "Total",
    flex: 0.8,
    filter: "agNumberColumnFilter", filterParams: numberFilterParams,
    valueFormatter: (p: ValueFormatterParams) => Number(p.value).toFixed(2),
  },
];

interface PurchaseOrderFormState {
  supplierId: number;
  warehouseId: number;
  expectedAt: string;
  notes: string;
  items: OrderLineForm[];
}

function emptyForm(): PurchaseOrderFormState {
  return {
    supplierId: 0,
    warehouseId: 0,
    expectedAt: "",
    notes: "",
    items: [],
  };
}

function toRequest(form: PurchaseOrderFormState): PurchaseOrderInput {
  return {
    supplierId: form.supplierId,
    warehouseId: form.warehouseId,
    expectedAt: form.expectedAt,
    notes: form.notes,
    items: form.items.map((item) => ({
      productId: item.productId,
      quantity: item.quantity,
      unitCost: item.unitValue,
    })),
  };
}

function fromDetails(details: PurchaseOrderDetails): PurchaseOrderFormState {
  return {
    supplierId: details.supplier?.id ?? 0,
    warehouseId: details.warehouse?.id ?? 0,
    expectedAt: details.expectedAt,
    notes: details.notes,
    items: details.items.map((item) => ({
      productId: item.productId ?? 0,
      quantity: item.quantity,
      unitValue: item.unitCost,
    })),
  };
}

export function PurchaseOrdersPage() {
  const { baseUrl } = useApiBaseUrl();
  const refs = useReferenceData();
  const queryClient = useQueryClient();
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [refreshToken, setRefreshToken] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<PurchaseOrderFormState>(emptyForm());



  const detailsQuery = useQuery({
    queryKey: ["purchase-order-details", baseUrl, selectedId],
    queryFn: () => resources.purchaseOrders.get(baseUrl, selectedId!),
    enabled: selectedId != null,
  });

  const saveMutation = useMutation({
    mutationFn: async (value: PurchaseOrderFormState) =>
      editingId == null
        ? resources.purchaseOrders.create(baseUrl, toRequest(value))
        : resources.purchaseOrders.update(baseUrl, editingId, toRequest(value)),
    onSuccess: (details) => {
      setDialogOpen(false);
      setEditingId(details.id);
      setSelectedId(details.id);
      setRefreshToken((t) => t + 1);
      void queryClient.invalidateQueries({ queryKey: ["purchase-order-details"] });
    },
  });

  const receiveMutation = useMutation({
    mutationFn: (id: number) => resources.purchaseOrders.receive(baseUrl, id),
    onSuccess: (details) => {
      setSelectedId(details.id);
      setRefreshToken((t) => t + 1);
      void queryClient.invalidateQueries({ queryKey: ["purchase-order-details"] });
    },
  });

  const fetchPage = useCallback(
    (spec: PageSpec) => resources.purchaseOrders.search(baseUrl, spec),
    [baseUrl],
  );

  const openCreate = () => {
    setEditingId(null);
    setForm({
      supplierId: refs.suppliers[0]?.id ?? 0,
      warehouseId: refs.warehouses[0]?.id ?? 0,
      expectedAt: "",
      notes: "",
      items: refs.products[0] ? [{ productId: refs.products[0].id, quantity: 1, unitValue: 0 }] : [],
    });
    setDialogOpen(true);
  };

  const openEdit = async (id: number) => {
    const details = await resources.purchaseOrders.get(baseUrl, id);
    setEditingId(id);
    setForm(fromDetails(details));
    setDialogOpen(true);
  };


  return (
    <Stack spacing={3}>
      <PageCard
        title="Purchase Orders"
        subtitle="Inbound commercial flow: draft orders, line items and stock reception."
        actions={
          <Button startIcon={<AddRounded />} variant="contained" onClick={openCreate}>
            New Purchase Order
          </Button>
        }
      >
        <Stack spacing={2.5}>
          <SearchDataGrid<PurchaseOrderListItem>
            columns={columns}
            fetchPage={fetchPage}
            refreshToken={refreshToken}
            exportFileName="purchase-orders"
            exportUrl={`${baseUrl}/api/purchase-orders/export`}
            onRowClick={(row) => setSelectedId(row.id)}
          />
        </Stack>
      </PageCard>

      {detailsQuery.data && (
        <PageCard
          title={`Order ${detailsQuery.data.orderNumber}`}
          subtitle={`${detailsQuery.data.supplier?.label ?? "Unknown supplier"} · ${detailsQuery.data.status}`}
          actions={
            <Stack direction="row" spacing={1}>
              <Button onClick={() => void openEdit(detailsQuery.data!.id)}>Edit</Button>
              <Button
                startIcon={<MoveToInboxRounded />}
                variant="contained"
                disabled={detailsQuery.data.status !== "DRAFT" || receiveMutation.isPending}
                onClick={() => receiveMutation.mutate(detailsQuery.data!.id)}
              >
                Receive Order
              </Button>
            </Stack>
          }
        >
          <Stack spacing={2}>
            {receiveMutation.error instanceof Error && <Alert severity="error">{receiveMutation.error.message}</Alert>}
            <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
              <Paper variant="outlined" sx={{ p: 2, flex: 1 }}>
                <Typography variant="subtitle2" color="text.secondary">
                  Notes
                </Typography>
                <Typography>{detailsQuery.data.notes || "No notes"}</Typography>
              </Paper>
              <Paper variant="outlined" sx={{ p: 2, flex: 1 }}>
                <Typography variant="subtitle2" color="text.secondary">
                  Expected / Received
                </Typography>
                <Typography>{detailsQuery.data.expectedAt}</Typography>
                <Typography color="text.secondary">{detailsQuery.data.receivedAt || "Not received yet"}</Typography>
              </Paper>
            </Stack>
            <TableLikeItems
              title="Items"
              rows={detailsQuery.data.items.map((item) => ({
                key: item.id,
                primary: `${item.productSku ?? "N/A"} · ${item.productName ?? "Unknown"}`,
                secondary: `Qty ${item.quantity} · Cost ${item.unitCost.toFixed(2)} · Total ${item.lineTotal.toFixed(2)}`,
              }))}
            />
          </Stack>
        </PageCard>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="lg">
        <DialogTitle>{editingId == null ? "Create Purchase Order" : "Edit Purchase Order"}</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={2.5} sx={{ pt: 1 }}>
            <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
              <FormControl fullWidth>
                <InputLabel>Supplier</InputLabel>
                <Select
                  label="Supplier"
                  value={form.supplierId || ""}
                  onChange={(event) => setForm((prev) => ({ ...prev, supplierId: Number(event.target.value) }))}
                >
                  {refs.suppliers.map((item) => (
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
              type="date"
              label="Expected at"
              InputLabelProps={{ shrink: true }}
              value={form.expectedAt}
              onChange={(event) => setForm((prev) => ({ ...prev, expectedAt: event.target.value }))}
            />
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
                unitLabel="Unit Cost"
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

function TableLikeItems({
  title,
  rows,
}: {
  title: string;
  rows: Array<{ key: number; primary: string; secondary: string }>;
}) {
  return (
    <Box>
      <Typography variant="h6" sx={{ mb: 1.5 }}>
        {title}
      </Typography>
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
    </Box>
  );
}
