import AddRounded from "@mui/icons-material/AddRounded";
import LocalShippingRounded from "@mui/icons-material/LocalShippingRounded";
import {
  Alert,
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
import { PageCard } from "../../components/PageCard";
import { EnumFilter } from "../../components/EnumFilter";
import { RefLink } from "../../components/RefLink";
import { SearchDataGrid } from "../../components/SearchDataGrid";
import { useApiBaseUrl } from "../../contexts/ApiBaseUrlContext";
import { useReferenceData } from "../../features/referenceData";
import type { PageSpec } from "../../types/common";
import type { ShipmentDetails, ShipmentInput, ShipmentListItem } from "../../types/domain";

const columns: ColDef<ShipmentListItem>[] = [
  { field: "shipmentNumber", headerName: "Shipment", flex: 1 },
  {
    field: "salesOrderNumber",
    headerName: "Sales Order",
    flex: 1,
    cellRenderer: (p: ICellRendererParams<ShipmentListItem>) => p.data ? <RefLink type="salesOrder" id={p.data.salesOrderId} label={p.data.salesOrderNumber} /> : null,
  },
  {
    field: "warehouseName",
    headerName: "Warehouse",
    flex: 1,
    cellRenderer: (p: ICellRendererParams<ShipmentListItem>) => p.data ? <RefLink type="warehouse" id={p.data.warehouseId} label={p.data.warehouseName} /> : null,
  },
  { field: "carrier", headerName: "Carrier", flex: 1 },
  { field: "trackingCode", headerName: "Tracking", flex: 1.1 },
  {
    field: "status", headerName: "Status", flex: 0.8,
    filter: EnumFilter, filterParams: { options: ["PREPARING", "SHIPPED", "DELIVERED", "CANCELLED"] },
  },
  { field: "shippedAt", headerName: "Shipped At", flex: 1 },
];

interface ShipmentFormState {
  salesOrderId: number;
  warehouseId: number;
  carrier: string;
  trackingCode: string;
}

function emptyForm(): ShipmentFormState {
  return { salesOrderId: 0, warehouseId: 0, carrier: "", trackingCode: "" };
}

function toRequest(form: ShipmentFormState): ShipmentInput {
  return {
    salesOrderId: form.salesOrderId,
    warehouseId: form.warehouseId,
    carrier: form.carrier,
    trackingCode: form.trackingCode,
  };
}

function fromDetails(details: ShipmentDetails): ShipmentFormState {
  return {
    salesOrderId: details.salesOrder?.id ?? 0,
    warehouseId: details.warehouse?.id ?? 0,
    carrier: details.carrier,
    trackingCode: details.trackingCode,
  };
}

export function ShipmentsPage() {
  const { baseUrl } = useApiBaseUrl();
  const refs = useReferenceData();
  const queryClient = useQueryClient();
  const salesOrderOptionsQuery = useQuery({
    queryKey: ["shipment-sales-order-options", baseUrl],
    queryFn: () =>
      resources.salesOrders.search(baseUrl, {
        page: 0,
        pageSize: 200,
        filters: {},
        sorts: {},
        caseSensitive: {},
      }),
  });
  const salesOrders = salesOrderOptionsQuery.data?.items ?? [];
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [refreshToken, setRefreshToken] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<ShipmentFormState>(emptyForm());



  const detailsQuery = useQuery({
    queryKey: ["shipment-details", baseUrl, selectedId],
    queryFn: () => resources.shipments.get(baseUrl, selectedId!),
    enabled: selectedId != null,
  });

  const saveMutation = useMutation({
    mutationFn: async (value: ShipmentFormState) =>
      editingId == null
        ? resources.shipments.create(baseUrl, toRequest(value))
        : resources.shipments.update(baseUrl, editingId, {
            carrier: value.carrier,
            trackingCode: value.trackingCode,
          }),
    onSuccess: (details) => {
      setDialogOpen(false);
      setEditingId(details.id);
      setSelectedId(details.id);
      setRefreshToken((t) => t + 1);
      void queryClient.invalidateQueries({ queryKey: ["shipment-details"] });
    },
  });

  const shipMutation = useMutation({
    mutationFn: (id: number) => resources.shipments.ship(baseUrl, id),
    onSuccess: (details) => {
      setSelectedId(details.id);
      setRefreshToken((t) => t + 1);
      void queryClient.invalidateQueries({ queryKey: ["shipment-details"] });
    },
  });

  const fetchPage = useCallback(
    (spec: PageSpec) => resources.shipments.search(baseUrl, spec),
    [baseUrl],
  );

  const openCreate = () => {
    setEditingId(null);
    setForm({
      salesOrderId: 0,
      warehouseId: refs.warehouses[0]?.id ?? 0,
      carrier: "",
      trackingCode: "",
    });
    setDialogOpen(true);
  };

  const openEdit = async (id: number) => {
    const details = await resources.shipments.get(baseUrl, id);
    setEditingId(id);
    setForm(fromDetails(details));
    setDialogOpen(true);
  };


  return (
    <Stack spacing={3}>
      <PageCard
        title="Shipments"
        subtitle="Final outbound stage: tie confirmed sales orders to carrier execution."
        actions={
          <Button startIcon={<AddRounded />} variant="contained" onClick={openCreate}>
            New Shipment
          </Button>
        }
      >
        <Stack spacing={2.5}>
          <SearchDataGrid<ShipmentListItem>
            columns={columns}
            fetchPage={fetchPage}
            refreshToken={refreshToken}
            exportFileName="shipments"
            exportUrl={`${baseUrl}/api/shipments/export`}
            onRowClick={(row) => setSelectedId(row.id)}
          />
        </Stack>
      </PageCard>

      {detailsQuery.data && (
        <PageCard
          title={`Shipment ${detailsQuery.data.shipmentNumber}`}
          subtitle={`${detailsQuery.data.carrier} · ${detailsQuery.data.status}`}
          actions={
            <Stack direction="row" spacing={1}>
              <Button onClick={() => void openEdit(detailsQuery.data!.id)}>Edit</Button>
              <Button
                startIcon={<LocalShippingRounded />}
                variant="contained"
                disabled={detailsQuery.data.status !== "PREPARING" || shipMutation.isPending}
                onClick={() => shipMutation.mutate(detailsQuery.data!.id)}
              >
                Ship
              </Button>
            </Stack>
          }
        >
          <Stack spacing={2}>
            {shipMutation.error instanceof Error && <Alert severity="error">{shipMutation.error.message}</Alert>}
            <Paper variant="outlined" sx={{ p: 2 }}>
              <Typography variant="subtitle2" color="text.secondary">
                Sales Order / Warehouse
              </Typography>
              <Typography>{detailsQuery.data.salesOrder?.label ?? "No sales order"}</Typography>
              <Typography color="text.secondary">{detailsQuery.data.warehouse?.label ?? "No warehouse"}</Typography>
            </Paper>
            <Paper variant="outlined" sx={{ p: 2 }}>
              <Typography variant="subtitle2" color="text.secondary">
                Tracking
              </Typography>
              <Typography>{detailsQuery.data.trackingCode || "No tracking code"}</Typography>
              <Typography color="text.secondary">{detailsQuery.data.shippedAt || "Not shipped yet"}</Typography>
            </Paper>
          </Stack>
        </PageCard>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>{editingId == null ? "Create Shipment" : "Edit Shipment"}</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={2.5} sx={{ pt: 1 }}>
            {editingId == null && (
              <>
                <FormControl fullWidth>
                  <InputLabel>Sales Order</InputLabel>
                  <Select
                    label="Sales Order"
                    value={form.salesOrderId || ""}
                    onChange={(event) => setForm((prev) => ({ ...prev, salesOrderId: Number(event.target.value) }))}
                  >
                    {salesOrders.map((item) => (
                      <MenuItem key={item.id} value={item.id}>
                        {item.orderNumber}
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
              </>
            )}
            <TextField
              label="Carrier"
              value={form.carrier}
              onChange={(event) => setForm((prev) => ({ ...prev, carrier: event.target.value }))}
            />
            <TextField
              label="Tracking Code"
              value={form.trackingCode}
              onChange={(event) => setForm((prev) => ({ ...prev, trackingCode: event.target.value }))}
            />
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
