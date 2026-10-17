import BarChartIcon from '@mui/icons-material/BarChart';
import TableRowsOutlinedIcon from '@mui/icons-material/TableRowsOutlined';
import {
  Card, CardContent, Stack, Table, TableBody, TableCell, TableHead, TableRow, ToggleButton, ToggleButtonGroup,
  Typography,
} from '@mui/material';
import type { ReactNode } from 'react';
import { useState } from 'react';

export interface TableData {
  columns: string[];
  rows: (string | number)[][];
}

interface ChartCardProps {
  title: string;
  subtitle?: string;
  /** AC-12.4: alternativa en tabla de cada gráfico. */
  table: TableData;
  children: ReactNode;
  empty?: boolean;
  emptyText?: string;
  /** Ocupar el alto de la celda; desactivar cuando se apilan varias tarjetas en una columna. */
  fill?: boolean;
}

/** Tarjeta de gráfico con conmutador Gráfico/Tabla; la tabla es la alternativa accesible. */
export function ChartCard({ title, subtitle, table, children, empty, emptyText = 'Sin datos para estos filtros.', fill = true }: ChartCardProps) {
  const [view, setView] = useState<'chart' | 'table'>('chart');
  return (
    <Card sx={{ height: fill ? '100%' : 'auto' }}>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={1} sx={{ mb: 2 }}>
          <div>
            <Typography variant="h6" component="h2" sx={{ fontSize: '1.05rem' }}>{title}</Typography>
            {subtitle && <Typography variant="caption" color="text.secondary">{subtitle}</Typography>}
          </div>
          {!empty && (
            <ToggleButtonGroup
              size="small" exclusive value={view} onChange={(_, value) => value && setView(value)}
              aria-label={`Vista de ${title}`}
            >
              <ToggleButton value="chart" aria-label="Ver gráfico"><BarChartIcon fontSize="small" /></ToggleButton>
              <ToggleButton value="table" aria-label="Ver tabla"><TableRowsOutlinedIcon fontSize="small" /></ToggleButton>
            </ToggleButtonGroup>
          )}
        </Stack>
        {empty ? (
          <Typography variant="body2" color="text.secondary">{emptyText}</Typography>
        ) : view === 'chart' ? children : (
          <Table size="small" aria-label={title}>
            <TableHead>
              <TableRow>
                {table.columns.map((column, index) => (
                  <TableCell key={column} align={index === 0 ? 'left' : 'right'}>{column}</TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {table.rows.map((row) => (
                <TableRow key={String(row[0])}>
                  {row.map((cell, index) => (
                    <TableCell key={index} align={index === 0 ? 'left' : 'right'} sx={{ fontVariantNumeric: 'tabular-nums' }}>{cell}</TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}
