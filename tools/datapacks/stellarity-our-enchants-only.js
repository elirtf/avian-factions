#!/usr/bin/env node
// Our-enchantments-only Stellarity (owner, 2026-10-01: "keep our stuff"; Stellarity's licence allows
// modifying it for our own server, never redistributing it). Run inside an unpacked Stellarity 6.0.0:
//   - its 8 enchantments leave the vanilla treasure/non_treasure tags, so enchanting tables, villagers
//     and random loot never offer them;
//   - every random enchant roll in its loot tables drops them (a roll left with none takes any vanilla
//     enchantment), in the old ("function") and new ("type") loot formats and every overlay.
// Its own items keep their built-in enchantments (Call of the Void's Void Shot is how that weapon works).
const fs = require('fs'), path = require('path');
const ours = /^stellarity:(ambush|critical_strike|dune_speed|levitation_shot|plated|soaring|void_shot|void_strike)$/;
let tags = 0, rolls = 0;
for (const t of ['treasure', 'non_treasure']) {
  for (const root of ['data', ...fs.readdirSync('.').filter(d => d.startsWith('overlay_'))]) {
    const f = path.join(root, 'minecraft/tags/enchantment', t + '.json');
    if (!fs.existsSync(f)) continue;
    const j = JSON.parse(fs.readFileSync(f, 'utf8'));
    const kept = j.values.filter(v => !ours.test(typeof v === 'string' ? v : v.id));
    tags += j.values.length - kept.length; j.values = kept;
    fs.writeFileSync(f, JSON.stringify(j, null, 2));
  }
}
function walk(node) {
  if (Array.isArray(node)) return node.forEach(walk);
  if (node && typeof node === 'object') {
    const fn = String(node.function || node.type || "").replace(/^minecraft:/, "");   // "function" (loot tables), "type" (newer format)
    if (fn === "enchant_randomly" && Array.isArray(node.options) && node.options.some(o => ours.test(o))) {
      // Keep any vanilla options; with none left, any vanilla enchantment can roll.
      const kept = node.options.filter(o => !ours.test(o));
      if (kept.length) node.options = kept; else delete node.options;
      rolls++;
    }
    Object.values(node).forEach(walk);
  }
}
const files = [];
(function find(d) { for (const e of fs.readdirSync(d, { withFileTypes: true })) { const p = path.join(d, e.name);
  if (e.isDirectory()) find(p); else if (/\/loot_table\/.*\.json$/.test(p)) files.push(p); } })('.');
for (const f of files) {
  const before = fs.readFileSync(f, 'utf8');
  if (!before.includes('enchant_randomly')) continue;
  const j = JSON.parse(before); const n = rolls; walk(j);
  if (rolls !== n) fs.writeFileSync(f, JSON.stringify(j, null, 2));
}
console.log(`tags: removed ${tags} entries; loot: ${rolls} Stellarity-only enchant rolls now roll vanilla`);
