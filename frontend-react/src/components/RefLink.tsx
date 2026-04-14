import { Link } from "@mui/material";
import { useState } from "react";
import { EntityPreviewDialog, type EntityType } from "./EntityPreviewDialog";

interface RefLinkProps {
  type: EntityType;
  id: number | null | undefined;
  label: string | null | undefined;
}

export function RefLink({ type, id, label }: RefLinkProps) {
  const [open, setOpen] = useState(false);
  if (id == null || !label) return <span>—</span>;
  return (
    <>
      <Link
        component="button"
        type="button"
        underline="hover"
        onClick={(e) => {
          e.stopPropagation();
          setOpen(true);
        }}
        sx={{ verticalAlign: "baseline", cursor: "pointer" }}
      >
        {label}
      </Link>
      <EntityPreviewDialog type={type} id={id} open={open} onClose={() => setOpen(false)} />
    </>
  );
}
