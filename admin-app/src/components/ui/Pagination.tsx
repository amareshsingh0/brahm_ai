interface PaginationProps {
  page:     number;
  pages:    number;
  onChange: (page: number) => void;
}

export function Pagination({ page, pages, onChange }: PaginationProps) {
  if (pages <= 1) return null;
  return (
    <div className="flex items-center gap-2 mt-4 text-sm">
      <button
        disabled={page === 1}
        onClick={() => onChange(page - 1)}
        className="px-3 py-1 rounded bg-muted text-muted-foreground disabled:opacity-30 hover:bg-border transition-colors"
      >
        ← Prev
      </button>
      <span className="text-muted-foreground">{page} / {pages}</span>
      <button
        disabled={page === pages}
        onClick={() => onChange(page + 1)}
        className="px-3 py-1 rounded bg-muted text-muted-foreground disabled:opacity-30 hover:bg-border transition-colors"
      >
        Next →
      </button>
    </div>
  );
}
