import type { ColDef } from "ag-grid-community";
import { EnumFilter } from "../../components/EnumFilter";
import { resources } from "../../api/resources";
import { MasterDataPage } from "./MasterDataPage";
import type { WarehouseDetails, WarehouseInput, WarehouseListItem } from "../../types/domain";

const columns: ColDef<WarehouseListItem>[] = [
  { field: "code", headerName: "Code", flex: 0.8 },
  { field: "name", headerName: "Name", flex: 1.3 },
  { field: "city", headerName: "City", flex: 1 },
  { field: "country", headerName: "Country", flex: 1 },
  { field: "active", headerName: "Active", width: 110, cellRenderer: (p: { value: unknown }) => (p.value ? "Yes" : "No"), filter: EnumFilter, filterParams: { options: [{ value: "true", label: "Yes" }, { value: "false", label: "No" }] } },
];

const fields = [
  { kind: "text", name: "code", label: "Code", required: true },
  { kind: "text", name: "name", label: "Name", required: true },
  { kind: "text", name: "city", label: "City", required: true },
  { kind: "text", name: "country", label: "Country", required: true },
  { kind: "checkbox", name: "active", label: "Active" },
] as const;

export function WarehousesPage() {
  return (
    <MasterDataPage<WarehouseListItem, WarehouseDetails, WarehouseInput>
      title="Warehouses"
      subtitle="Storage locations and stock pools used across inbound and outbound flows."
      queryKey="warehouses"
      columns={columns}
      fields={fields}
      createEmpty={() => ({ code: "", name: "", city: "", country: "", active: true })}
      search={(baseUrl, spec) => resources.warehouses.search(baseUrl, spec)}
      get={(baseUrl, id) => resources.warehouses.get(baseUrl, id)}
      create={(baseUrl, body) => resources.warehouses.create(baseUrl, body)}
      update={(baseUrl, id, body) => resources.warehouses.update(baseUrl, id, body)}
      remove={(baseUrl, id) => resources.warehouses.remove(baseUrl, id)}
      fromDetails={(details) => ({
        code: details.code,
        name: details.name,
        city: details.city,
        country: details.country,
        active: details.active,
      })}
    />
  );
}
