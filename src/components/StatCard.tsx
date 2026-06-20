import type { ReactNode } from 'react'

type StatCardProps = {
  label: string
  value: string | number
  icon?: ReactNode
}

export function StatCard({ label, value, icon }: StatCardProps) {
  return (
    <div className="stat-card">
      {icon ? <div className="stat-icon">{icon}</div> : null}
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}
