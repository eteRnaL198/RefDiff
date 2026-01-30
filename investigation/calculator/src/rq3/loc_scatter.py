import pandas as pd
import matplotlib.pyplot as plt
import datetime

from src.lib.load_csv import load_csv_files
from src.lib.file_util import ensure_parent_dir

OUT_PATH = './output/extract_loc_scatter_javascript_{ts}.pdf'

FREQUENT_LOC_THRESHOLD = 500


def build_loc_counts(df, loc_col):
    series = pd.to_numeric(df[loc_col], errors='coerce').dropna().astype(int)
    series = series[series >= 0]
    return series.value_counts().sort_index()


def report_frequent_loc_commits(df, loc_col, threshold):
    loc_series = pd.to_numeric(df[loc_col], errors='coerce').dropna().astype(int)
    loc_series = loc_series[loc_series >= 0]
    if loc_series.empty:
        return

    counts = loc_series.value_counts()
    frequent_locs = counts[counts >= threshold]
    if frequent_locs.empty:
        return

    for loc_value, total in frequent_locs.sort_index().items():
        subset = df.loc[loc_series.index].copy()
        subset = subset[subset[loc_col].astype(str) == str(loc_value)]
        if "Commit" not in subset.columns:
            print(f"{loc_col}={loc_value} count={int(total)} (Commit column missing)")
            continue
        commit_counts = subset["Commit"].astype(str)
        commit_counts = commit_counts[commit_counts != ""].value_counts()
        print(f"{loc_col}={loc_value} count={int(total)} unique_commits={int(commit_counts.shape[0])}")
        for commit_hash, cnt in commit_counts.sort_values(ascending=True).items():
            print(f"  {commit_hash}: {int(cnt)}")


if __name__ == '__main__':
    df_all = load_csv_files("../result")

    if df_all.empty or "Lang" not in df_all.columns:
        print("No CSV data found to plot.")
        raise SystemExit(0)

    df_js = df_all[df_all["Lang"] == "javascript"].copy()

    if df_js.empty:
        print("No JavaScript CSV data found to plot.")
        raise SystemExit(0)

    if "RefactoringType" in df_js.columns:
        df_js = df_js[df_js["RefactoringType"] == "EXTRACT"].copy()

    if df_js.empty:
        print("No 'EXTRACT' refactoring data found to plot.")
        raise SystemExit(0)

    report_frequent_loc_commits(df_js, "BeforeLOC", FREQUENT_LOC_THRESHOLD)
    report_frequent_loc_commits(df_js, "AfterLOC", FREQUENT_LOC_THRESHOLD)

    counts_before = build_loc_counts(df_js, "BeforeLOC")
    counts_after = build_loc_counts(df_js, "AfterLOC")

    if counts_before.empty and counts_after.empty:
        print("No valid LOC values to plot.")
        raise SystemExit(0)

    plt.style.use('ggplot')
    fig, ax = plt.subplots(figsize=(12, 8))

    if not counts_before.empty:
        ax.scatter(counts_before.index, counts_before.values, label="BeforeLOC", s=18, alpha=0.8)
    if not counts_after.empty:
        ax.scatter(counts_after.index, counts_after.values, label="AfterLOC", s=18, alpha=0.7)

    ax.set_title("EXTRACT Refactoring LOC Distribution (JavaScript)")
    ax.set_xlabel("Lines of Code")
    ax.set_ylabel("Count")
    ax.set_xlim(0, 200)
    ax.legend()

    plt.tight_layout()
    out_path = OUT_PATH.format(ts=datetime.datetime.now().strftime("%m%d_%H-%M"))
    ensure_parent_dir(out_path)
    plt.savefig(out_path)
    print(f"Saved LOC line plot to {out_path}")
    plt.close(fig)
