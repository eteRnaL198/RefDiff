"""Select random records from a CSV and print or save the sample.

Defaults are defined as constants at the top of the file.
"""

import argparse
import sys
from pathlib import Path
from typing import Optional

from pandas import read_csv, DataFrame, concat

# Default CLI values (use these constants in argparse)
# DEFAULT_DIR = "../detection-result/c/head"
# DEFAULT_DIR = "../detection-result/javascript/head"
DEFAULT_DIR = "../detection-result/java/head"
# Default seed used when none is provided explicitly
DEFAULT_SEED = 42
# Number of records to sample per `type` value when grouping by 'type'
SAMPLES_PER_TYPE = 10
# Default output directory (parent of evaluation/calculator/src is evaluation/calculator/,
# so ../sampled matches request)
DEFAULT_OUTPUT_DIR = "./sampled"
# Default language name used for output naming
DEFAULT_LANGUAGE = "java"
# DEFAULT_LANGUAGE = "c"
# DEFAULT_LANGUAGE = "javascript"
# Only process rows whose (type, element) match these per language. Empty means all.
# Example: {"java": [("EXTRACT", "Method"), ("EXTRACT_SUPER", "Class")]}
TARGET_PAIRS: dict[str, list[tuple[str, str]]] = {
    "java": [
        ("MOVE", "Class"),
        ("MOVE", "Method"),
        ("RENAME", "Class"),
        ("RENAME", "Method"),
        ("EXTRACT_SUPER", "Interface"),
        ("EXTRACT_SUPER", "Class"),
        ("PULL_UP", "Method"),
        ("PUSH_DOWN", "Method"),
        ("EXTRACT", "Method"),
        ("INLINE", "Method"),
    ],
    "c": [
        ("CHANGE_SIGNATURE", "Function"),
        ("MOVE", "File"),
        ("MOVE", "Function"),
        ("RENAME", "File"),
        ("RENAME", "Function"),
        ("MOVE_RENAME", "File"),
        ("MOVE_RENAME", "Function"),
        ("EXTRACT", "Function"),
        ("INLINE", "Function"),
    ],
    "javascript": [
        ("MOVE", "File"),
        ("MOVE", "Class"),
        ("MOVE", "Function"),
        ("RENAME", "File"),
        ("RENAME", "Class"),
        ("RENAME", "Function"),
        ("MOVE_RENAME", "File"),
        ("MOVE_RENAME", "Function"),
        ("EXTRACT", "Function"),
        ("INLINE", "Function"),
    ],
}
# Group types together per language when sampling.
# Example: {"java": {"MOVE_RENAME": "MOVE"}}
TYPE_GROUPS: dict[str, dict[str, str]] = {
    "java": {
        "MOVE_RENAME": "MOVE",
        "EXTRACT_MOVE": "EXTRACT",
    },
}

