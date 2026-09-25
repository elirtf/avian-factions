#!/usr/bin/env bash
# Regenerates the BetterHud HUD art in dev-server/plugins/BetterHud/assets/ (avian_*.png).
#
# The heart orb, heartbeat and bars are a remix of pxlpunkt's "Pulsing Heart" (itch.io; commercial
# use and modification allowed, redistributing the asset pack itself is not). Its files are NOT in
# this repo: download it and point ART at the unzipped folder (default: the art-reference folder).
#
#   tools/hud-art/build.sh            then: ./dev pack  (or ./dev restart)
set -euo pipefail
cd "$(dirname "$0")/../.."
ART="${ART:-$HOME/projects/minecraft/art-reference/pulsing-heart}"
OUT=dev-server/plugins/BetterHud/assets
[[ -f "$ART/PulsingHeart.gif" ]] || { echo "Pulsing Heart assets not found in $ART (set ART=...)" >&2; exit 1; }
T=tools/hud-art
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT

rm -f "$OUT"/avian_*.png
java "$T/Gif.java" "$ART/PulsingHeart.gif" "$tmp"        # the 12 GIF frames (stored at 10x)
java "$T/Strip.java" "$tmp" "$tmp/strip.png"             # composited back to real pixels: s0..s11.png
java "$T/HeartRemix.java" "$tmp" "$OUT"                  # heart orb frames: plum glass, red for low health
java "$T/FoodOrb.java" "$OUT"                            # food orb, from the heart orb's rim and glass
java "$T/BarRemix.java" "$ART" "$OUT"                    # casings and every status fill, left and mirrored
java "$T/LevelArt.java" "$OUT"                           # level orbs + ornaments per tier, level-up burst
java "$T/Pin.java" "$OUT"                                # corner pins, so BetterHud keeps one scale
echo "Wrote $(ls "$OUT"/avian_*.png | wc -l) images to $OUT"
