#!/usr/bin/env node
// Generates Avian's ExcellentEnchants setup from the plugin's own default files (docs/ENCHANTS.md):
//   - dev-server/plugins/ExcellentEnchants/enchants/<name>.yml: every enabled enchant, its name coloured
//     by tier, level caps applied, Legendary ones kept out of enchanting tables (Treasure: true);
//   - dev-server/plugins/ExcellentEnchants/enchants/_disabled_/<name>.yml: the disabled ones;
//   - dev-server/plugins/CrazyCrates/crates/Enchant{Common,Rare,Legendary}.yml: the Enchanter's book
//     rolls, one prize per enchant level (higher levels rarer).
//
// Usage: node tools/enchant-tiers.mjs <defaults-dir>
//   <defaults-dir> = plugins/ExcellentEnchants/enchants from a server that booted ExcellentEnchants once
//   with no config of ours (a scratch server). Re-run after changing the tables below, and commit the output.
import fs from 'node:fs';
import path from 'node:path';

const defaults = process.argv[2];
if (!defaults) { console.error('usage: enchant-tiers.mjs <defaults-dir>'); process.exit(2); }
const repo = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');
const out = path.join(repo, 'dev-server/plugins/ExcellentEnchants/enchants');
const crates = path.join(repo, 'dev-server/plugins/CrazyCrates/crates');

// Off entirely, and why.
const DISABLED = {
  silk_spawner: 'mines spawners, skipping the /shop spawner economy',
  silk_chest: 'picks up whole chests with their contents: a raid shortcut',
  thrifty: 'drops spawn eggs, which change a spawner\'s type',
  ender_bow: 'shoots ender pearls, skipping the pearl cooldown',
  bomber: 'free TNT: raiding without paying for it',
  ghast: 'free fireballs: raiding and griefing',
  explosive_arrows: 'free explosions: raiding and griefing',
  kamikadze: 'explodes on death: griefing',
  blast_mining: 'mines by explosion: griefing near claims',
  rocket: 'launches players into the sky: fall-damage kills',
  soulbound: 'keeps items on death: PvP loot has to drop',
  curse_of_death: 'kills the killer: no place in factions PvP',
  // Owner, 2026-09-29: curses and these don't earn their place.
  curse_of_breaking: 'curses removed (owner)',
  curse_of_drowned: 'curses removed (owner)',
  curse_of_fragility: 'curses removed (owner)',
  curse_of_mediocrity: 'curses removed (owner)',
  curse_of_misfortune: 'curses removed (owner)',
  cutter: 'removed (owner)',
  fire_shield: 'removed (owner)',
  flare: 'removed (owner)',
  hover: 'removed (owner)',
  nimble: 'removed (owner)',
  telekinesis: 'removed (owner)',
};

// Tiers. Colours match the crates (docs/ECONOMY.md): Common #C9C9C9, Rare #4DA3FF, Legendary #FFB84D,
// Mythic #FF4F7A.
const TIERS = {
  Common: {
    colour: '#C9C9C9',
    enchants: ['glassbreaker', 'lightweight', 'lucky_miner', 'smelter', 'haste', 'replanter', 'river_master',
      'seasoned_angler', 'double_catch', 'sniper', 'jumping', 'saturation', 'night_vision', 'water_breathing',
      'wisdom', 'village_defender', 'bane_of_netherspawn', 'cure', 'survivalist', 'lingering'],
  },
  Rare: {
    colour: '#4DA3FF',
    enchants: ['venom', 'blindness', 'confusion', 'exhaust', 'cold_steel', 'hardened', 'poisoned_arrows',
      'withered_arrows', 'darkness_arrows', 'confusing_arrows', 'electrified_arrows', 'vampiric_arrows',
      'veinminer', 'treefeller', 'speed', 'restore', 'elemental_protection', 'decapitator', 'swiper', 'rage',
      'wither', 'infernus'],
  },
  Legendary: {
    colour: '#FFB84D',
    treasure: true,   // not from enchanting tables; loot, fishing, trades and the Enchanter only
    enchants: ['vampire', 'double_strike', 'temper', 'thunder', 'paralyze', 'dragon_heart', 'regrowth',
      'darkness_cloak', 'tunnel', 'flame_walker', 'rebound', 'dragonfire_arrows'],
  },
  // Owner, 2026-09-29: these four are too strong or too good for their old tier.
  Mythic: {
    colour: '#FF4F7A',
    treasure: true,
    enchants: ['auto_reel', 'ice_aspect', 'ice_shield', 'stopping_force'],
  },
};
// Curses were kept as shipped until the owner removed them (2026-09-29); none left.
const CURSES = [];

