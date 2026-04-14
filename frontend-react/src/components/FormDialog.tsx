import {
  Box,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
} from "@mui/material";
import { useEffect, useState, type ReactNode } from "react";

export interface SelectOption {
  value: number | string;
  label: string;
}

export type FieldConfig<TForm> =
  | {
      kind: "text" | "number" | "date" | "multiline";
      name: keyof TForm;
      label: string;
      required?: boolean;
      min?: number;
    }
  | {
      kind: "checkbox";
      name: keyof TForm;
      label: string;
    }
  | {
      kind: "select";
      name: keyof TForm;
      label: string;
      options: ReadonlyArray<SelectOption>;
      nullable?: boolean;
    };

interface FormDialogProps<TForm extends object> {
  open: boolean;
  title: string;
  fields: ReadonlyArray<FieldConfig<TForm>>;
  value: TForm;
  saving?: boolean;
  onChange: <K extends keyof TForm>(key: K, value: TForm[K]) => void;
  onClose: () => void;
  onSubmit: () => void;
  extraContent?: ReactNode;
}

export function FormDialog<TForm extends object>({
  open,
  title,
  fields,
  value,
  saving,
  onChange,
  onClose,
  onSubmit,
  extraContent,
}: FormDialogProps<TForm>) {
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!open) setErrors({});
  }, [open]);

  const validate = (): boolean => {
    const next: Record<string, string> = {};
    for (const field of fields) {
      if (field.kind === "checkbox" || field.kind === "select") continue;
      if (!field.required) continue;
      const raw = value[field.name];
      const isEmpty = raw == null
        || (typeof raw === "string" && raw.trim() === "")
        || (field.kind === "number" && (typeof raw !== "number" || Number.isNaN(raw)));
      if (isEmpty) next[String(field.name)] = `${field.label} is required`;
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = () => {
    if (validate()) onSubmit();
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
      <DialogTitle>{title}</DialogTitle>
      <DialogContent dividers>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {fields.map((field) => {
            if (field.kind === "checkbox") {
              return (
                <FormControlLabel
                  key={String(field.name)}
                  control={
                    <Checkbox
                      checked={Boolean(value[field.name])}
                      onChange={(event) => onChange(field.name, event.target.checked as TForm[keyof TForm])}
                    />
                  }
                  label={field.label}
                />
              );
            }

            if (field.kind === "select") {
              return (
                <FormControl fullWidth key={String(field.name)}>
                  <InputLabel>{field.label}</InputLabel>
                  <Select
                    label={field.label}
                    value={value[field.name] == null ? "" : String(value[field.name])}
                    onChange={(event) => {
                      const selected = event.target.value;
                      const nextValue = selected === "" && field.nullable
                        ? null
                        : typeof field.options[0]?.value === "number"
                          ? Number(selected)
                          : selected;
                      onChange(field.name, nextValue as TForm[keyof TForm]);
                    }}
                  >
                    {field.nullable && <MenuItem value="">None</MenuItem>}
                    {field.options.map((option) => (
                      <MenuItem key={String(option.value)} value={String(option.value)}>
                        {option.label}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              );
            }

            const fieldError = errors[String(field.name)];
            return (
              <TextField
                key={String(field.name)}
                fullWidth
                type={field.kind === "multiline" ? "text" : field.kind}
                multiline={field.kind === "multiline"}
                minRows={field.kind === "multiline" ? 3 : undefined}
                label={field.label}
                required={field.required}
                error={Boolean(fieldError)}
                helperText={fieldError}
                value={String(value[field.name] ?? "")}
                inputProps={field.kind === "number" && field.min != null ? { min: field.min } : undefined}
                onChange={(event) => {
                  const nextValue = field.kind === "number"
                    ? Number(event.target.value)
                    : event.target.value;
                  onChange(field.name, nextValue as TForm[keyof TForm]);
                  if (fieldError) {
                    setErrors((prev) => {
                      const rest = { ...prev };
                      delete rest[String(field.name)];
                      return rest;
                    });
                  }
                }}
              />
            );
          })}
          {extraContent && <Box>{extraContent}</Box>}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={handleSubmit} variant="contained" disabled={saving}>
          Save
        </Button>
      </DialogActions>
    </Dialog>
  );
}
