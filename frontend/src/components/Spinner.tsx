export function Spinner({ label }: { label?: string }) {
  return (
    <div className="spinner-wrap">
      <span className="spinner" />
      {label && <span className="muted small">{label}</span>}
    </div>
  )
}

export function Empty({ text, sub }: { text: string; sub?: string }) {
  return (
    <div className="empty">
      <p>{text}</p>
      {sub && <p className="muted small">{sub}</p>}
    </div>
  )
}
