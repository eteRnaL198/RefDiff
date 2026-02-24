from pathlib import Path
from typing import Any, Dict

from pandas import DataFrame, read_csv

from src.calc_util import calc_precision, calc_recall
from src.schema import oracle_java_schema

DEFAULT_ORACLE_PATH = "../oracle/java/evaluation-data-public.csv"
RESULT_COLUMN = "RefDiff 2.0"
TYPE_COLUMN = "Refactoring Type"
AGGREGATION_TYPES = [
    # "Move Class",
    "Move Method",
    # "Rename Class",
    "Rename Method",
    # "Extract Interface",
    # "Extract Superclass",
    # "Pull Up Method",
    # "Push Down Method",
    "Extract Method",
    "Inline Method",
]


def read_oracle_csv(path: str | Path = DEFAULT_ORACLE_PATH) -> DataFrame:
    oracle_path = Path(path)
    return read_csv(
        oracle_path,
        on_bad_lines="warn",
        dtype=oracle_java_schema,
        skiprows=[0],  # skip first row (header description)
    )


def calc_oracle_precision_recall(
    oracle_df: DataFrame,
) -> Dict[str, Dict[str, Any]]:
    normalized = oracle_df.copy()
    normalized[RESULT_COLUMN] = (
        normalized[RESULT_COLUMN].astype(str).str.strip().str.upper()
    )

    valid = {"TP", "FP", "TN", "FN"}
    normalized = normalized[normalized[RESULT_COLUMN].isin(valid)]

    count = (
        normalized.groupby(TYPE_COLUMN)[RESULT_COLUMN]
        .value_counts()
        .unstack(fill_value=0.0)
        .to_dict(orient="index")
    )

    filtered_count: Dict[str, Dict[str, float]] = {}
    for ref_type in AGGREGATION_TYPES:
        filtered_count[ref_type] = dict(count.get(ref_type, {}))

    total_count = {"TP": 0.0, "FP": 0.0, "FN": 0.0, "TN": 0.0}
    for _, v in filtered_count.items():
        total_count["TP"] += v.get("TP", 0)
        total_count["FP"] += v.get("FP", 0)
        total_count["FN"] += v.get("FN", 0)
        total_count["TN"] += v.get("TN", 0)
    filtered_count["Total"] = total_count

    for _, v in filtered_count.items():
        tp = v.get("TP", 0)
        fp = v.get("FP", 0)
        fn = v.get("FN", 0)
        v["precision"] = calc_precision(tp=tp, fp=fp)
        v["recall"] = calc_recall(tp=tp, fn=fn)

    return {
        key: filtered_count[key]
        for key in AGGREGATION_TYPES + ["Total"]
    }


def main() -> int:
    oracle_df = read_oracle_csv()
    metrics = calc_oracle_precision_recall(oracle_df)
    for ref_type, vals in metrics.items():
        print(f"{ref_type}:")
        print(f"  TP: {vals.get('TP', 0)}")
        print(f"  FP: {vals.get('FP', 0)}")
        print(f"  FN: {vals.get('FN', 0)}")
        print(f"  TN: {vals.get('TN', 0)}")
        count = (
            vals.get("TP", 0)
            + vals.get("FP", 0)
            + vals.get("FN", 0)
            + vals.get("TN", 0)
        )
        print(f"  Count: {count}")
        print(f"  Precision: {vals.get('precision', 0):.4f}")
        print(f"  Recall: {vals.get('recall', 0):.4f}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