// One plain line per enchant for the Enchanter's prize previews (owner, 2026-09-29: "say what the
// enchantment does simply"). Effects checked against ExcellentEnchants' source.
const SIMPLE = {
  glassbreaker: 'Breaks glass instantly.',
  lightweight: 'Walk on crops and turtle eggs without breaking them.',
  lucky_miner: 'Chance for extra XP from ores.',
  smelter: 'Chance to smelt what you mine.',
  haste: 'Mine faster.',
  replanter: 'Replants crops for you.',
  river_master: 'Cast your line farther.',
  seasoned_angler: 'More XP from fishing.',
  double_catch: 'Chance to double your catch.',
  sniper: 'Arrows fly faster.',
  jumping: 'Jump higher.',
  saturation: 'Slowly refills your hunger.',
  night_vision: 'See in the dark.',
  water_breathing: 'Breathe underwater.',
  wisdom: 'Mobs drop more XP.',
  village_defender: 'Extra damage to pillagers.',
  bane_of_netherspawn: 'Extra damage to Nether mobs.',
  cure: 'Chance to cure zombie villagers and piglins.',
  survivalist: 'Cooks the fish you catch.',
  lingering: 'Tipped arrows leave a lingering cloud.',
  venom: 'Chance to poison your target.',
  blindness: 'Chance to blind your target.',
  confusion: 'Chance to make your target dizzy.',
  exhaust: 'Chance to drain your target\'s hunger.',
  cold_steel: 'Chance to slow your attacker\'s swings.',
  hardened: 'Chance to gain Resistance when hit.',
  poisoned_arrows: 'Chance for arrows to poison.',
  withered_arrows: 'Chance for arrows to wither.',
  darkness_arrows: 'Chance for arrows to bring darkness.',
  confusing_arrows: 'Chance for arrows to make targets dizzy.',
  electrified_arrows: 'Chance for arrows to call lightning.',
  vampiric_arrows: 'Chance to heal when your arrow hits.',
  veinminer: 'Mines a whole ore vein at once.',
  treefeller: 'Cuts down a whole tree at once.',
  speed: 'Move faster.',
  restore: 'Chance to save your item from breaking.',
  elemental_protection: 'Less damage from potions and the elements.',
  decapitator: 'Chance to drop your victim\'s head.',
  swiper: 'Chance to steal XP from players.',
  rage: 'Chance to gain Strength when you hit.',
  wither: 'Chance to wither your target.',
  infernus: 'Thrown tridents set targets on fire.',
  vampire: 'Chance to heal when you hit.',
  double_strike: 'Chance to deal double damage.',
  temper: 'Hit harder the lower your health.',
  thunder: 'Chance to strike your target with lightning.',
  paralyze: 'Chance to slow your target\'s swings.',
  dragon_heart: 'Extra hearts.',
  regrowth: 'Slowly heals you.',
  darkness_cloak: 'Chance to bring darkness on your attacker.',
  tunnel: 'Mines several blocks at once.',
  flame_walker: 'Walk on lava; no magma damage.',
  rebound: 'Bounce when you land.',
  dragonfire_arrows: 'Chance for arrows to leave dragon fire.',
  auto_reel: 'Reels in your catch automatically.',
  ice_aspect: 'Freezes and slows your target.',
  ice_shield: 'Chance to freeze and slow your attacker.',
  stopping_force: 'Chance to take less knockback.',
};

