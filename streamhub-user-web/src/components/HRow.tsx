/** Horizontal swipe row (scroll-snap, hidden scrollbar). Children set their own width. */
export function HRow({ children }: { children: React.ReactNode }) {
  return <div className="hrow px-5 pb-1">{children}</div>;
}

/** Fixed-width slot for a carousel item. */
export function HItem({ width, children }: { width: number; children: React.ReactNode }) {
  return (
    <div style={{ width }} className="min-w-0">
      {children}
    </div>
  );
}
