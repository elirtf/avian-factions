#!/usr/bin/perl
# Rewrites one EconomyShopGUI shop file (plugin defaults) to the Avian price sheet, docs/ECONOMY.md.
# Usage: tools/reprice-shop.pl <default shop file> <output file>
# Hand-written, not produced by this script: Mobs/spawners.yml, and magic.yml without its potion
# button. Do not run it over those two.
use strict; use warnings; use POSIX qw(ceil);

# --- what can be SOLD, and for how much (everything else: not sellable) -----------------------
my %sell = (
  # Farming: sugar cane is the best crop by design.
  SUGAR_CANE=>16, CACTUS=>4, PUMPKIN=>3, MELON_SLICE=>1, WHEAT=>2, CARROT=>2, POTATO=>2,
  BEETROOT=>2, NETHER_WART=>2, COCOA_BEANS=>2, SWEET_BERRIES=>1, GLOW_BERRIES=>1,
  CHORUS_FRUIT=>2, BAMBOO=>1, KELP=>1, BROWN_MUSHROOM=>1, RED_MUSHROOM=>1,
  # Mob drops, by spawner tier (see docs/ECONOMY.md).
  PORKCHOP=>1, BEEF=>1, MUTTON=>1, CHICKEN=>1, RABBIT=>1,
  COOKED_PORKCHOP=>2, COOKED_BEEF=>2, COOKED_MUTTON=>2, COOKED_CHICKEN=>2, COOKED_RABBIT=>2,
  FEATHER=>2, LEATHER=>5, RABBIT_HIDE=>1,
  ROTTEN_FLESH=>12, BONE=>10, ARROW=>2, STRING=>10, SPIDER_EYE=>8,
  GUNPOWDER=>30, ENDER_PEARL=>60, SLIME_BALL=>15, MAGMA_CREAM=>25,
  BLAZE_ROD=>120, IRON_INGOT=>40, GHAST_TEAR=>50, PHANTOM_MEMBRANE=>20, INK_SAC=>2,
  # Ores: ingots/gems, and their storage blocks at exactly 9x.
  COAL=>5, COAL_BLOCK=>45, IRON_BLOCK=>360, GOLD_INGOT=>8, GOLD_BLOCK=>72,
  DIAMOND=>150, DIAMOND_BLOCK=>1350, LAPIS_LAZULI=>4, LAPIS_BLOCK=>36, REDSTONE=>2,
  REDSTONE_BLOCK=>18, QUARTZ=>6, AMETHYST_SHARD=>3,
  NETHERITE_SCRAP=>750, ANCIENT_DEBRIS=>750, NETHERITE_INGOT=>3000, NETHERITE_BLOCK=>27000,
);

# --- things that craft or smelt INTO a sellable item: never cheaper to buy than to sell -------
my %floor = (
  IRON_NUGGET=>18, RAW_IRON=>160, RAW_IRON_BLOCK=>1440, IRON_ORE=>160, DEEPSLATE_IRON_ORE=>160,
  GOLD_NUGGET=>4, RAW_GOLD=>32, RAW_GOLD_BLOCK=>288, GOLD_ORE=>32, DEEPSLATE_GOLD_ORE=>32,
  NETHER_GOLD_ORE=>32, DIAMOND_ORE=>600, DEEPSLATE_DIAMOND_ORE=>600, COAL_ORE=>20,
  DEEPSLATE_COAL_ORE=>20, LAPIS_ORE=>16, DEEPSLATE_LAPIS_ORE=>16, REDSTONE_ORE=>8,
  DEEPSLATE_REDSTONE_ORE=>8, NETHER_QUARTZ_ORE=>24, RAW_COPPER=>4, RAW_COPPER_BLOCK=>36,
  COPPER_ORE=>4, DEEPSLATE_COPPER_ORE=>4, COPPER_NUGGET=>1, HAY_BLOCK=>72, SLIME_BLOCK=>540,
  MELON=>36, BONE_BLOCK=>1, QUARTZ_BLOCK=>48,
);

