// 1.1.0: builds the text-rewrite document from tools/text/out/payload.json (made by textdoc.py export).
// node tools/text/build_docx.js <out.docx>
const fs = require('fs');
const path = require('path');
const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, Table, TableRow, TableCell, WidthType, ShadingType, BorderStyle,
  LevelFormat, AlignmentType, Footer, PageNumber, TableOfContents, PageBreak,
} = require('docx');

const P = JSON.parse(fs.readFileSync(path.join(__dirname, 'out', 'payload.json'), 'utf8'));
const OUT = process.argv[2] || path.join(__dirname, 'out', 'Theoretical Sciences text.docx');

const W = 9638;                     // A4 with 2 cm margins
const GREY = '8A8A8A';
const FONT = 'Calibri';
const STYLE_RUN = {                 // how each Codex style looks (and is read back: bold / italic / underline)
  item: { bold: true, color: '1F4E79' },
  thing: { italics: true, color: '6B3FA0' },
  fluid: { bold: true, italics: true, color: '0E7C7B' },
  head: { underline: {}, color: '000000' },
  strong: { bold: true, underline: {} },
};

const marker = (id, info) => new Paragraph({
  style: 'TSMarker',
  children: [new TextRun({ text: `⟦${id}⟧`, font: 'Consolas' }), ...(info ? [new TextRun({ text: '  ' + info })] : [])],
});

function body(paras) {
  const out = [];
  for (const p of paras) {
    const kids = [];
    for (const r of p.runs) {
      if (r.br) kids.push(new TextRun({ break: 1 }));
      else kids.push(new TextRun({ text: r.t, ...(r.s ? STYLE_RUN[r.s] : {}) }));
    }
    out.push(new Paragraph(p.li ? { children: kids, numbering: { reference: 'li', level: 0 } } : { children: kids }));
  }
  if (!out.length) out.push(new Paragraph({ children: [] }));
  return out;
}

const border = { style: BorderStyle.SINGLE, size: 4, color: 'D0D0D0' };
const borders = { top: border, bottom: border, left: border, right: border };

function cell(text, width, opts = {}) {
  const lines = String(text ?? '').split('\n');
  const kids = [];
  lines.forEach((l, i) => { if (i) kids.push(new TextRun({ break: 1 })); kids.push(new TextRun({ text: l, ...(opts.run || {}) })); });
  return new TableCell({
    borders, width: { size: width, type: WidthType.DXA },
    shading: opts.fill ? { fill: opts.fill, type: ShadingType.CLEAR, color: 'auto' } : undefined,
    margins: { top: 40, bottom: 40, left: 80, right: 80 },
    children: [new Paragraph({ style: opts.style, children: kids })],
  });
}

function table(cols, header, rows) {
  const head = new TableRow({ tableHeader: true, children: header.map((h, i) => cell(h, cols[i], { fill: 'E8E8F0', run: { bold: true } })) });
  return new Table({
    width: { size: W, type: WidthType.DXA }, columnWidths: cols,
    rows: [head, ...rows.map(r => new TableRow({
      children: r.map((v, i) => cell(v, cols[i], i === 0 ? { style: 'TSId' } : {})),
    }))],
  });
}

const kids = [];
const H = (lvl, text, pageBreak) => new Paragraph({ heading: lvl, pageBreakBefore: !!pageBreak, children: [new TextRun(text)] });
const para = (text, run) => new Paragraph({ children: [new TextRun({ text, ...(run || {}) })] });
const bullet = (runs) => new Paragraph({ numbering: { reference: 'li', level: 0 }, children: runs });

