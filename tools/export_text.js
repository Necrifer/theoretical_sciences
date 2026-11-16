// 1.15.1: turns tools/export_text.py's export.json into Word files for the text rewrite.
// Run: node tools/export_text.js <dir>
const fs = require('fs');
const path = require('path');
const { Document, Packer, Paragraph, TextRun, HeadingLevel, Footer, Header, AlignmentType, PageNumber, LevelFormat,
        BorderStyle } = require('docx');

const dir = process.argv[2];
const docs = JSON.parse(fs.readFileSync(path.join(dir, 'export.json'), 'utf8'));
const GREY = '8A8A8A', CODE = '2E7D8C', FONT = 'Arial';

// a piece of text with the Codex's $(...) codes shown in teal; $(br2) starts a new paragraph, $(br) a new line
function paragraphsOf(text) {
  const paras = [];
  for (const block of String(text).split('$(br2)')) {
    const runs = [];
    const lines = block.split('$(br)');
    lines.forEach((line, li) => {
      if (li > 0) runs.push(new TextRun({ text: '$(br)', color: CODE, font: FONT, size: 18, break: 1 }));
      for (const seg of line.split(/(\$\([^)]*\))/)) {
        if (!seg) continue;
        runs.push(/^\$\(/.test(seg) ? new TextRun({ text: seg, color: CODE, font: FONT, size: 18 })
                                    : new TextRun({ text: seg, font: FONT, size: 21 }));
      }
    });
    if (runs.length === 0) runs.push(new TextRun({ text: '', font: FONT, size: 21 }));
    paras.push(runs);
  }
  return paras.map((runs, i) => new Paragraph({ children: i === 0 ? runs : [new TextRun({ text: '$(br2) ', color: CODE, font: FONT, size: 18 }), ...runs],
                                                spacing: { after: 100, line: 276 } }));
}

function howTo() {
  const b = (t) => new Paragraph({ numbering: { reference: 'bullets', level: 0 }, spacing: { after: 60 },
                                   children: [new TextRun({ text: t, font: FONT, size: 20 })] });
  return [
    new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun('How to use this file')] }),
    b('Rewrite the black text freely. Leave the grey lines alone: each names the key the text below it comes from, so the rewritten file can be read straight back into the mod.'),
    b('Teal codes are the Codex\u2019s formatting: $(thing)\u2026$(), $(item)\u2026$() and $(fluid)\u2026$() colour a word (a concept, an item, a fluid) and $(bold)\u2026$() bolds it, each closed by $(); $(li) starts a bullet point; $(l:entry)\u2026$(/l) links to another entry; $(br) is a line break; $(br2) starts a new paragraph (shown here as a new paragraph that begins with it). Keep them where you want the formatting, or remove them.'),
    b('%s and %d in a line are numbers or names the game fills in; keep them in.'),
    b('You can add or remove paragraphs within a piece of text; a new paragraph is read back as $(br2).'),
  ];
}

function build(d) {
  const children = [new Paragraph({ heading: HeadingLevel.TITLE, children: [new TextRun(d.title)] })];
  for (const t of d.intro || []) children.push(new Paragraph({ spacing: { after: 120 }, children: [new TextRun({ text: t, font: FONT, size: 21 })] }));
  if (d.tab && d.tab.text) {
    children.push(new Paragraph({ spacing: { before: 120 }, children: [new TextRun({ text: 'Tab description · ' + d.tab.key, color: GREY, font: FONT, size: 16 })] }));
    children.push(...paragraphsOf(d.tab.text));
  }
  children.push(...howTo());
  for (const s of d.sections) {
    children.push(new Paragraph({ heading: HeadingLevel.HEADING_1, pageBreakBefore: true, children: [new TextRun(s.heading)] }));
    for (const e of s.entries) {
      children.push(new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(e.name)] }));
      children.push(new Paragraph({ spacing: { after: 60 }, children: [new TextRun({ text: e.id, color: GREY, font: FONT, size: 16 })] }));
      for (const p of e.parts) {
        children.push(new Paragraph({ spacing: { before: 80, after: 20 }, keepNext: true,
          children: [new TextRun({ text: p.label, color: GREY, font: FONT, size: 16, bold: true }),
                     new TextRun({ text: '  ·  ' + p.key, color: GREY, font: FONT, size: 16 })] }));
        children.push(...paragraphsOf(p.text));
      }
    }
  }
  return new Document({
    creator: 'Theoretical Sciences', title: d.title,
    styles: {
      default: { document: { run: { font: FONT, size: 21 } } },
      paragraphStyles: [
        { id: 'Title', name: 'Title', basedOn: 'Normal', run: { font: FONT, size: 40, bold: true }, paragraph: { spacing: { before: 0, after: 200 }, keepNext: true } },
        { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true, run: { font: FONT, size: 32, bold: true },
          paragraph: { spacing: { before: 240, after: 160 }, keepNext: true, outlineLevel: 0 } },
        { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true, run: { font: FONT, size: 24, bold: true },
          paragraph: { spacing: { before: 280, after: 20 }, keepNext: true, outlineLevel: 1,
                       border: { top: { style: BorderStyle.SINGLE, size: 4, color: 'D0D0D0', space: 6 } } } },
      ],
    },
    numbering: { config: [{ reference: 'bullets', levels: [{ level: 0, format: LevelFormat.BULLET, text: '•', alignment: AlignmentType.LEFT,
                                                              style: { paragraph: { indent: { left: 540, hanging: 270 } } } }] }] },
    sections: [{
      properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1300, bottom: 1300, left: 1300, right: 1300 } } },
      headers: { default: new Header({ children: [new Paragraph({ alignment: AlignmentType.RIGHT,
        children: [new TextRun({ text: d.title, color: GREY, font: FONT, size: 16 })] })] }) },
      footers: { default: new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER,
        children: [new TextRun({ children: [PageNumber.CURRENT], color: GREY, font: FONT, size: 16 })] })] }) },
      children,
    }],
  });
}

(async () => {
  for (const d of docs) {
    const buf = await Packer.toBuffer(build(d));
    fs.writeFileSync(path.join(dir, d.file), buf);
    console.log('wrote', d.file, (buf.length / 1024).toFixed(0) + ' KB');
  }
})();
