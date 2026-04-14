import AddRounded from "@mui/icons-material/AddRounded";
import DeleteRounded from "@mui/icons-material/DeleteRounded";
import EditRounded from "@mui/icons-material/EditRounded";
import {
  Alert,
  Button,
  Fade,
  IconButton,
  Snackbar,
  Stack,
  Tooltip,
} from "@mui/material";
import type { ColDef, ICellRendererParams } from "ag-grid-community";
import { useMutation } from "@tanstack/react-query";
import { useCallback, useMemo, useState } from "react";
import { FormDialog, type FieldConfig } from "../../components/FormDialog";
import { PageCard } from "../../components/PageCard";
import { SearchDataGrid } from "../../components/SearchDataGrid";
import { useApiBaseUrl } from "../../contexts/ApiBaseUrlContext";
import type { PageSpec, PagedResponse } from "../../types/common";

interface MasterDataPageProps<
  TList extends { id: number },
  TDetails,
  TInput extends object,
> {
  title: string;
  subtitle: string;
  queryKey: string;
  columns: ColDef<TList>[];
  fields: ReadonlyArray<FieldConfig<TInput>>;
  createEmpty: () => TInput;
  search: (baseUrl: string, spec: PageSpec) => Promise<PagedResponse<TList>>;
  get: (baseUrl: string, id: number) => Promise<TDetails>;
  create: (baseUrl: string, body: TInput) => Promise<TDetails>;
  update: (baseUrl: string, id: number, body: TInput) => Promise<TDetails>;
  remove: (baseUrl: string, id: number) => Promise<void>;
  fromDetails: (details: TDetails) => TInput;
}

export function MasterDataPage<TList extends { id: number }, TDetails, TInput extends object>({
  title,
  subtitle,
  queryKey,
  columns,
  fields,
  createEmpty,
  search,
  get,
  create,
  update,
  remove,
  fromDetails,
}: MasterDataPageProps<TList, TDetails, TInput>) {
  const { baseUrl } = useApiBaseUrl();
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [formValue, setFormValue] = useState<TInput>(createEmpty());
  const [refreshToken, setRefreshToken] = useState(0);
  const [snackOpen, setSnackOpen] = useState(false);
  const [snackContent, setSnackContent] = useState<{ message: string; severity: "success" | "error" }>({ message: "", severity: "success" });
  const showSnackbar = (message: string, severity: "success" | "error") => {
    setSnackContent({ message, severity });
    setSnackOpen(true);
  };

  const openCreate = () => {
    setSelectedId(null);
    setFormValue(createEmpty());
    setFormOpen(true);
  };

  const openEdit = async (id: number) => {
    const details = await get(baseUrl, id);
    setSelectedId(id);
    setFormValue(fromDetails(details));
    setFormOpen(true);
  };

  const createMutation = useMutation({
    mutationFn: (value: TInput) => create(baseUrl, value),
    onSuccess: () => {
      setFormOpen(false);
      setFormValue(createEmpty());
      setRefreshToken((t) => t + 1);
    },
    onError: (err) => {
      showSnackbar(err instanceof Error ? err.message : "Save failed", "error");
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, value }: { id: number; value: TInput }) => update(baseUrl, id, value),
    onSuccess: () => {
      setFormOpen(false);
      setSelectedId(null);
      setFormValue(createEmpty());
      setRefreshToken((t) => t + 1);
    },
    onError: (err) => {
      showSnackbar(err instanceof Error ? err.message : "Save failed", "error");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => remove(baseUrl, id),
    onSuccess: () => {
      setRefreshToken((t) => t + 1);
      showSnackbar("Record deleted.", "success");
    },
    onError: (err) => {
      showSnackbar(err instanceof Error ? err.message : "Delete failed", "error");
    },
  });

  const actionColumns = useMemo<ColDef<TList>[]>(() => [
    ...columns,
    {
      colId: "__actions__",
      headerName: "Actions",
      width: 110,
      sortable: false,
      filter: false,
      suppressHeaderMenuButton: true,
      resizable: false,
      cellRenderer: (params: ICellRendererParams<TList>) => (
        <Stack direction="row" spacing={0.5} sx={{ alignItems: "center", height: "100%" }}>
          <Tooltip title="Edit">
            <IconButton size="small" onClick={() => params.data && void openEdit(params.data.id)}>
              <EditRounded fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Delete">
            <IconButton
              size="small"
              color="error"
              onClick={() => params.data && void deleteMutation.mutateAsync(params.data.id)}
            >
              <DeleteRounded fontSize="small" />
            </IconButton>
          </Tooltip>
        </Stack>
      ),
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
  ], [columns, deleteMutation]);

  const fetchPage = useCallback(
    (spec: PageSpec) => search(baseUrl, spec),
    [search, baseUrl],
  );

  return (
    <Stack spacing={3}>
      <PageCard
        title={title}
        subtitle={subtitle}
        actions={
          <Button startIcon={<AddRounded />} variant="contained" onClick={openCreate}>
            New Record
          </Button>
        }
      >
        <Stack spacing={2.5}>
          <SearchDataGrid<TList>
            columns={actionColumns}
            fetchPage={fetchPage}
            refreshToken={refreshToken}
            exportFileName={queryKey}
            exportUrl={`${baseUrl}/api/${queryKey}/export`}
          />
        </Stack>
      </PageCard>

      <FormDialog
        open={formOpen}
        title={selectedId == null ? `Create ${title.slice(0, -1)}` : `Edit ${title.slice(0, -1)}`}
        fields={fields}
        value={formValue}
        saving={createMutation.isPending || updateMutation.isPending}
        onChange={(key, value) => setFormValue((prev) => ({ ...prev, [key]: value }))}
        onClose={() => setFormOpen(false)}
        onSubmit={() => {
          if (selectedId == null) {
            createMutation.mutate(formValue);
          } else {
            updateMutation.mutate({ id: selectedId, value: formValue });
          }
        }}
      />

      <Snackbar
        open={snackOpen}
        autoHideDuration={3000}
        onClose={() => setSnackOpen(false)}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
        TransitionComponent={Fade}
        disableWindowBlurListener
      >
        <Alert
          severity={snackContent.severity}
          onClose={() => setSnackOpen(false)}
          variant="filled"
        >
          {snackContent.message}
        </Alert>
      </Snackbar>
    </Stack>
  );
}
