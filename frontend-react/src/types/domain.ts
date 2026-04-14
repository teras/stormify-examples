export type CustomerType = "RETAIL" | "WHOLESALE";
export type PurchaseOrderStatus = "DRAFT" | "SUBMITTED" | "RECEIVED" | "CANCELLED";
export type SalesOrderStatus = "DRAFT" | "CONFIRMED" | "SHIPPED" | "CANCELLED";
export type ShipmentStatus = "PREPARING" | "SHIPPED" | "DELIVERED" | "CANCELLED";

export interface CategoryListItem {
  id: number;
  name: string;
  description: string;
  active: boolean;
}

export interface CategoryDetails extends CategoryListItem {}

export interface SupplierListItem {
  id: number;
  name: string;
  contactName: string;
  city: string;
  country: string;
  active: boolean;
}

export interface SupplierDetails extends SupplierListItem {
  email: string;
  phone: string;
}

export interface CustomerListItem {
  id: number;
  name: string;
  email: string;
  city: string;
  country: string;
  customerType: CustomerType;
  active: boolean;
}

export interface CustomerDetails extends CustomerListItem {
  phone: string;
}

export interface WarehouseListItem {
  id: number;
  code: string;
  name: string;
  city: string;
  country: string;
  active: boolean;
}

export interface WarehouseDetails extends WarehouseListItem {}

export interface ProductListItem {
  id: number;
  sku: string;
  name: string;
  categoryId: number | null;
  categoryName: string | null;
  supplierId: number | null;
  supplierName: string | null;
  unitPrice: number;
  active: boolean;
}

export interface ProductDetails {
  id: number;
  sku: string;
  name: string;
  description: string;
  category: { id: number; name: string } | null;
  supplier: { id: number; name: string } | null;
  unitPrice: number;
  reorderLevel: number;
  active: boolean;
}

export interface StockListItem {
  id: number;
  warehouseId: number | null;
  warehouseName: string | null;
  productId: number | null;
  productSku: string | null;
  productName: string | null;
  quantityOnHand: number;
  quantityReserved: number;
  availableQuantity: number;
  reorderLevel: number;
  lastUpdatedAt: string;
}

export interface TransactionReference {
  id: number;
  label: string;
}

export interface PurchaseOrderItemResponse {
  id: number;
  productId: number | null;
  productSku: string | null;
  productName: string | null;
  quantity: number;
  unitCost: number;
  lineTotal: number;
}

export interface PurchaseOrderListItem {
  id: number;
  orderNumber: string;
  supplierId: number | null;
  supplierName: string | null;
  warehouseId: number | null;
  warehouseName: string | null;
  status: PurchaseOrderStatus;
  orderedAt: string;
  expectedAt: string;
  receivedAt: string;
  totalAmount: number;
}

export interface PurchaseOrderDetails {
  id: number;
  orderNumber: string;
  supplier: TransactionReference | null;
  warehouse: TransactionReference | null;
  status: PurchaseOrderStatus;
  orderedAt: string;
  expectedAt: string;
  receivedAt: string;
  notes: string;
  totalAmount: number;
  items: PurchaseOrderItemResponse[];
}

export interface SalesOrderItemResponse {
  id: number;
  productId: number | null;
  productSku: string | null;
  productName: string | null;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface SalesOrderListItem {
  id: number;
  orderNumber: string;
  customerId: number | null;
  customerName: string | null;
  warehouseId: number | null;
  warehouseName: string | null;
  status: SalesOrderStatus;
  orderedAt: string;
  confirmedAt: string;
  totalAmount: number;
}

export interface SalesOrderDetails {
  id: number;
  orderNumber: string;
  customer: TransactionReference | null;
  warehouse: TransactionReference | null;
  status: SalesOrderStatus;
  orderedAt: string;
  confirmedAt: string;
  notes: string;
  totalAmount: number;
  items: SalesOrderItemResponse[];
}

export interface ShipmentListItem {
  id: number;
  shipmentNumber: string;
  salesOrderId: number | null;
  salesOrderNumber: string | null;
  warehouseId: number | null;
  warehouseName: string | null;
  carrier: string;
  trackingCode: string;
  status: ShipmentStatus;
  shippedAt: string;
  deliveredAt: string;
}

export interface ShipmentDetails {
  id: number;
  shipmentNumber: string;
  salesOrder: TransactionReference | null;
  warehouse: TransactionReference | null;
  carrier: string;
  trackingCode: string;
  status: ShipmentStatus;
  shippedAt: string;
  deliveredAt: string;
}

export interface CategoryInput {
  name: string;
  description: string;
  active: boolean;
}

export interface SupplierInput {
  name: string;
  contactName: string;
  email: string;
  phone: string;
  city: string;
  country: string;
  active: boolean;
}

export interface CustomerInput {
  name: string;
  email: string;
  phone: string;
  city: string;
  country: string;
  customerType: CustomerType;
  active: boolean;
}

export interface WarehouseInput {
  code: string;
  name: string;
  city: string;
  country: string;
  active: boolean;
}

export interface ProductInput {
  sku: string;
  name: string;
  description: string;
  categoryId: number | null;
  supplierId: number | null;
  unitPrice: number;
  reorderLevel: number;
  active: boolean;
}

export interface OrderItemInput {
  productId: number;
  quantity: number;
  unitCost?: number;
  unitPrice?: number;
}

export interface PurchaseOrderInput {
  supplierId: number;
  warehouseId: number;
  expectedAt: string;
  notes: string;
  items: Array<{ productId: number; quantity: number; unitCost: number }>;
}

export interface SalesOrderInput {
  customerId: number;
  warehouseId: number;
  notes: string;
  items: Array<{ productId: number; quantity: number; unitPrice: number }>;
}

export interface ShipmentInput {
  salesOrderId: number;
  warehouseId: number;
  carrier: string;
  trackingCode: string;
}
