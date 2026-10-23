// SheetJS is supplied by the worker (or by tests), keeping this adapter testable.
export function readHistoryWorkbook(buffer, XLSX) {
  const bytes = new Uint8Array(buffer);
  if (!(bytes[0] === 0x50 && bytes[1] === 0x4b) && !(bytes[0] === 0xd0 && bytes[1] === 0xcf)) {
    throw new Error('El archivo no es un libro Excel válido. Usá .xlsx, .xls o CSV.');
  }
  const book = XLSX.read(buffer, { type: 'array', cellNF: true, cellDates: false, sheetRows: 5002, cellFormula: true });
  if (book.SheetNames.length > 20) throw new Error('Usá un libro con hasta 20 hojas.');
  let totalCells = 0;
  const sheets = book.SheetNames.flatMap((name, index) => {
    if (book.Workbook?.Sheets?.[index]?.Hidden) return [];
    const sheet = book.Sheets[name];
    if (!sheet?.['!ref']) return [];
    const range = XLSX.utils.decode_range(sheet['!fullref'] || sheet['!ref']);
    if (range.e.r > 5000 || range.e.c > 63) return [{ name, error: 'Esta hoja supera 5001 filas o 64 columnas. Copiá la tabla a un libro más pequeño.' }];
    totalCells += (range.e.r + 1) * (range.e.c + 1);
    if (totalCells > 150000) throw new Error('El libro contiene demasiadas celdas. Importá una hoja por archivo.');
    const rows = [];
    let error = '';
    for (let r = 0; r <= range.e.r; r++) {
      const row = [];
      for (let c = 0; c <= range.e.c; c++) {
        const cell = sheet[XLSX.utils.encode_cell({ r, c })];
        let value = cell?.v ?? '';
        if (cell?.f) error = 'La hoja contiene fórmulas. Guardá una copia pegando solo los valores antes de importarla.';
        if (cell?.t === 'e') error = 'La hoja contiene errores de Excel. Corregilos antes de importar.';
        if (cell?.t === 'n' && cell.z && XLSX.SSF.is_date(cell.z)) {
          const d = XLSX.SSF.parse_date_code(value, { date1904: Boolean(book.Workbook?.WBProps?.date1904) });
          value = d ? `${d.y}-${String(d.m).padStart(2, '0')}-${String(d.d).padStart(2, '0')}` : 'Fecha inválida';
        }
        row.push(String(value));
      }
      rows.push(row);
    }
    return [{ name, rows, error }];
  });
  if (!sheets.length) throw new Error('El libro no tiene hojas visibles con datos.');
  return sheets;
}

export function workbookTable(sheet, headerRow) {
  if (sheet.error) throw new Error(sheet.error);
  if (!Number.isInteger(headerRow) || headerRow < 1 || headerRow > sheet.rows.length) throw new Error('Indicá una fila de encabezados válida.');
  const table = sheet.rows.slice(headerRow - 1).map(row => [...row]);
  while (table.length > 1 && table.at(-1).every(v => !v.trim())) table.pop();
  if (table.length < 2) throw new Error('No hay participaciones debajo de los encabezados.');
  if (table[0].every(v => !v.trim())) throw new Error('La fila seleccionada no tiene encabezados.');
  // Keep physical row numbers for actionable preview messages.
  table.sourceRow = headerRow;
  table.sourceFormat = 'xlsx';
  return table;
}

export function writeHistoryWorkbook(records, XLSX) {
  const rows = [['ID', 'Fecha', 'Nombre', 'Participación', 'Tema'], ...records.map(r => [r.id, r.date, r.name, r.type, r.topic || ''])];
  // Strings are written as literal cells, including strings starting with '='.
  const sheet = XLSX.utils.aoa_to_sheet(rows);
  sheet['!cols'] = [{ wch: 48 }, { wch: 14 }, { wch: 30 }, { wch: 23 }, { wch: 48 }];
  sheet['!autofilter'] = { ref: sheet['!ref'] };
  const book = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(book, sheet, 'Historial');
  return XLSX.write(book, { type: 'array', bookType: 'xlsx', compression: true });
}