// ---------------------------------------------------------------- cover and how to edit
const nPages = P.codex.reduce((a, c) => a + c.entries.reduce((b, e) => b + e.pages.length, 0), 0);
const nEntries = P.codex.reduce((a, c) => a + c.entries.length, 0);
const nLang = P.items.length + P.lang.reduce((a, s) => a + s.rows.length, 0);
kids.push(new Paragraph({ style: 'Title', children: [new TextRun('Theoretical Sciences — all in-game text')] }));
kids.push(para(`Version 1.1.0 (second pass, with your first edits in) · exported 3 November 2026 · ${nEntries} Codex entries (${nPages} pages) and ${nLang} lines of item, tooltip and interface text.`, { color: GREY }));
kids.push(H(HeadingLevel.HEADING_2, 'How to edit this document'));
kids.push(para('Edit any text freely. When you send the document back, every piece you changed goes into the mod, and everything you left alone stays exactly as it is. A few things keep that working:'));
kids.push(bullet([new TextRun({ text: 'Leave the grey ID lines and the grey ID column alone. ', bold: true }), new TextRun('Lines like '), new TextRun({ text: '⟦codex:basics/guide_03_power:2⟧', font: 'Consolas', color: GREY }), new TextRun(' tell me where each piece of text belongs. The note after the ID (the page kind, which item it shows) is only for you.')]));
kids.push(bullet([new TextRun('Codex entry titles are the medium headings, page titles the small headings under an ID line. Both can be edited.')]));
kids.push(bullet([new TextRun('In Codex text, formatting means something: '), new TextRun({ text: 'bold', ...STYLE_RUN.item }), new TextRun(' marks an item or block, '), new TextRun({ text: 'italic', ...STYLE_RUN.thing }), new TextRun(' a concept or key word, '), new TextRun({ text: 'bold italic', ...STYLE_RUN.fluid }), new TextRun(' a fluid, and '), new TextRun({ text: 'underlined', ...STYLE_RUN.head }), new TextRun(' a small sub-heading. The colours are only a reading aid; bold, italic and underline are what count.')]));
kids.push(bullet([new TextRun('A new paragraph (Enter) is a blank line in the Codex; Shift+Enter is a plain line break; a bulleted paragraph is a list item. Codex pages are small, so very long pages may be split when they go in.')]));
kids.push(bullet([new TextRun('In the tables, keep placeholders such as '), new TextRun({ text: '%s', font: 'Consolas' }), new TextRun(' or '), new TextRun({ text: '%1$s', font: 'Consolas' }), new TextRun(' (the game fills them in with numbers or names) and keep them in the same order unless you number them.')]));
kids.push(bullet([new TextRun('Lines such as '), new TextRun({ text: 'Made', ...STYLE_RUN.thing }), new TextRun(', '), new TextRun({ text: 'Used in', ...STYLE_RUN.thing }), new TextRun(', '), new TextRun({ text: 'Unlocked by', ...STYLE_RUN.thing }), new TextRun(', '), new TextRun({ text: 'Insight', ...STYLE_RUN.thing }), new TextRun(', '), new TextRun({ text: 'Drops', ...STYLE_RUN.thing }), new TextRun(' and '), new TextRun({ text: 'Found', ...STYLE_RUN.thing }), new TextRun(' (and the ore descriptions) are written by the game from its recipes, research and world generation. You can reword them, but a reworded page stops following later recipe changes, so it is usually better to leave those lines and rewrite the prose around them.')]));
kids.push(bullet([new TextRun('To ask for something you cannot write in place (move a page, add or remove an entry, change which item a page shows), leave a Word comment. Tracked changes are fine; I take the final text.')]));
kids.push(bullet([new TextRun('Text in this document comes straight from the game files. Names in “shows …” notes and in the item table are the item names as they are now: if you rename an item in the table, its name changes everywhere the game shows it, but Codex text that mentions it by name does not change by itself.')]));
kids.push(H(HeadingLevel.HEADING_2, 'Parts'));
kids.push(bullet([new TextRun({ text: 'Part 1, the Codex: ', bold: true }), new TextRun('the tabs and chapters, then every entry, chapter by chapter, starting with Getting Started.')]));
kids.push(bullet([new TextRun({ text: 'Part 2, items and tooltips: ', bold: true }), new TextRun('every item and block with its name and description, then research, tooltip hints and the machine consoles\' hover text.')]));
kids.push(bullet([new TextRun({ text: 'Part 3, other in-game text: ', bold: true }), new TextRun('names of fluids, creatures and places, interface and chat messages, and sound subtitles.')]));
kids.push(new Paragraph({ children: [new PageBreak()] }));
kids.push(new TableOfContents('Contents', { hyperlink: true, headingStyleRange: '1-1' }));

// ---------------------------------------------------------------- Part 1: the Codex
kids.push(H(HeadingLevel.HEADING_1, 'Part 1 · Codex tabs and chapters', true));
kids.push(para('The tabs across the top of the Codex, and the chapter names its entries are filed under.'));
kids.push(table([2400, 2400, W - 4800], ['ID', 'Tab name', 'Tab description'], P.tabs.map(t => [t.id, t.name, t.description])));
kids.push(para(''));
kids.push(table([2400, W - 2400], ['ID', 'Chapter name'], P.chapters.map(c => [c.id, c.name])));

