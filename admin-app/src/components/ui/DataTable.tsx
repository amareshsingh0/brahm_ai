// Generic reusable table — add new column definitions without touching table markup
// Usage:
//   <DataTable
//     columns={[{ key: "name", header: "Name", render: (row) => row.name }]}
//     rows={users}
//     loading={loading}
//     empty="No users found."
//     onRowClick={(row) => navigate(`/users/${row.id}`)}
//   />

import { Loader, Empty } from "./Loader";

export interface Column<T> {
  key:        string;
  header:     string;
  className?: string;
  render:     (row: T) => React.ReactNode;
}

interface DataTableProps<T> {
  columns:     Column<T>[];
  rows:        T[];
  loading?:    boolean;
  empty?:      string;
  onRowClick?: (row: T) => void;
  className?:  string;
}

export function DataTable<T>({ columns, rows, loading, empty, onRowClick, className }: DataTableProps<T>) {
  return (
    <div className={`rounded-xl border border-border bg-white overflow-x-auto shadow-sm ${className ?? ""}`}>
      <table className="w-full text-xs">
        <thead>
          <tr className="border-b border-border bg-muted/50">
            {columns.map((col) => (
              <th key={col.key} className={`text-left px-4 py-3 text-muted-foreground font-medium whitespace-nowrap ${col.className ?? ""}`}>
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading ? (
            <tr>
              <td colSpan={columns.length} className="py-8">
                <Loader />
              </td>
            </tr>
          ) : rows.length === 0 ? (
            <tr>
              <td colSpan={columns.length}>
                <Empty msg={empty} />
              </td>
            </tr>
          ) : (
            rows.map((row, i) => (
              <tr
                key={i}
                className={`border-b border-border/50 hover:bg-muted/40 transition-colors ${onRowClick ? "cursor-pointer" : ""}`}
                onClick={() => onRowClick?.(row)}
              >
                {columns.map((col) => (
                  <td key={col.key} className={`px-4 py-2.5 ${col.className ?? ""}`}>
                    {col.render(row)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