// Trigger chance overrides (percent at level I, added per level).
const TRIGGER_CHANCE = { stopping_force: { base: 25, perLevel: 10 } };   // was 100 % on every hit

// Max level overrides: strong permanent effects.
const MAX_LEVEL = { dragon_heart: 2, speed: 1 };

// Players see "Chance to …", never the percentage (owner, 2026-09-29): the numbers stay tunable
// without the descriptions promising them. Only the Description lines change.
function hideChances(e, text) {
  const chance = '%enchantment_trigger_chance%%';
  const out = text.replace(/^(  Description:\n)((?:  - .*\n)+)/m, (m, head, lines) => head + lines
    .replaceAll(`Smelts mined blocks with ${chance} chance.`, 'Chance to smelt mined blocks.')
    .replaceAll(`Increases amount of caught item by x2 with ${chance} chance.`, 'Chance to double the caught item.')
    .replaceAll(`${chance} chance to `, 'Chance to ')
    .replaceAll(`${chance} chance for `, 'Chance for '));
  const desc = out.match(/^  Description:\n((?:  - .*\n)+)/m)?.[1] ?? '';
  if (desc.includes('%enchantment_trigger_chance%')) {
    console.error(`${e}: a description still shows a chance percentage; add a rewrite for it:\n${desc}`);
    process.exit(1);
  }
  return out;
}

const files = fs.readdirSync(defaults).filter((f) => f.endsWith('.yml'));
const all = files.map((f) => f.replace(/\.yml$/, ''));
const tierOf = {};
for (const [tier, t] of Object.entries(TIERS)) for (const e of t.enchants) tierOf[e] = tier;
const unplaced = all.filter((e) => !tierOf[e] && !DISABLED[e] && !CURSES.includes(e));
const unknown = [...Object.keys(tierOf), ...Object.keys(DISABLED), ...CURSES].filter((e) => !all.includes(e));
const noSimple = Object.keys(tierOf).filter((e) => !SIMPLE[e]);
if (noSimple.length) {
  console.error('Every tiered enchant needs a plain description in SIMPLE: ' + noSimple.join(', '));
  process.exit(1);
}
if (unplaced.length || unknown.length) {
  console.error('Every default enchant needs a tier, a curse or a reason to disable it.');
  if (unplaced.length) console.error('  not placed: ' + unplaced.join(', '));
  if (unknown.length) console.error('  not in the defaults: ' + unknown.join(', '));
  process.exit(1);
}

fs.rmSync(out, { recursive: true, force: true });
fs.mkdirSync(path.join(out, '_disabled_'), { recursive: true });