# --- never buyable: rare PvP consumables (#40) and enchantments ---------------------------------
my %nobuy = map { $_=>1 } qw(GOLDEN_APPLE ENCHANTED_GOLDEN_APPLE DRAGON_BREATH POTION
  SPLASH_POTION LINGERING_POTION TIPPED_ARROW ENCHANTED_BOOK);

# --- explicit buy prices: gear by tier, and a few specials ------------------------------------
my %buy;
my %tier = (WOODEN=>100, STONE=>250, COPPER=>500, GOLDEN=>1500, IRON=>2500, DIAMOND=>15000, NETHERITE=>60000);
my %tool = (PICKAXE=>1, AXE=>1, SWORD=>1, SPEAR=>0.75, SHOVEL=>0.5, HOE=>0.5);
for my $t (keys %tier) { for my $k (keys %tool) { $buy{"${t}_$k"} = ceil($tier{$t} * $tool{$k}); } }
my %armor = (LEATHER=>250, CHAINMAIL=>2000, COPPER=>500, GOLDEN=>1500, IRON=>2500, DIAMOND=>15000, NETHERITE=>60000);
my %piece = (HELMET=>1.25, CHESTPLATE=>2, LEGGINGS=>1.75, BOOTS=>1);
for my $t (keys %armor) { for my $p (keys %piece) { $buy{"${t}_$p"} = ceil($armor{$t} * $piece{$p}); } }
%buy = (%buy, BOW=>500, CROSSBOW=>750, ARROW=>8, SPECTRAL_ARROW=>12, SHIELD=>500,
  TRIDENT=>25000, MACE=>100000, TOTEM_OF_UNDYING=>25000, ELYTRA=>250000,
  SUGAR_CANE=>64, CACTUS=>20);

my ($in, $out) = @ARGV;
open my $fh, '<', $in or die "$in: $!";
my @lines = <$fh>; close $fh;

# Group into items: an item starts at a 6-space "key:" line under "items:".
my @outl; my @item; my $mat;
sub flush {
  return unless @item;
  my ($has_buy, $has_sell) = (0, 0);
  for (@item) { $has_buy++ if /^\s{8}buy:/; $has_sell++ if /^\s{8}sell:/; }
  my $m = $mat // '';
  # EconomyShopGUI prices are for the item's stack-size (a diamond at stack-size 9 is priced per 9),
  # while every table here is per ONE item: scale by the stack-size.
  my $n = 1;
  for (@item) { $n = $1 if /^\s{8}stack-size:\s*(\d+)/; }
  my $sellp = exists $sell{$m} ? $sell{$m} * $n : undef;
  for (@item) {
    if (/^(\s{8})buy:\s*(\S+)/) {
      my ($ind, $v) = ($1, $2);
      my $b = $v;
      if ($nobuy{$m}) { $b = -1 }
      elsif (exists $buy{$m}) { $b = $buy{$m} * $n }
      elsif ($b != -1) { $b = ceil($b); $b = 1 if $b < 1; }
      if ($b != -1) {
        $b = $floor{$m} * $n if exists $floor{$m} && $b < $floor{$m} * $n;
        $b = 4 * $sellp if defined $sellp && $b < 4 * $sellp;   # never buy low, sell high
      }
      $_ = "${ind}buy: $b\n";
    } elsif (/^(\s{8})sell:/) {
      $_ = "$1sell: " . (defined $sellp ? $sellp : -1) . "\n";
    }
  }
  if (!$has_sell && defined $sellp) { push @item, "        sell: $sellp\n"; }
  push @outl, @item; @item = (); $mat = undef;
}
for (@lines) {
  if (/^\s{6}\S[^:]*:\s*$/) { flush(); push @item, $_; next; }
  if (@item && /^\s{0,6}\S/) { flush(); push @outl, $_; next; }
  if (@item) { $mat = $1 if /^\s{8}material:\s*(\S+)/; push @item, $_; next; }
  push @outl, $_;
}
flush();
open my $oh, '>', $out or die "$out: $!"; print $oh @outl; close $oh;
