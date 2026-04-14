import type { ColDef } from "ag-grid-community";
import { EnumFilter } from "../../components/EnumFilter";
import { resources } from "../../api/resources";
import { MasterDataPage } from "./MasterDataPage";
import type { CategoryDetails, CategoryInput, CategoryListItem } from "../../types/domain";

const columns: ColDef<CategoryListItem>[] = [
  { field: "name", headerName: "Name", flex: 1.2 },
  { field: "description", headerName: "Description", flex: 1.6 },
  { field: "active", headerName: "Active", width: 110, cellRenderer: (p: { value: unknown }) => (p.value ? "Yes" : "No"), filter: EnumFilter, filterParams: { options: [{ value: "true", label: "Yes" }, { value: "false", label: "No" }] } },
];

const fields = [
  { kind: "text", name: "name", label: "Name", required: true },
  { kind: "multiline", name: "description", label: "Description" },
  { kind: "checkbox", name: "active", label: "Active" },
] as const;

export function CategoriesPage() {
  return (
    <MasterDataPage<CategoryListItem, CategoryDetails, CategoryInput>
      title="Categories"
      subtitle="Manage product classification and keep aliases stable for shared search facets."
      queryKey="categories"
      columns={columns}
      fields={fields}
      createEmpty={() => ({ name: "", description: "", active: true })}
      search={(baseUrl, spec) => resources.categories.search(baseUrl, spec)}
      get={(baseUrl, id) => resources.categories.get(baseUrl, id)}
      create={(baseUrl, body) => resources.categories.create(baseUrl, body)}
      update={(baseUrl, id, body) => resources.categories.update(baseUrl, id, body)}
      remove={(baseUrl, id) => resources.categories.remove(baseUrl, id)}
      fromDetails={(details) => ({ name: details.name, description: details.description, active: details.active })}
    />
  );
}
