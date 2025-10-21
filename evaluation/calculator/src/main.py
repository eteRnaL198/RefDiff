"""Read and show summary for a java.csv file produced by the evaluation.

Default path: ../detection-result/1011-1833/java.csv

Usage:
  python -m src.main --file ../detection-result/1011-1833/java.csv
"""

import argparse
import sys
import csv
from pathlib import Path
from pprint import pprint

from pandas import read_csv

from src.schema import oracle_java_schema, detected_java_schema
from src.join import join_table
from src.calc import calc_precision_recall

REPO_OWNER_NAME = "icse18-refactorings"

def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Read and summarize a java.csv result file")
    parser.add_argument("--detected", "-d", default="../detection-result/1016-0052-java.csv", help="Relative path to detected results")
    parser.add_argument("--oracle", default="../oracle/java/evaluation-data-public.csv", help="Relative path to oracle")
    parser.add_argument("--output", "-o", default="result/result.csv", help="Output path")
    parser.add_argument("--ignore-line", "-i", action='store_true', help="Ignore line or not")
    args = parser.parse_args(argv)

    oracle_path = Path(args.oracle)
    try:
        if not oracle_path.exists():
            raise FileNotFoundError(f"File not found: {oracle_path}")
        oracle_java_df = read_csv(
            oracle_path, on_bad_lines="warn", dtype=oracle_java_schema, skiprows=[0]
        )
    except Exception as e:
        print(f"Error reading CSV: {e}", file=sys.stderr)
        return 2

    detected_path = Path(args.detected)
    try:
        if not detected_path.exists():
            raise FileNotFoundError(f"File not found: {detected_path}")
        detected_reprod_java_df = read_csv(
            detected_path, on_bad_lines="warn", dtype=detected_java_schema
        )
    except Exception as e:
        print(f"Error reading CSV: {e}", file=sys.stderr)
        return 2
    detected_reprod_java_df = detected_reprod_java_df.dropna() # drop rows containing "Error processing commit" in csv

    if args.ignore_line:
        result_df = join_table(oracle_java_df, detected_reprod_java_df, REPO_OWNER_NAME, does_ignore_line=True)
    else:
        result_df = join_table(oracle_java_df, detected_reprod_java_df, REPO_OWNER_NAME)

    if args.output:
        output_path = Path(args.output)
        result_df["oracle index"] = result_df["oracle index"].astype('Int64')
        result_df["detected index"] = result_df["detected index"].astype('Int64')
        result_df.to_csv(output_path, index=False, encoding="utf-8", quoting=csv.QUOTE_NONNUMERIC)

    try:
        metrics = calc_precision_recall(result_df)
        pprint(metrics, sort_dicts=False)
    except Exception as e:
        print(f"Error calculating precision/recall: {e}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
