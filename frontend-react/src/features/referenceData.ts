import { useQuery } from "@tanstack/react-query";
import { resources } from "../api/resources";
import { useApiBaseUrl } from "../contexts/ApiBaseUrlContext";
import type { PageSpec } from "../types/common";

const allRowsSpec: PageSpec = {
  page: 0,
  pageSize: 200,
  filters: {},
  sorts: {},
  caseSensitive: {},
};

export function useReferenceData() {
  const { baseUrl } = useApiBaseUrl();

  const categories = useQuery({
    queryKey: ["ref-categories", baseUrl],
    queryFn: () => resources.categories.search(baseUrl, allRowsSpec),
  });
  const suppliers = useQuery({
    queryKey: ["ref-suppliers", baseUrl],
    queryFn: () => resources.suppliers.search(baseUrl, allRowsSpec),
  });
  const customers = useQuery({
    queryKey: ["ref-customers", baseUrl],
    queryFn: () => resources.customers.search(baseUrl, allRowsSpec),
  });
  const warehouses = useQuery({
    queryKey: ["ref-warehouses", baseUrl],
    queryFn: () => resources.warehouses.search(baseUrl, allRowsSpec),
  });
  const products = useQuery({
    queryKey: ["ref-products", baseUrl],
    queryFn: () => resources.products.search(baseUrl, allRowsSpec),
  });

  return {
    categories: categories.data?.items ?? [],
    suppliers: suppliers.data?.items ?? [],
    customers: customers.data?.items ?? [],
    warehouses: warehouses.data?.items ?? [],
    products: products.data?.items ?? [],
    isLoading:
      categories.isLoading ||
      suppliers.isLoading ||
      customers.isLoading ||
      warehouses.isLoading ||
      products.isLoading,
  };
}
