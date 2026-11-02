const fs = require("node:fs");
const assert = require("node:assert/strict");
const { chromium } = require("playwright");
const app = fs.readFileSync("docs/app.js", "utf8");
const functions = ["openBulkPrint", "openModal", "closeModal", "printAgendaBatch", "pdfHtml", "pdfTestimonyPage",
  "pdfField", "pdfListSection", "pdfBusinessSection", "pdfMessagesSection", "pdfEmptyLines", "pdfDensityClass",
  "splitAnnouncements", "splitCommaItems", "escapeHtml", "escapeAttr", "toDate", "dateInputValue",
  "formatDateShort", "formatAgendaDateCompact"];

(async () => {
  const browser = await chromium.launch({ channel: "chrome" });
  try {
    const english = process.env.QA_LOCALE === "en";
    const page = await browser.newPage({ locale: english ? "en-US" : "es-UY", viewport: { width: 390, height: 844 } });
    await page.setContent('<main>Agendas</main><dialog id="modal"></dialog>');
    await page.addStyleTag({ content: fs.readFileSync("docs/styles.css", "utf8").replace(/^@import[^\n]+/, "") });
    await page.addScriptTag({ content: app.slice(app.indexOf("const MEETING_TEXT"), app.indexOf("const UNIT_STORAGE_KEY")) +
      functions.map(name => {
        const start = app.indexOf(`function ${name}(`);
        return app.slice(start, app.indexOf("\n}", start) + 2);
      }).join("\n") });
    await page.evaluate(() => {
      window.hymnLabel = window.messageText = () => "";
      window.businessBlocksInOrder = () => [];
      window.print = () => {};
      window.state = { agendas: [22, 8, 15, 1].map(day => ({
        id: String(day), fecha: new Date(2026, 10, day), reunionTestimonios: day === 1,
        testimonios: ["Ana", "Luis"], anuncios: day === 1 ? "Anuncio largo. ".repeat(400) : ""
      })) };
      openBulkPrint();
    });
    await page.getByText(english ? "Select by date range" : "Seleccionar por fechas").click();
    await page.locator('[name="from"]').fill("2026-11-01");
    await page.locator('[name="until"]').fill("2026-11-15");
    await page.getByText(english ? "Select range" : "Seleccionar rango").click();
    await page.locator('[value="8"]').uncheck();
    await page.getByText(english ? "Continue" : "Continuar").click();
    for (const [label, count] of [[english ? "Regular Sundays" : "Domingos regulares", 1], [english ? "Fast and testimony" : "Ayuno y testimonio", 2], [english ? "All agendas" : "Todas las agendas", 3]]) {
      await page.getByRole("button", { name: label, exact: true }).click();
      assert.equal(await page.locator(".bulk-sheet").count(), count);
      if (count > 1) assert.match(await page.locator(".bulk-sheet").nth(1).textContent(), /Luis/);
      const pdf = await page.pdf({ preferCSSPageSize: true });
      assert.equal((pdf.toString("latin1").match(/\/Type\s*\/Page\b/g) || []).length, count);
    }
    await page.getByText(english ? "Back to selection" : "Volver a la selección").click();
    await page.locator('[value="15"]').uncheck();
    await page.getByRole("button", { name: english ? "Print" : "Imprimir", exact: true }).click();
    assert.equal(await page.locator(".bulk-sheet").count(), 2);
    console.log("Bulk print: inclusive range, manual edits, both batches, mixed PDF and single-type shortcut passed.");
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
