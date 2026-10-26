// Parse untrusted spreadsheets off the UI thread. The caller enforces a timeout.
importScripts('./vendor/xlsx-0.20.3.full.min.js');
self.onmessage = async ({ data }) => {
  try {
    const { readHistoryWorkbook, writeHistoryWorkbook } = await import('./history-workbook.mjs');
    const result = data.action === 'read'
      ? readHistoryWorkbook(data.buffer, self.XLSX)
      : writeHistoryWorkbook(data.records, self.XLSX, data.language);
    self.postMessage({ result }, result instanceof ArrayBuffer ? [result] : []);
  } catch (error) {
    self.postMessage({ error: error.message || 'No se pudo leer el archivo Excel.' });
  }
};
