/**
 * Batch-translate dynamic strings (company descriptions etc.) to a target language via the
 * backend's LibreTranslate proxy. Returns a { source: translated } map. Static UI labels are
 * handled by the offline dictionary (de.js); this is only for text that can't be pre-translated.
 */
export async function translateBatch(texts, target = 'de') {
  if (!texts || texts.length === 0) return {};
  const res = await fetch('/api/translate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ texts, target }),
  });
  if (!res.ok) throw new Error(`translate failed (${res.status})`);
  return res.json();
}
