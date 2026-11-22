#!/usr/bin/env node
// 打印 .remap-work/m4b3-classify.json 里失败文件的首条错误上下文（±6 行），便于人工判读。
'use strict';
const fs = require('fs');
const path = require('path');
const ROOT = path.resolve(__dirname, '..');
const j = JSON.parse(fs.readFileSync(path.join(ROOT, '.remap-work/m4b3-classify.json'), 'utf8'));
const filter = process.argv[2] || '';
for (const r of j.rows) {
  if (r.ok) continue;
  if (filter && !r.file.includes(filter)) continue;
  const abs = path.join(ROOT, r.file);
  const lines = fs.readFileSync(abs, 'utf8').split('\n').map(l => l.replace(/\r$/, ''));
  console.log(`\n===== ${r.file}  [${r.cat}] ${r.nErr} errs`);
  const seen = new Set();
  for (const el of r.errorLines) {
    const m = el.match(/^L(\d+): (.*)$/);
    const ln = +m[1];
    if (seen.has(ln)) continue;
    seen.add(ln);
    console.log(`--- L${ln}: ${m[2]}`);
    for (let i = Math.max(0, ln - 7); i < Math.min(lines.length, ln + 4); i++) {
      console.log(`${i + 1 === ln ? '>>' : '  '}${String(i + 1).padStart(5)} ${lines[i]}`);
    }
  }
}
