import { FormControl, MenuItem, Select } from "@mui/material";
import type { CustomFilterProps } from "ag-grid-react";
import { useGridFilter } from "ag-grid-react";
import { useCallback, useRef } from "react";

export interface EnumOption {
  value: string;
  label: string;
}

interface EnumFilterProps extends CustomFilterProps {
  options: readonly (string | EnumOption)[];
}

function toOption(opt: string | EnumOption): EnumOption {
  return typeof opt === "string" ? { value: opt, label: opt } : opt;
}

export function EnumFilter({ options, model, onModelChange, getValue, api }: EnumFilterProps) {
  const refContainer = useRef<HTMLDivElement>(null);
  const value = model?.value ?? "";

  const doesFilterPass = useCallback(
    ({ node }: { node: Parameters<typeof getValue>[0] }) => {
      if (!value) return true;
      const cellValue = getValue(node);
      return cellValue != null && String(cellValue) === value;
    },
    [value, getValue],
  );

  useGridFilter({ doesFilterPass });

  const handleChange = useCallback(
    (newValue: string) => {
      onModelChange(newValue ? { value: newValue } : null);
      setTimeout(() => api.hidePopupMenu(), 0);
    },
    [onModelChange, api],
  );

  const resolved = options.map(toOption);

  return (
    <div ref={refContainer}>
      <FormControl size="small" sx={{ m: 1, minWidth: 140 }}>
        <Select
          value={value}
          displayEmpty
          onChange={(e) => handleChange(e.target.value)}
          MenuProps={{ disablePortal: true }}
        >
          <MenuItem value="">
            <em>All</em>
          </MenuItem>
          {resolved.map((opt) => (
            <MenuItem key={opt.value} value={opt.value}>{opt.label}</MenuItem>
          ))}
        </Select>
      </FormControl>
    </div>
  );
}
