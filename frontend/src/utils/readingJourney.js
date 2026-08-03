export const READING_STATUS_OPTIONS = [
  { value: 'NOT_STARTED', label: 'Want to read' },
  { value: 'READING', label: 'Reading' },
  { value: 'PAUSED', label: 'Paused' },
  { value: 'COMPLETED', label: 'Completed' },
]

export const READING_FILTERS = [
  { value: 'ALL', label: 'All' },
  ...READING_STATUS_OPTIONS,
]

export function readingStatusLabel(status) {
  return READING_STATUS_OPTIONS.find((option) => option.value === status)?.label || status
}

export function readingDate(value) {
  if (!value) return 'Not recorded'
  return new Date(`${value}T00:00:00`).toLocaleDateString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
  })
}
