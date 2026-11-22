// 语言键清理（M3）：删掉 A 侧（灯光 / 光束 / 散射 / 碰撞 / 水雾 / 连接器 / 优化配置界面）的键。
// 「在用」判定 = 键名在剩余源码里字面出现，或者「≥3 段的父前缀」（拼接写法，如
// "screen.mtrlte.clock.glow." + mode）在源码里出现。
const fs = require('fs');
const path = require('path');

const ROOT = 'D:/MC-MTR-JS/mtr-headlight';
const LANG = ROOT + '/v1.20.1/src/main/resources/assets/mtrlte/lang';

function walk(dir, out = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, out);
    else out.push(p);
  }
  return out;
}
let text = '';
for (const base of [ROOT + '/v1.20.1/src', ROOT + '/common/src']) {
  for (const f of walk(base)) {
    if (f.replace(/\\/g, '/').includes('/lang/') && f.endsWith('.json')) continue;
    try { text += fs.readFileSync(f, 'utf8'); } catch (e) { /* binary */ }
  }
}

const A_PREFIX = ['beam3d.', 'beam.', 'sidings.', 'cat.', 'detail.', 'death.', 'key.', 'tip.',
  'opt.beam', 'opt.collision', 'opt.damage', 'opt.dynamicLight', 'opt.flare', 'opt.rainHush',
  'opt.rainSpray', 'opt.scatter', 'opt.trackbed', 'opt.trainCollision', 'opt.trainDamage',
  'opt.trainMobs', 'options.particles.mtrlte.spray'];
const A_KEYWORDS = ['light', 'beam', 'collision', 'spray', 'rain', 'trackbed', 'scatter',
  'flare', 'headlight', 'glow', 'volume', 'cone', 'shadow', 'lamp'];
const A_EXTRA = new Set([
  'item.mtrlte.signal_linker', 'item.mtrlte.projector_connector', 'item.mtrlte.pids_connector',
  'item.mtrlte.lte_dashboard', 'item.mtrlte.beam_editor',
  'screen.mtrlte.config', 'btn.global', 'btn.sidingMenu', 'btn.reloadAll', 'btn.reloadTrains',
  'btn.backBeams', 'btn.beamAdd', 'btn.beamDelete', 'btn.beamDiscard', 'btn.beamEditor',
  'btn.beamExport', 'btn.beamResetBeam', 'btn.toBeam', 'btn.toBeamPage',
  'map.title', 'map.needWorld', 'screen.mtrlte.param', 'tip.needWorld'
]);

const zh = JSON.parse(fs.readFileSync(LANG + '/zh_cn.json', 'utf8'));
const en = JSON.parse(fs.readFileSync(LANG + '/en_us.json', 'utf8'));

/** 键名或它的「≥3 段父前缀」在源码里出现过 = 还在用。 */
function inUse(k) {
  if (text.includes(k)) return true;
  for (let i = k.indexOf('.'); i !== -1; i = k.indexOf('.', i + 1)) {
    const prefix = k.slice(0, i + 1);
    const dots = (prefix.match(/\./g) || []).length;
    if (dots >= 2 && text.includes(prefix)) return true;
  }
  return false;
}

const isA = (k) => A_EXTRA.has(k)
  || A_PREFIX.some((p) => k.startsWith(p))
  || A_KEYWORDS.some((w) => k.toLowerCase().includes(w));

const del = [];
for (const k of Object.keys(zh)) if (isA(k) && !inUse(k)) del.push(k);
const delSet = new Set(del);

const prune = (o) => Object.fromEntries(Object.keys(o).filter((k) => !delSet.has(k)).map((k) => [k, o[k]]));
const sortObj = (o) => Object.fromEntries(Object.keys(o).sort().map((k) => [k, o[k]]));

fs.writeFileSync(LANG + '/zh_cn.json', JSON.stringify(sortObj(prune(zh)), null, 2) + '\n', 'utf8');
fs.writeFileSync(LANG + '/en_us.json', JSON.stringify(sortObj(prune(en)), null, 2) + '\n', 'utf8');
fs.writeFileSync(ROOT + '/.deleted-lang-keys.txt', del.join('\n'), 'utf8');

console.log('deleted', del.length, '| zh', Object.keys(prune(zh)).length, '| en', Object.keys(prune(en)).length);
console.log(del.slice(0, 12).join(' '), '...');