for (const ch of P.codex) {
  kids.push(H(HeadingLevel.HEADING_1, `Codex · ${ch.name}`, true));
  for (const e of ch.entries) {
    kids.push(H(HeadingLevel.HEADING_2, e.title));
    kids.push(marker('entry:' + e.id));
    for (const pg of e.pages) {
      kids.push(marker(`codex:${e.id}:${pg.n}`, `page ${pg.n} · ${pg.info}`));
      if (pg.title != null) kids.push(H(HeadingLevel.HEADING_3, pg.title));
      kids.push(...body(pg.paras));
    }
  }
}

// ---------------------------------------------------------------- Part 2 and 3: lang
kids.push(H(HeadingLevel.HEADING_1, 'Part 2 · Items and blocks', true));
kids.push(para('Each item\'s and block\'s name, and the description shown in its tooltip.'));
kids.push(table([2500, 2300, W - 4800], ['ID', 'Name', 'Description'], P.items.map(r => [r.id, r.name ?? '', r.lore ?? ''])));

for (const sec of P.lang) {
  const part = ['Other names', 'Interface, messages and puzzles', 'Sound subtitles'].includes(sec.title) ? 'Part 3' : 'Part 2';
  kids.push(H(HeadingLevel.HEADING_1, `${part} · ${sec.title}`, true));
  kids.push(para(sec.intro));
  if (sec.title === 'Machine consoles') {
    const groups = {};
    for (const r of sec.rows) (groups[r.group] = groups[r.group] || []).push(r);
    for (const g of Object.keys(groups)) {
      const t = groups[g].find(r => r.id.endsWith('.title'));
      kids.push(H(HeadingLevel.HEADING_3, t ? t.text : g));
      kids.push(table([3600, W - 3600], ['ID', 'Text'], groups[g].map(r => [r.id, r.text])));
    }
  } else {
    kids.push(table([3600, W - 3600], ['ID', 'Text'], sec.rows.map(r => [r.id, r.text])));
  }
}

const doc = new Document({
  creator: 'Theoretical Sciences', title: 'Theoretical Sciences — all in-game text',
  features: { updateFields: true },
  styles: {
    default: { document: { run: { font: FONT, size: 21 } } },
    paragraphStyles: [
      { id: 'Title', name: 'Title', basedOn: 'Normal', run: { size: 44, bold: true, font: FONT }, paragraph: { spacing: { after: 160 } } },
      { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true, run: { size: 34, bold: true, font: FONT, color: '1F3864' }, paragraph: { spacing: { before: 120, after: 200 }, outlineLevel: 0 } },
      { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true, run: { size: 26, bold: true, font: FONT, color: '2E5597' }, paragraph: { spacing: { before: 320, after: 40 }, keepNext: true, outlineLevel: 1 } },
      { id: 'Heading3', name: 'Heading 3', basedOn: 'Normal', next: 'Normal', quickFormat: true, run: { size: 22, bold: true, font: FONT, color: '404040' }, paragraph: { spacing: { before: 60, after: 40 }, keepNext: true, outlineLevel: 2 } },
      { id: 'TSMarker', name: 'TS ID line', basedOn: 'Normal', next: 'Normal', run: { size: 14, color: GREY }, paragraph: { spacing: { before: 140, after: 20 }, keepNext: true } },
      { id: 'TSId', name: 'TS ID cell', basedOn: 'Normal', run: { size: 14, color: GREY, font: 'Consolas' } },
    ],
  },
  numbering: { config: [{ reference: 'li', levels: [{ level: 0, format: LevelFormat.BULLET, text: '•', alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 540, hanging: 270 } } } }] }] },
  sections: [{
    properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1134, bottom: 1134, left: 1134, right: 1134 } } },
    footers: { default: new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ children: [PageNumber.CURRENT], color: GREY, size: 16 })] })] }) },
    children: kids,
  }],
});
Packer.toBuffer(doc).then(b => { fs.writeFileSync(OUT, b); console.log('wrote', OUT, (b.length / 1048576).toFixed(1) + ' MB'); });
