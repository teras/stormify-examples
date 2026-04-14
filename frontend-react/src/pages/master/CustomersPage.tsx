import type { ColDef } from "ag-grid-community";
import { resources } from "../../api/resources";
import { EnumFilter } from "../../components/EnumFilter";
import { MasterDataPage } from "./MasterDataPage";
import type { CustomerDetails, CustomerInput, CustomerListItem } from "../../types/domain";

const customerTypes = ["RETAIL", "WHOLESALE"] as const;

const columns: ColDef<CustomerListItem>[] = [
  { field: "name", headerName: "Name", flex: 1.3 },
  { field: "email", headerName: "Email", flex: 1.3 },
  { field: "city", headerName: "City", flex: 1 },
  { field: "country", headerName: "Country", flex: 1 },
  {
    field: "customerType", headerName: "Type", flex: 0.9,
    filter: EnumFilter, filterParams: { options: customerTypes },
  },
  { field: "active", headerName: "Active", width: 110, cellRenderer: (p: { value: unknown }) => (p.value ? "Yes" : "No"), filter: EnumFilter, filterParams: { options: [{ value: "true", label: "Yes" }, { value: "false", label: "No" }] } },
];

const fields = [
  { kind: "text", name: "name", label: "Name", required: true },
  { kind: "text", name: "email", label: "Email", required: true },
  { kind: "text", name: "phone", label: "Phone", required: true },
  { kind: "text", name: "city", label: "City", required: true },
  { kind: "text", name: "country", label: "Country", required: true },
  {
    kind: "select",
    name: "customerType",
    label: "Customer type",
    options: [
      { value: "RETAIL", label: "Retail" },
      { value: "WHOLESALE", label: "Wholesale" },
    ],
  },
  { kind: "checkbox", name: "active", label: "Active" },
] as const;

export function CustomersPage() {
  return (
    <MasterDataPage<CustomerListItem, CustomerDetails, CustomerInput>
      title="Customers"
      subtitle="Commercial customers used by sales orders and shipment flows."
      queryKey="customers"
      columns={columns}
      fields={fields}
      createEmpty={() => ({
        name: "",
        email: "",
        phone: "",
        city: "",
        country: "",
        customerType: "RETAIL",
        active: true,
      })}
      search={(baseUrl, spec) => resources.customers.search(baseUrl, spec)}
      get={(baseUrl, id) => resources.customers.get(baseUrl, id)}
      create={(baseUrl, body) => resources.customers.create(baseUrl, body)}
      update={(baseUrl, id, body) => resources.customers.update(baseUrl, id, body)}
      remove={(baseUrl, id) => resources.customers.remove(baseUrl, id)}
      fromDetails={(details) => ({
        name: details.name,
        email: details.email,
        phone: details.phone,
        city: details.city,
        country: details.country,
        customerType: details.customerType,
        active: details.active,
      })}
    />
  );
}
