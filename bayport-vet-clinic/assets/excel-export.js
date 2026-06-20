/**
 * Export tabular data as a formatted Excel-compatible .xls (HTML) file.
 * Opens in Excel with bold headers, column widths, currency, and date hints.
 */
window.exportFormattedExcel = function exportFormattedExcel(filename, sheets) {
  const esc = (v) =>
    String(v ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;");

  const isMoneyCol = (header) => /amount|total|price|revenue|paid|pending|sales|cost|₱/i.test(String(header || ""));
  const isDateCol = (header) => /date|period|from|to|at|time/i.test(String(header || ""));

  let body = "";
  (sheets || []).forEach((sheet, idx) => {
    const title = sheet.title || `Sheet ${idx + 1}`;
    const headers = sheet.headers || [];
    const rows = sheet.rows || [];
    const colWidths = headers.map((h, i) => {
      let max = String(h || "").length;
      rows.forEach((row) => {
        const len = String((row || [])[i] ?? "").length;
        if (len > max) max = len;
      });
      return Math.min(Math.max(max + 2, 10), 48);
    });

    body += `<h2 style="font-family:Calibri,Arial;font-size:14pt;color:#0057b8;margin:16px 0 8px;">${esc(title)}</h2>`;
    body += "<table border='1' cellpadding='4' cellspacing='0' style='border-collapse:collapse;font-family:Calibri,Arial;font-size:11pt;'>";
    if (headers.length) {
      body += "<tr>";
      headers.forEach((h, i) => {
        const w = colWidths[i] ? ` width:${colWidths[i] * 7}px;` : "";
        body += `<th style="background:#0057b8;color:#fff;font-weight:bold;text-align:left;${w}">${esc(h)}</th>`;
      });
      body += "</tr>";
    }
    rows.forEach((row) => {
      body += "<tr>";
      (row || []).forEach((cell, i) => {
        const h = headers[i] || "";
        let style = "vertical-align:top;";
        if (isMoneyCol(h)) {
          style += "mso-number-format:'\\0022₱\\0022\\#\\,\\#\\#0.00';text-align:right;";
        } else if (isDateCol(h)) {
          style += "mso-number-format:'Short Date';";
        }
        body += `<td style="${style}">${esc(cell)}</td>`;
      });
      body += "</tr>";
    });
    body += "</table><br/>";
  });

  const html = `<!DOCTYPE html>
<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel">
<head><meta charset="UTF-8"/>
<!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet>
<x:Name>Report</x:Name></x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]-->
<style>td,th{font-family:Calibri,Arial;}</style></head>
<body>${body}</body></html>`;

  const blob = new Blob(["\ufeff", html], { type: "application/vnd.ms-excel;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = (filename || "export.xls").replace(/\.xlsx?$/i, "") + ".xls";
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 500);
};
