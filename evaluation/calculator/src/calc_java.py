from pandas import DataFrame
from typing import Dict, Any
import math

from src.calc_util import calc_precision, calc_recall


def calc_java_precision_recall(
    result_df: DataFrame,
) -> Dict[str, Any]:
    # Refactoring types to aggregate. Start with only "Move Class".
    aggregate_types = [
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
    count = (
        result_df.groupby("refactoring type")["detected result"]
        .value_counts()
        .unstack(fill_value=0.0)
        .to_dict(orient="index")
    )

    # Compute total counts by summing the TP/FP/FN/TN values from each
    # refactoring-type entry in `count`. Treat missing or NaN values as 0.0.
    def _to_num(x):
        try:
            n = float(x)
        except Exception:
            return 0.0
        return 0.0 if math.isnan(n) else n

    filtered_count: Dict[str, Dict[str, float]] = {}
    for ref_type in aggregate_types:
        v = dict(count.get(ref_type, {}))
        filtered_count[ref_type] = v

    total_count = {"TP": 0.0, "FP": 0.0, "FN": 0.0, "TN": 0.0}
    for _, v in filtered_count.items():
        total_count["TP"] += _to_num(v.get("TP", 0))
        total_count["FP"] += _to_num(v.get("FP", 0))
        total_count["FN"] += _to_num(v.get("FN", 0))
        total_count["TN"] += _to_num(v.get("TN", 0))
    filtered_count["Total"] = total_count

    for _, v in filtered_count.items():
        tp = v.get("TP", 0)
        fp = v.get("FP", 0)
        fn = v.get("FN", 0)
        v["precision"] = calc_precision(tp=tp, fp=fp)
        v["recall"] = calc_recall(tp=tp, fn=fn)

    return {
        key: filtered_count[key]
        for key in aggregate_types + ["Total"]
    }