def main(argv: Optional[list[str]] = None) -> int:
    parser = argparse.ArgumentParser(description="Select random records from CSVs inside a single directory")
    parser.add_argument("--dir", "-d", default=DEFAULT_DIR, help="Path to directory containing CSV files")
    parser.add_argument("--per-type", type=int, default=SAMPLES_PER_TYPE, help="Number of records to sample per 'type' value")
    parser.add_argument("--seed", type=int, default=DEFAULT_SEED, help="Optional random seed")
    parser.add_argument("--output", "-o", default=None, help="Optional output directory path (if omitted, writes to DEFAULT_OUTPUT_DIR/sampled-{lang}-seed{seed})")
    parser.add_argument("--lang", default=DEFAULT_LANGUAGE, help="Language name used for output naming")
    args = parser.parse_args(argv)
    language = args.lang

    # Support single directory input: validate directory and collect CSV files (non-recursive)
    dir_path = Path(args.dir)
    if not dir_path.exists():
        print(f"Path not found: {dir_path}", file=sys.stderr)
        return 2
    if not dir_path.is_dir():
        print(f"Not a directory: {dir_path}", file=sys.stderr)
        return 2
    # collect .csv files in the directory (non-recursive), deterministic order
    expanded_files = sorted([f for f in dir_path.iterdir() if f.is_file() and f.suffix.lower() == ".csv"])
    if not expanded_files:
        print(f"No CSV files found in directory: {dir_path}", file=sys.stderr)
        return 2

    data_frames: list[DataFrame] = []
    for p in expanded_files:
        try:
            df_part = read_csv(p, on_bad_lines="warn")
        except Exception as e:
            print(f"Error reading CSV {p}: {e}", file=sys.stderr)
            return 3
        data_frames.append(df_part)

    # Concatenate all input files into a single DataFrame
    try:
        df = concat(data_frames, ignore_index=True)
    except Exception as e:
        print(f"Error concatenating input CSVs: {e}", file=sys.stderr)
        return 3

    if df.empty:
        print("Input CSV is empty", file=sys.stderr)
        return 4

    # Exclude rows that contain 'dist/' in 'before' or 'after' columns
    path_cols = [col for col in ["before", "after"] if col in df.columns]
    for col in path_cols:
        df = df[~df[col].astype(str).str.contains("dist/|min/|esm.js|markdown.js", na=False)]

    # If the CSV has a 'type' column, sample per-type; otherwise fall back to global sampling
    per_type = int(args.per_type)
    try:
        # extract syntax element from the start of the 'before' column (e.g. '{Function', 'Class')
        if "before" in df.columns:
            df["element"] = df["before"].astype(str).str.extract(r'^\{?\s*([A-Za-z_]+)', expand=False)
        else:
            df["element"] = ""

        if "type" not in df.columns:
            print("CSV does not contain a 'type' column; per-type sampling requires 'type'", file=sys.stderr)
            return 5

        # Filter by (type, element) if configured for this language
        df = filter_by_targets(language, df)
        if df.empty:
            print("No records match configured target pairs", file=sys.stderr)
            return 4

        # Sample per-type while ensuring no two sampled rows share the same 'commit'
        if "commit" not in df.columns:
            print("CSV does not contain a 'commit' column; uniqueness across commits requires 'commit'", file=sys.stderr)
            return 7

        parts = []
        chosen_commits: set[str] = set()
        df["type_group"] = df["type"].apply(lambda t: normalize_type(language, str(t)))
        # iterate groups in file order (avoid sorting groups)
        for _, g in df.groupby("type_group", sort=False):
            # iterate element categories in order of appearance within this type
            seen_elements: list[str] = []
            for val in g["element"].fillna("").tolist():
                if val not in seen_elements:
                    seen_elements.append(val)
            for element in seen_elements:
                # filter rows by element and exclude already-chosen commits (compare as str)
                available = g[g["element"] == element]
                available = available[~available["commit"].astype(str).isin(chosen_commits)]
                if available.empty:
                    continue
                # ensure one row per commit (keep first occurrence)
                available_unique = available.drop_duplicates(subset=["commit"], keep="first")
                k = min(len(available_unique), per_type)
                if args.seed is not None:
                    sampled_part = available_unique.sample(n=k, random_state=int(args.seed))
                else:
                    sampled_part = available_unique.sample(n=k)
                parts.append(sampled_part)
                # record chosen commits as strings
                chosen_commits.update(sampled_part["commit"].astype(str).tolist())

        if parts:
            sampled = concat(parts)
            # sort output by 'type' so rows are grouped by type in the CSV
            sampled = sampled.sort_values(by="type")
            if "type_group" in sampled.columns:
                sampled = sampled.drop(columns=["type_group"])
        else:
            sampled = df.head(0)

        for _col in ("manual check result", "notes"):
            if _col not in sampled.columns:
                sampled[_col] = ""
    except Exception as e:
        print(f"Error sampling CSV by type: {e}", file=sys.stderr)
        return 6

    # Determine output directory: use provided --output, otherwise default to DEFAULT_OUTPUT_DIR/sampled-{lang}-seed{seed}
    output_dir = make_output_dir(language, args.seed, args.output)

    try:
        wrote_any = False
        for (type_value, element_value), g in sampled.groupby(["type", "element"], sort=False):
            kind = make_kind_name(type_value, element_value)
            if not should_write_kind(language, kind):
                continue
            output_path = output_dir / f"sampled-{language}-{kind}-seed{args.seed}.csv"
            g.to_csv(output_path, index=False)
            wrote_any = True
        if not wrote_any:
            print("No records to write", file=sys.stderr)
            return 6
    except Exception as e:
        print(f"Error writing output CSVs: {e}", file=sys.stderr)
        return 6

    # Also print the chosen output directory to stdout for convenience
    print(f"Wrote {len(sampled)} records to directory: {output_dir}")

    return 0

def make_output_dir(
    language: str,
    seed: Optional[int] = None,
    output: Optional[str] = None,
) -> Path:
    """Generate the output directory path.

    If `output` is provided, return that Path. Otherwise create `DEFAULT_OUTPUT_DIR`
    and generate a directory name using the provided language name.
    """
    if output:
        out = Path(output)
        out.mkdir(parents=True, exist_ok=True)
        return out

    output_dir = Path(DEFAULT_OUTPUT_DIR)
    output_dir.mkdir(parents=True, exist_ok=True)

    dirname = f"sampled-{language}-seed{seed}"
    out = output_dir / dirname
    out.mkdir(parents=True, exist_ok=True)
    return out

def make_kind_name(type_value: str, element_value: str) -> str:
    """Create a stable kind name from type and element."""
    type_part = (type_value or "").strip()
    element_part = (element_value or "").strip()
    kind = f"{type_part}-{element_part}" if element_part else type_part
    # sanitize for filename
    safe = "".join(ch if ch.isalnum() or ch in ("-", "_") else "_" for ch in kind)
    return safe or "unknown"

def filter_by_targets(language: str, df: DataFrame) -> DataFrame:
    target = TARGET_PAIRS.get(language, [])
    if not target:
        return df
    allowed = {(t.strip(), e.strip()) for t, e in target}
    return df[df.apply(lambda r: (str(r.get("type", "")).strip(), str(r.get("element", "")).strip()) in allowed, axis=1)]

def should_write_kind(language: str, kind: str) -> bool:
    target = TARGET_PAIRS.get(language, [])
    if not target:
        return True
    # keep naming consistent with filter (type-element)
    return kind in {make_kind_name(t, e) for t, e in target}

def normalize_type(language: str, type_value: str) -> str:
    mapping = TYPE_GROUPS.get(language, {})
    value = type_value.strip()
    return mapping.get(value, value)

if __name__ == "__main__":
	raise SystemExit(main())
