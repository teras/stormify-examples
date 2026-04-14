import AddRounded from "@mui/icons-material/AddRounded";
import DeleteRounded from "@mui/icons-material/DeleteRounded";
import {
  Button,
  IconButton,
  InputAdornment,
  MenuItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
} from "@mui/material";

export interface OrderLineForm {
  productId: number;
  quantity: number;
  unitValue: number;
}

interface ProductOption {
  id: number;
  label: string;
}

interface OrderItemsEditorProps {
  lines: OrderLineForm[];
  products: ProductOption[];
  unitLabel: string;
  onChange: (lines: OrderLineForm[]) => void;
}

export function OrderItemsEditor({ lines, products, unitLabel, onChange }: OrderItemsEditorProps) {
  const updateRow = (index: number, next: Partial<OrderLineForm>) => {
    onChange(lines.map((line, currentIndex) => (currentIndex === index ? { ...line, ...next } : line)));
  };

  return (
    <Stack spacing={2}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Product</TableCell>
            <TableCell width={120}>Quantity</TableCell>
            <TableCell width={160}>{unitLabel}</TableCell>
            <TableCell width={140}>Line Total</TableCell>
            <TableCell width={72} />
          </TableRow>
        </TableHead>
        <TableBody>
          {lines.map((line, index) => (
            <TableRow key={`${line.productId}-${index}`}>
              <TableCell>
                <TextField
                  select
                  fullWidth
                  size="small"
                  value={line.productId || ""}
                  onChange={(event) => updateRow(index, { productId: Number(event.target.value) })}
                >
                  {products.map((product) => (
                    <MenuItem key={product.id} value={product.id}>
                      {product.label}
                    </MenuItem>
                  ))}
                </TextField>
              </TableCell>
              <TableCell>
                <TextField
                  fullWidth
                  size="small"
                  type="number"
                  value={line.quantity}
                  onChange={(event) => updateRow(index, { quantity: Number(event.target.value) })}
                />
              </TableCell>
              <TableCell>
                <TextField
                  fullWidth
                  size="small"
                  type="number"
                  value={line.unitValue}
                  onChange={(event) => updateRow(index, { unitValue: Number(event.target.value) })}
                  slotProps={{
                    input: {
                      startAdornment: <InputAdornment position="start">€</InputAdornment>,
                    },
                  }}
                />
              </TableCell>
              <TableCell>{(line.quantity * line.unitValue).toFixed(2)}</TableCell>
              <TableCell>
                <IconButton color="error" onClick={() => onChange(lines.filter((_, currentIndex) => currentIndex !== index))}>
                  <DeleteRounded />
                </IconButton>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      <Button
        startIcon={<AddRounded />}
        onClick={() => onChange([...lines, { productId: products[0]?.id ?? 0, quantity: 1, unitValue: 0 }])}
      >
        Add Item
      </Button>
    </Stack>
  );
}
