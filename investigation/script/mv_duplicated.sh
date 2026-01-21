#!/usr/bin/env sh
set -eu

usage() {
  echo "Usage: $0 <directory>" 1>&2
  echo "Move files named like foo-{date}-{time}-{num}.csv where `num` is duplicated into <directory>/duplicate/<num>/" 1>&2
  exit 2
}

DIR=${1:-}
[ -n "$DIR" ] || usage
[ -d "$DIR" ] || { echo "Not a directory: $DIR" 1>&2; exit 1; }

DUP_ROOT="$DIR/duplicated"
mkdir -p "$DUP_ROOT"

TMP=$(mktemp)
trap 'rm -f "$TMP"' EXIT

# Collect num and file path pairs. Filename pattern assumed: something-...-<num>.csv
for f in "$DIR"/*; do
  [ -f "$f" ] || continue
  name=$(basename "$f")
  # skip directories and non-csv
  case "$name" in
    *.csv) ;;
    *) continue ;;
  esac
  base=${name%.*}
  # extract last hyphen-separated token as num
  num=${base##*-}
  # if num equals base (no hyphen present), skip
  if [ "$num" = "$base" ]; then
    continue
  fi
  printf '%s\t%s\n' "$num" "$f" >> "$TMP"
done

# For each num that appears more than once, move its files to duplicated/
awk -F'\t' '{cnt[$1]++; files[$1]=files[$1] RS $2} END {for (k in cnt) if (cnt[k]>1) print k}' "$TMP" | while IFS= read -r num; do
  [ -n "$num" ] || continue
  target_dir="$DUP_ROOT"
  # Move only one file from the duplicated group (the first one found)
  src=$(awk -F'\t' -v n="$num" '$1==n {print $2; exit}' "$TMP")
  if [ -n "$src" ] && [ -f "$src" ]; then
    base=$(basename "$src")
    dest="$target_dir/$base"
    if [ -e "$dest" ]; then
      i=1
      while [ -e "$target_dir/${base}.$i" ]; do i=$((i+1)); done
      dest="$target_dir/${base}.$i"
    fi
    mv "$src" "$dest"
  fi
done

echo "Moved files with duplicated num into: $DUP_ROOT"
