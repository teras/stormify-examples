import {
  Box,
  CircularProgress,
  Dialog,
  DialogContent,
  DialogTitle,
  Divider,
  IconButton,
  Stack,
  Typography,
} from "@mui/material";
import CloseRounded from "@mui/icons-material/CloseRounded";
import { useQuery } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { resources } from "../api/resources";
import { useApiBaseUrl } from "../contexts/ApiBaseUrlContext";
import type {
  CategoryDetails,
  CustomerDetails,
  ProductDetails,
  PurchaseOrderDetails,
  SalesOrderDetails,
  ShipmentDetails,
  SupplierDetails,
  WarehouseDetails,
} from "../types/domain";
import { RefLink } from "./RefLink";

export type EntityType =
  | "category"
  | "supplier"
  | "customer"
  | "warehouse"
  | "product"
  | "purchaseOrder"
  | "salesOrder"
  | "shipment";

interface EntityPreviewDialogProps {
  open: boolean;
  type: EntityType;
  id: number;
  onClose: () => void;
}

const titles: Record<EntityType, string> = {
  category: "Category",
  supplier: "Supplier",
  customer: "Customer",
  warehouse: "Warehouse",
  product: "Product",
  purchaseOrder: "Purchase Order",
  salesOrder: "Sales Order",
  shipment: "Shipment",
};

export function EntityPreviewDialog({ open, type, id, onClose }: EntityPreviewDialogProps) {
  const { baseUrl } = useApiBaseUrl();

  const query = useQuery<unknown, Error>({
    queryKey: ["preview", type, baseUrl, id],
    queryFn: () => fetchDetails(type, baseUrl, id),
    enabled: open,
  });

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <span>{titles[type]}</span>
        <IconButton size="small" onClick={onClose}>
          <CloseRounded />
        </IconButton>
      </DialogTitle>
      <DialogContent dividers>
        {query.isLoading && (
          <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
            <CircularProgress size={28} />
          </Box>
        )}
        {query.error && (
          <Typography color="error">Failed to load: {(query.error as Error).message}</Typography>
        )}
        {query.data !== undefined && renderBody(type, query.data)}
      </DialogContent>
    </Dialog>
  );
}

function fetchDetails(type: EntityType, baseUrl: string, id: number): Promise<unknown> {
  switch (type) {
    case "category": return resources.categories.get(baseUrl, id);
    case "supplier": return resources.suppliers.get(baseUrl, id);
    case "customer": return resources.customers.get(baseUrl, id);
    case "warehouse": return resources.warehouses.get(baseUrl, id);
    case "product": return resources.products.get(baseUrl, id);
    case "purchaseOrder": return resources.purchaseOrders.get(baseUrl, id);
    case "salesOrder": return resources.salesOrders.get(baseUrl, id);
    case "shipment": return resources.shipments.get(baseUrl, id);
  }
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <Stack direction="row" spacing={2} sx={{ alignItems: "baseline" }}>
      <Typography variant="body2" sx={{ color: "text.secondary", minWidth: 140 }}>{label}</Typography>
      <Box sx={{ flex: 1 }}>{children}</Box>
    </Stack>
  );
}

function renderBody(type: EntityType, data: unknown): ReactNode {
  switch (type) {
    case "category": return renderCategory(data as CategoryDetails);
    case "supplier": return renderSupplier(data as SupplierDetails);
    case "customer": return renderCustomer(data as CustomerDetails);
    case "warehouse": return renderWarehouse(data as WarehouseDetails);
    case "product": return renderProduct(data as ProductDetails);
    case "purchaseOrder": return renderPurchaseOrder(data as PurchaseOrderDetails);
    case "salesOrder": return renderSalesOrder(data as SalesOrderDetails);
    case "shipment": return renderShipment(data as ShipmentDetails);
  }
}

function renderCategory(d: CategoryDetails) {
  return (
    <Stack spacing={1.5}>
      <Row label="Name">{d.name}</Row>
      <Row label="Description">{d.description}</Row>
      <Row label="Active">{d.active ? "Yes" : "No"}</Row>
    </Stack>
  );
}

function renderSupplier(d: SupplierDetails) {
  return (
    <Stack spacing={1.5}>
      <Row label="Name">{d.name}</Row>
      <Row label="Contact">{d.contactName}</Row>
      <Row label="Email">{d.email}</Row>
      <Row label="Phone">{d.phone}</Row>
      <Row label="City">{d.city}</Row>
      <Row label="Country">{d.country}</Row>
      <Row label="Active">{d.active ? "Yes" : "No"}</Row>
    </Stack>
  );
}

function renderCustomer(d: CustomerDetails) {
  return (
    <Stack spacing={1.5}>
      <Row label="Name">{d.name}</Row>
      <Row label="Type">{d.customerType}</Row>
      <Row label="Email">{d.email}</Row>
      <Row label="Phone">{d.phone}</Row>
      <Row label="City">{d.city}</Row>
      <Row label="Country">{d.country}</Row>
      <Row label="Active">{d.active ? "Yes" : "No"}</Row>
    </Stack>
  );
}

