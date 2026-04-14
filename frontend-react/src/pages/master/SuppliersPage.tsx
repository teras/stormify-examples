import type { ColDef } from "ag-grid-community";
import { EnumFilter } from "../../components/EnumFilter";
import { resources } from "../../api/resources";
import { MasterDataPage } from "./MasterDataPage";
import type { SupplierDetails, SupplierInput, SupplierListItem } from "../../types/domain";

const columns: ColDef<SupplierListItem>[] = [
  { field: "name", headerName: "Name", flex: 1.3 },
  { field: "contactName", headerName: "Contact", flex: 1.2 },
  { field: "city", headerName: "City", flex: 1 },
  { field: "country", headerName: "Country", flex: 1 },
  { field: "active", headerName: "Active", width: 110, cellRenderer: (p: { value: unknown }) => (p.value ? "Yes" : "No"), filter: EnumFilter, filterParams: { options: [{ value: "true", label: "Yes" }, { value: "false", label: "No" }] } },
];

const fields = [
  { kind: "text", name: "name", label: "Name", required: true },
  { kind: "text", name: "contactName", label: "Contact name", required: true },
  { kind: "text", name: "email", label: "Email", required: true },
  { kind: "text", name: "phone", label: "Phone", required: true },
  { kind: "text", name: "city", label: "City", required: true },
  { kind: "text", name: "country", label: "Country", required: true },
  { kind: "checkbox", name: "active", label: "Active" },
] as const;

export function SuppliersPage() {
  return (
    <MasterDataPage<SupplierListItem, SupplierDetails, SupplierInput>
      title="Suppliers"
      subtitle="Supplier master data used by products and inbound purchasing flows."
      queryKey="suppliers"
      columns={columns}
      fields={fields}
      createEmpty={() => ({
        name: "",
        contactName: "",
        email: "",
        phone: "",
        city: "",
        country: "",
        active: true,
      })}
      search={(baseUrl, spec) => resources.suppliers.search(baseUrl, spec)}
      get={(baseUrl, id) => resources.suppliers.get(baseUrl, id)}
      create={(baseUrl, body) => resources.suppliers.create(baseUrl, body)}
      update={(baseUrl, id, body) => resources.suppliers.update(baseUrl, id, body)}
      remove={(baseUrl, id) => resources.suppliers.remove(baseUrl, id)}
      fromDetails={(details) => ({
        name: details.name,
        contactName: details.contactName,
        email: details.email,
        phone: details.phone,
        city: details.city,
        country: details.country,
        active: details.active,
      })}
    />
  );
}
