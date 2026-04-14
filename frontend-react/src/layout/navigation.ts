import DashboardRounded from "@mui/icons-material/DashboardRounded";
import CategoryRounded from "@mui/icons-material/CategoryRounded";
import GroupsRounded from "@mui/icons-material/GroupsRounded";
import Inventory2Rounded from "@mui/icons-material/Inventory2Rounded";
import LocalShippingRounded from "@mui/icons-material/LocalShippingRounded";
import StoreRounded from "@mui/icons-material/StoreRounded";
import WarehouseRounded from "@mui/icons-material/WarehouseRounded";
import ReceiptLongRounded from "@mui/icons-material/ReceiptLongRounded";
import PointOfSaleRounded from "@mui/icons-material/PointOfSaleRounded";
import type { SvgIconComponent } from "@mui/icons-material";

export interface NavigationItem {
  label: string;
  path: string;
  icon: SvgIconComponent;
}

export interface NavigationGroup {
  heading?: string;
  items: NavigationItem[];
}

export const navigationGroups: NavigationGroup[] = [
  {
    items: [{ label: "Dashboard", path: "/", icon: DashboardRounded }],
  },
  {
    heading: "Master Data",
    items: [
      { label: "Categories", path: "/categories", icon: CategoryRounded },
      { label: "Suppliers", path: "/suppliers", icon: StoreRounded },
      { label: "Customers", path: "/customers", icon: GroupsRounded },
      { label: "Warehouses", path: "/warehouses", icon: WarehouseRounded },
      { label: "Products", path: "/products", icon: Inventory2Rounded },
    ],
  },
  {
    heading: "Operations",
    items: [
      { label: "Stock", path: "/stock", icon: WarehouseRounded },
      { label: "Purchase Orders", path: "/purchase-orders", icon: ReceiptLongRounded },
      { label: "Sales Orders", path: "/sales-orders", icon: PointOfSaleRounded },
      { label: "Shipments", path: "/shipments", icon: LocalShippingRounded },
    ],
  },
];

// Flat list used for page title lookup.
export const navigationItems: NavigationItem[] = navigationGroups.flatMap((g) => g.items);
