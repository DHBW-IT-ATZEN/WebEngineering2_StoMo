/** Batch-translate UI strings to Yodish. Returns a { source: translated } map. */
export async function translateBatch(texts) {
  if (!texts || texts.length === 0) return {};
  const res = await fetch('/api/yoda/translate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ texts }),
  });
  if (!res.ok) throw new Error(`translate failed (${res.status})`);
  return res.json();
}
