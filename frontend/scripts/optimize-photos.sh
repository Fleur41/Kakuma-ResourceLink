#!/usr/bin/env bash
# Prepares a background photo for the web app.
#   scripts/optimize-photos.sh <photo.jpg|png|heic-converted> <hero|dashboard|community>
# Resizes to 1920px wide, compresses to WebP (usually 150–300 KB, important on slow connections) and
# strips all metadata, including GPS location, which could reveal where people live.
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <photo> <hero|dashboard|community>" >&2
  exit 1
fi
src=$1
slot=$2
case "$slot" in hero | dashboard | community) ;; *)
  echo "Slot must be hero, dashboard or community (see src/lib/photos.ts)" >&2
  exit 1
  ;;
esac
command -v cwebp >/dev/null || { echo "cwebp not found. Install it with: brew install webp" >&2; exit 1; }

out="$(cd "$(dirname "$0")/.." && pwd)/public/images/$slot.webp"
mkdir -p "$(dirname "$out")"
cwebp -quiet -resize 1920 0 -q 72 -metadata none "$src" -o "$out"
echo "Wrote $out ($(($(wc -c <"$out") / 1024)) KB). Add the photographer to src/lib/photos.ts → $slot.credit."