function renderWarehouse(d: WarehouseDetails) {
  return (
    <Stack spacing={1.5}>
      <Row label="Code">{d.code}</Row>
      <Row label="Name">{d.name}</Row>
      <Row label="City">{d.city}</Row>
      <Row label="Country">{d.country}</Row>
      <Row label="Active">{d.active ? "Yes" : "No"}</Row>
    </Stack>
  );
}

function renderProduct(d: ProductDetails) {
  return (
    <Stack spacing={1.5}>
      <Row label="SKU">{d.sku}</Row>
      <Row label="Name">{d.name}</Row>
      <Row label="Description">{d.description}</Row>
      <Row label="Category">
        <RefLink type="category" id={d.category?.id} label={d.category?.name} />
      </Row>
      <Row label="Supplier">
        <RefLink type="supplier" id={d.supplier?.id} label={d.supplier?.name} />
      </Row>
      <Row label="Unit Price">{d.unitPrice.toFixed(2)}</Row>
      <Row label="Reorder Level">{d.reorderLevel}</Row>
      <Row label="Active">{d.active ? "Yes" : "No"}</Row>
    </Stack>
  );
}

function renderPurchaseOrder(d: PurchaseOrderDetails) {
  return (
    <Stack spacing={1.5}>
      <Row label="Order">{d.orderNumber}</Row>
      <Row label="Supplier">
        <RefLink type="supplier" id={d.supplier?.id} label={d.supplier?.label} />
      </Row>
      <Row label="Warehouse">
        <RefLink type="warehouse" id={d.warehouse?.id} label={d.warehouse?.label} />
      </Row>
      <Row label="Status">{d.status}</Row>
      <Row label="Ordered">{d.orderedAt}</Row>
      <Row label="Expected">{d.expectedAt}</Row>
      <Row label="Received">{d.receivedAt}</Row>
      <Row label="Total">{d.totalAmount.toFixed(2)}</Row>
      {d.notes && <Row label="Notes">{d.notes}</Row>}
      {d.items.length > 0 && (
        <>
          <Divider sx={{ my: 1 }} />
          <Typography variant="body2" sx={{ fontWeight: 600 }}>Items ({d.items.length})</Typography>
          {d.items.map((item) => (
            <Row key={item.id} label={`× ${item.quantity}`}>
              <RefLink type="product" id={item.productId} label={item.productName ?? item.productSku} />
              {" — "}
              {item.lineTotal.toFixed(2)}
            </Row>
          ))}
        </>
      )}
    </Stack>
  );
}

function renderSalesOrder(d: SalesOrderDetails) {
  return (
    <Stack spacing={1.5}>
      <Row label="Order">{d.orderNumber}</Row>
      <Row label="Customer">
        <RefLink type="customer" id={d.customer?.id} label={d.customer?.label} />
      </Row>
      <Row label="Warehouse">
        <RefLink type="warehouse" id={d.warehouse?.id} label={d.warehouse?.label} />
      </Row>
      <Row label="Status">{d.status}</Row>
      <Row label="Ordered">{d.orderedAt}</Row>
      <Row label="Confirmed">{d.confirmedAt}</Row>
      <Row label="Total">{d.totalAmount.toFixed(2)}</Row>
      {d.notes && <Row label="Notes">{d.notes}</Row>}
      {d.items.length > 0 && (
        <>
          <Divider sx={{ my: 1 }} />
          <Typography variant="body2" sx={{ fontWeight: 600 }}>Items ({d.items.length})</Typography>
          {d.items.map((item) => (
            <Row key={item.id} label={`× ${item.quantity}`}>
              <RefLink type="product" id={item.productId} label={item.productName ?? item.productSku} />
              {" — "}
              {item.lineTotal.toFixed(2)}
            </Row>
          ))}
        </>
      )}
    </Stack>
  );
}

function renderShipment(d: ShipmentDetails) {
  return (
    <Stack spacing={1.5}>
      <Row label="Shipment">{d.shipmentNumber}</Row>
      <Row label="Sales Order">
        <RefLink type="salesOrder" id={d.salesOrder?.id} label={d.salesOrder?.label} />
      </Row>
      <Row label="Warehouse">
        <RefLink type="warehouse" id={d.warehouse?.id} label={d.warehouse?.label} />
      </Row>
      <Row label="Status">{d.status}</Row>
      <Row label="Carrier">{d.carrier}</Row>
      <Row label="Tracking">{d.trackingCode}</Row>
      <Row label="Shipped">{d.shippedAt}</Row>
      <Row label="Delivered">{d.deliveredAt}</Row>
    </Stack>
  );
}
