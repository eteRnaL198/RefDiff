import pandas as pd
import matplotlib
import matplotlib.pyplot as plt
import glob
import os
import datetime
import re

from src.lib.load_csv import load_csv_files
from src.lib.file_util import ensure_parent_dir

OUT_PATH_TEMPLATE = "./output/refactoring_distribution_{ts}.pdf"
CACHE_PATH = "./output/refactoring_distribution_cache.csv"
CACHE_COLUMNS = ["Lang", "RefactoringType", "Percent", "Count"]

ENTITY_NAMES_BY_LANG = {
    "java": ["Method"],
    "c": ["Function"],
    "javascript": ["Function"],
    "python": ["Function"],
    "go": ["function_declaration", "method_declaration"],
    "php": ["function_definition", "method_declaration"],
    "ruby": ["Method"],
}

LANG_DISPLAY = {
    "java": "Java",
    "c": "C",
    "javascript": "JS",
    "python": "Python",
    "go": "Go",
    "php": "PHP",
    "ruby": "Ruby",
}

REFACTORING_DISPLAY = {
    "CHANGE_SIGNATURE": "Change\nSignature",
    "EXTRACT": "Extract",
    "EXTRACT_MOVE": "Extract &\nMove",
    "INLINE": "Inline",
    "MOVE": "Move",
    "RENAME": "Rename",
    "MOVE_RENAME": "Move &\nRename",
}

def build_cache_df():
    df = load_csv_files("../result")
    if df.empty or "Lang" not in df.columns:
        print("No refactoring data found to plot.")
        return pd.DataFrame()

    # Step 1: Calculate percentages and absolute counts
    allowed_refactoring_types = [
        "CHANGE_SIGNATURE",
        "EXTRACT",
        "EXTRACT_MOVE",
        "INLINE",
        "MOVE",
        "RENAME",
        "MOVE_RENAME",
    ]

    rows = []
    for lang, df_lang in df.groupby("Lang"):
        df_lang = df_lang.copy()
        if "RefactoringType" in df_lang.columns:
            if "Before" in df_lang.columns:
                entity_names = ENTITY_NAMES_BY_LANG.get(lang, [])
                if entity_names:
                    pattern = r"^\{(?:%s)\b" % "|".join(
                        re.escape(name) for name in entity_names
                    )
                    df_lang = df_lang[
                        df_lang["Before"].astype(str).str.match(pattern, na=False)
                    ]

            df_lang["RefactoringType"] = df_lang["RefactoringType"].replace(
                ["INTERNAL_MOVE"], "MOVE"
            )
            df_lang["RefactoringType"] = df_lang["RefactoringType"].replace(
                ["INTERNAL_MOVE_RENAME"], "MOVE_RENAME"
            )

            # Filter the DataFrame to include only allowed refactoring types
            filtered_df = df_lang[
                df_lang["RefactoringType"].isin(allowed_refactoring_types)
            ]

            # Calculate normalized counts for the filtered DataFrame
            counts = filtered_df["RefactoringType"].value_counts(normalize=True) * 100
            absolute_counts = filtered_df["RefactoringType"].value_counts()
            for ref_type, percent in counts.items():
                rows.append(
                    {
                        "Lang": lang,
                        "RefactoringType": ref_type,
                        "Percent": float(percent),
                        "Count": int(absolute_counts.get(ref_type, 0)),
                    }
                )

    if not rows:
        print("No data available to plot.")
        return pd.DataFrame()

    return pd.DataFrame(rows, columns=CACHE_COLUMNS)


def load_cache_df(cache_path):
    if not os.path.exists(cache_path):
        return None
    try:
        df = pd.read_csv(cache_path)
    except Exception as e:
        print(f"Failed to read cache {cache_path}: {e}")
        return None
    if df.empty:
        return df
    missing = [c for c in CACHE_COLUMNS if c not in df.columns]
    if missing:
        print(f"Cache {cache_path} is missing columns: {missing}")
        return None
    return df


def save_cache_df(df, cache_path):
    ensure_parent_dir(cache_path)
    df.to_csv(cache_path, index=False)
    print(f"Saved cache to {cache_path} ({len(df)} rows)")


if __name__ == "__main__":
    matplotlib.rc("pdf", fonttype=42)
    plt.rcParams.update({
        "font.size": 24,
        "axes.titlesize": 16,
        "axes.labelsize": 16,
        "xtick.labelsize": 24,
        "ytick.labelsize": 24,
        "legend.fontsize": 16,
    })
    cache_df = load_cache_df(CACHE_PATH)
    if cache_df is not None:
        print(f"Loaded cache from {CACHE_PATH} ({len(cache_df)} rows)")
    if cache_df is None:
        cache_df = build_cache_df()
        if cache_df.empty:
            exit(0)
        save_cache_df(cache_df, CACHE_PATH)

    # Prepare data for plotting
    plot_data = (
        cache_df.pivot(index="RefactoringType", columns="Lang", values="Percent")
        .fillna(0)
    )

    # Check if there is data to plot
    if plot_data.empty:
        print("No data available to plot.")
        exit(0)

    # Transpose for plotting (languages on x-axis)
    lang_order = ["java", "c", "javascript", "python", "go", "php", "ruby"]
    plot_data = plot_data.reindex(columns=lang_order)
    ax = plot_data.T.plot(kind="bar", stacked=True, figsize=(11, 7.5), width=0.7)
    display_labels = [LANG_DISPLAY.get(lang, lang) for lang in plot_data.columns]
    ax.set_xticklabels(display_labels, rotation=0)

    # Add percentages on the bars
    for container in ax.containers:
        # The label for each container is the refactoring type
        refactoring_type = container.get_label()

        # Get the percentage values for this type across all languages
        if refactoring_type in plot_data.index:
            labels = plot_data.loc[refactoring_type].values

            # Create labels only for non-zero bars to avoid clutter
            display_labels = [f"{v:.1f}" if v > 1 else "" for v in labels]

            ax.bar_label(
                container,
                labels=display_labels,
                label_type="center",
                color="black",
                fontsize=16,
            )

    plt.title("")
    plt.xlabel("Language")
    plt.ylabel("Percentage (%)")
    handles, labels = ax.get_legend_handles_labels()
    labels = [REFACTORING_DISPLAY.get(l, l) for l in labels]
    handles = list(reversed(handles))
    labels = list(reversed(labels))
    plt.legend(
        handles,
        labels,
        loc="upper left",
        bbox_to_anchor=(1.02, 1),
    )
    plt.tight_layout()
    plt.subplots_adjust(right=0.78)

    # Save the plot (include month-day and hour-minute, no year)
    out_path = OUT_PATH_TEMPLATE.format(
        ts=datetime.datetime.now().strftime("%m-%d_%H-%M")
    )
    ensure_parent_dir(out_path)
    plt.savefig(out_path)

    print(f"Plot saved to {out_path}")
