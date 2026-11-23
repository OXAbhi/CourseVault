export function formatInstant(iso: string | null | undefined): string {
  if (!iso) {
    return '—';
  }
  return new Date(iso).toLocaleString();
}

/** `datetime-local` value → ISO-8601 for the API */
export function datetimeLocalToIso(value: string): string {
  return new Date(value).toISOString();
}