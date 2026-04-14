import type { ColDef, ICellRendererParams, ValueFormatterParams } from "ag-grid-community";
import { EnumFilter } from "../../components/EnumFilter";
import { resources } from "../../api/resources";
import { useReferenceData } from "../../features/referenceData";
import { MasterDataPage } from "./MasterDataPage";
import { RefLink } from "../../components/RefLink";
import { numberFilterParams } from "../../components/SearchDataGrid";
import type { ProductDetails, ProductInput, ProductListItem } from "../../types/domain";

const columns: ColDef<ProductListItem>[] = [
  { field: "sku", headerName: "SKU", flex: 0.9 },
  { field: "name", headerName: "Name", flex: 1.4 },
  {
    field: "categoryName",
    headerName: "Category",
    flex: 1,
    cellRenderer: (p: ICellRendererParams<ProductListItem>) =>
      p.data ? <RefLink type="category" id={p.data.categoryId} label={p.data.categoryName} /> : null,
  },
  {
    field: "supplierName",
    headerName: "Supplier",
    flex: 1,
    cellRenderer: (p: ICellRendererParams<ProductListItem>) =>
      p.data ? <RefLink type="supplier" id={p.data.supplierId} label={p.data.supplierName} /> : null,
  },
  {
    field: "unitPrice",
    headerName: "Unit Price",
    flex: 0.8,
    filter: "agNumberColumnFilter", filterParams: numberFilterParams,
    valueFormatter: (p: ValueFormatterParams) => Number(p.value).toFixed(2),
  },
  { field: "active", headerName: "Active", width: 110, cellRenderer: (p: { value: unknown }) => (p.value ? "Yes" : "No"), filter: EnumFilter, filterParams: { options: [{ value: "true", label: "Yes" }, { value: "false", label: "No" }] } },
];

export function ProductsPage() {
  const refs = useReferenceData();

  const fields = [
    { kind: "text", name: "sku", label: "SKU", required: true },
    { kind: "text", name: "name", label: "Name", required: true },
    { kind: "multiline", name: "description", label: "Description" },
    {
      kind: "select",
      name: "categoryId",
      label: "Category",
      options: refs.categories.map((item) => ({ value: item.id, label: item.name })),
      nullable: true,
    },
    {
      kind: "select",
      name: "supplierId",
      label: "Supplier",
      options: refs.suppliers.map((item) => ({ value: item.id, label: item.name })),
      nullable: true,
    },
    { kind: "number", name: "unitPrice", label: "Unit price", min: 0 },
    { kind: "number", name: "reorderLevel", label: "Reorder level", min: 0 },
    { kind: "checkbox", name: "active", label: "Active" },
  ] as const;

  return (
    <MasterDataPage<ProductListItem, ProductDetails, ProductInput>
      title="Products"
      subtitle="Product catalog, supplier linkage and reorder thresholds."
      queryKey="products"
      columns={columns}
      fields={fields}
      createEmpty={() => ({
        sku: "",
        name: "",
        description: "",
        categoryId: null,
        supplierId: null,
        unitPrice: 0,
        reorderLevel: 0,
        active: true,
      })}
      search={(baseUrl, spec) => resources.products.search(baseUrl, spec)}
      get={(baseUrl, id) => resources.products.get(baseUrl, id)}
      create={(baseUrl, body) => resources.products.create(baseUrl, body)}
      update={(baseUrl, id, body) => resources.products.update(baseUrl, id, body)}
      remove={(baseUrl, id) => resources.products.remove(baseUrl, id)}
      fromDetails={(details) => ({
        sku: details.sku,
        name: details.name,
        description: details.description,
        categoryId: details.category?.id ?? null,
        supplierId: details.supplier?.id ?? null,
        unitPrice: details.unitPrice,
        reorderLevel: details.reorderLevel,
        active: details.active,
      })}
    />
  );
}
