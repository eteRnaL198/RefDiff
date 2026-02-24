"""Compute precision per CSV file based on 'manual check result' column.

Precision = TP / (TP + FP) for each file in a directory.
"""

import argparse
import csv
import sys
from pathlib import Path
from typing import Optional

DEFAULT_DIR = "./manually_assessment/sampled-java-seed42"
DEFAULT_FILES = [
    "sampled-java-EXTRACT-Method-seed42.csv",
    "sampled-java-INLINE-Method-seed42-0126.csv",
    "sampled-java-MOVE-Method-seed42.csv",
    "sampled-java-RENAME-Method-seed42.csv",
]
RESULT_COLUMN = "manual check result"


def compute_precision(csv_path: Path) -> tuple[int, int, int, float]:
    tp = 0
    fp = 0
    with csv_path.open(newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        if reader.fieldnames is None or RESULT_COLUMN not in reader.fieldnames:
            raise ValueError(f"Missing '{RESULT_COLUMN}' column")
        for row in reader:
            val = (row.get(RESULT_COLUMN) or "").strip().upper()
            if val == "TP":
                tp += 1
            elif val == "FP":
                fp += 1
    total = tp + fp
    precision = tp / total if total > 0 else 0.0
    return tp, fp, total, precision


def main(argv: Optional[list[str]] = None) -> int:
    parser = argparse.ArgumentParser(description="Compute precision per CSV file")
    parser.add_argument("--dir", "-d", default=DEFAULT_DIR, help="Directory containing CSV files")
    parser.add_argument(
        "--file",
        "-f",
        action="append",
        default=DEFAULT_FILES,
        help="CSV file name within the directory (can be repeated)",
    )
    args = parser.parse_args(argv)

    dir_path = Path(args.dir)
    if not dir_path.exists():
        print(f"Path not found: {dir_path}", file=sys.stderr)
        return 2
    if not dir_path.is_dir():
        print(f"Not a directory: {dir_path}", file=sys.stderr)
        return 2

    if args.file:
        csv_files = []
        missing = []
        for name in args.file:
            p = dir_path / name
            if p.is_file() and p.suffix.lower() == ".csv":
                csv_files.append(p)
            else:
                missing.append(name)
        if missing:
            missing_list = ", ".join(missing)
            print(f"CSV file(s) not found in directory: {missing_list}", file=sys.stderr)
            return 3
    else:
        csv_files = sorted([p for p in dir_path.iterdir() if p.is_file() and p.suffix.lower() == ".csv"])
        if not csv_files:
            print(f"No CSV files found in directory: {dir_path}", file=sys.stderr)
            return 3

    total_tp = 0
    total_fp = 0
    for csv_path in csv_files:
        try:
            tp, fp, total, precision = compute_precision(csv_path)
        except Exception as e:
            print(f"{csv_path.name}: error: {e}", file=sys.stderr)
            continue
        total_tp += tp
        total_fp += fp
        print(f"{csv_path.name}\tprecision={precision:.4f}\tTP={tp}\tFP={fp}\tN={total}")

    total = total_tp + total_fp
    total_precision = total_tp / total if total > 0 else 0.0
    print(f"TOTAL\tprecision={total_precision:.4f}\tTP={total_tp}\tFP={total_fp}\tN={total}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
