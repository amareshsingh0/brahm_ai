import type { ReactNode } from "react";

export function Loader() {
  return (
    <div className="flex items-center justify-center py-12">
      <div className="w-6 h-6 rounded-full border-2 border-amber-200 border-t-amber-600 animate-spin" />
    </div>
  );
}

export function Empty({ msg = "No data." }: { msg?: string }) {
  return <p className="text-muted-foreground py-10 text-center text-sm">{msg}</p>;
}

export function ActionBtn({
  label, icon, cls = "", loading = false, onClick,
}: {
  label:    string;
  icon?:    ReactNode;
  cls?:     string;
  loading?: boolean;
  onClick:  () => void;
}) {
  return (
    <button
      onClick={onClick}
      disabled={loading}
      className={`flex items-center gap-1.5 px-2.5 py-1 rounded text-xs font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${cls}`}
    >
      {icon}
      {label}
    </button>
  );
}