const levels = {};
const names = {};
for (const e of all) {
  let text = fs.readFileSync(path.join(defaults, e + '.yml'), 'utf8');
  if (DISABLED[e]) {
    fs.writeFileSync(path.join(out, '_disabled_', e + '.yml'), `# AVIAN: disabled: ${DISABLED[e]}.\n` + text);
    continue;
  }
  const tier = tierOf[e];
  const name = text.match(/^  DisplayName: <c:#[0-9A-Fa-f]{6}>(.*)<\/c>$/m)?.[1]
    ?? text.match(/^  DisplayName: (.*)$/m)[1].replace(/<[^>]*>/g, '');
  names[e] = name;
  if (tier) {
    text = text.replace(/^  DisplayName: .*$/m, `  DisplayName: <c:${TIERS[tier].colour}>${name}</c>`);
    if (TIERS[tier].treasure) text = text.replace(/^  Treasure: false$/m, '  Treasure: true');
  }
  // A disabled enchant listed as incompatible makes ExcellentEnchants warn at every boot.
  for (const d of Object.keys(DISABLED)) text = text.replace(new RegExp(`^  - excellentenchants:${d}\\n`, 'gm'), '');
  text = text.replace(/^  Exclusives:\n(?!  - )/m, '  Exclusives: []\n');
  if (MAX_LEVEL[e]) text = text.replace(/^  MaxLevel: \d+$/m, `  MaxLevel: ${MAX_LEVEL[e]}`);
  if (TRIGGER_CHANCE[e]) {
    const { base, perLevel } = TRIGGER_CHANCE[e];
    text = text.replace(/^(Probability:\n  Trigger_Chance:\n    Base: )[\d.]+(\n    Per_Level: )[\d.]+/m, `$1${base}.0$2${perLevel}.0`);
  }
  text = hideChances(e, text);
  levels[e] = +text.match(/^  MaxLevel: (\d+)$/m)[1];
  const header = tier ? `# AVIAN: ${tier} tier (docs/ENCHANTS.md).` : '# AVIAN: curse, as shipped; loot only.';
  fs.writeFileSync(path.join(out, e + '.yml'), header + (MAX_LEVEL[e] ? ` Max level capped at ${MAX_LEVEL[e]}.` : '') + '\n' + text);
}

// --- item types: the plugin ships "Brekable" as the display name of the breakable set ----------
const itemTypes = fs.readFileSync(path.join(defaults, '..', 'item_types.yml'), 'utf8');
fs.writeFileSync(path.join(out, '..', 'item_types.yml'),
  '# AVIAN: the plugin\'s default item sets, with its "Brekable" typo fixed (tools/enchant-tiers.mjs).\n'
  + itemTypes.replace(/^    Name: Brekable$/m, '    Name: Breakable'));

// --- the Enchanter's crates -------------------------------------------------------------------
const roman = ['', 'I', 'II', 'III', 'IV', 'V'];
const template = fs.readFileSync(path.join(crates, 'Rare.yml'), 'utf8').split('\n');
const headerEnd = template.findIndex((l) => l.trim() === 'Prizes:');
const head = template.slice(0, headerEnd + 1).filter((l) => !l.startsWith('#')).join('\n');
const blurb = {
  Common: 'Utility and grinding enchants.',
  Rare: 'Combat effects, arrows and better tools.',
  Legendary: 'The strongest enchants on the server.',
  Mythic: 'The rarest enchants of all.',
};
for (const [tier, t] of Object.entries(TIERS)) {
  let prizes = '';
  let n = 0;
  for (const e of t.enchants) {
    const max = levels[e];
    for (let lvl = 1; lvl <= max; lvl++) {
      n++;
      const label = max > 1 ? `${names[e]} ${roman[lvl]}` : names[e];
      prizes += `    "${n}":\n`
        + `      DisplayName: "<bold><${t.colour}>${label}</${t.colour}></bold>"\n`
        + `      DisplayItem: "enchanted_book"\n`
        + `      DisplayLore: ["<#B8B8C8>${SIMPLE[e].replace(/"/g, '\\"')}</#B8B8C8>"]\n`
        + `      DisplayAmount: 1\n`
        + `      Weight: ${max - lvl + 1}\n`
        + `      Commands: ["eenchants book excellentenchants:${e} ${lvl} %player%"]\n`;
    }
  }
  const crate = head
    .replace(/Rare Crate/g, `${tier} Book`)
    .replace(/Rare Key/g, `${tier} Book`)
    .replace(/#4DA3FF/g, t.colour)
    .replace(/^  InGUI: true$/m, '  InGUI: false')
    .replace(/^  Item: "barrel"$/m, '  Item: "enchanted_book"')
    .replace(/Diamond gear, tokens and your first spawner\./g, blurb[tier])
    .replace(/Opens one [^.]*\./g, 'Rolled by the Enchanter.');
  fs.writeFileSync(path.join(crates, `Enchant${tier}.yml`),
    `# AVIAN: the Enchanter's ${tier} roll (/enchanter, docs/ENCHANTS.md). Generated by\n`
    + '# tools/enchant-tiers.mjs: edit the tables there and re-run, not this file. Hidden from /crates;\n'
    + '# opened for a player by `crazycrates forceopen` after the Enchanter charges tokens.\n'
    + crate + '\n' + prizes);
  console.log(`${tier}: ${t.enchants.length} enchants, ${n} prizes`);
}
console.log(`disabled: ${Object.keys(DISABLED).length}, curses: ${CURSES.length}`);
