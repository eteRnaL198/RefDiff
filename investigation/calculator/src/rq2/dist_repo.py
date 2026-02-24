import pandas as pd
import matplotlib
import matplotlib.pyplot as plt
import os
import datetime
import numpy as np
import re

from src.lib.load_csv import load_csv_files
from src.lib.file_util import ensure_parent_dir

OUT_PATH_TEMPLATE = './output/dist_repo/refactoring_dist_repo_{lang}_{ts}.pdf'
CACHE_PATH = './output/refactoring_dist_repo_cache.csv'
CACHE_COLUMNS = ['Repo', 'Lang', 'RefactoringType', 'Percent', 'Count']

ENTITY_NAMES_BY_LANG = {
    "java": ["Method"],
    "c": ["Function"],
    "javascript": ["Function"],
    "python": ["Function"],
    "go": ["function_declaration", "method_declaration"],
    "php": ["function_definition", "method_declaration"],
    "ruby": ["Method"],
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
    if df.empty:
        print("No refactoring data found to plot.")
        return pd.DataFrame()
    required_columns = {'Repo', 'Lang', 'RefactoringType'}
    missing_required = required_columns - set(df.columns)
    if missing_required:
        print(f"Missing required columns: {sorted(missing_required)}")
        return pd.DataFrame()

    allowed_refactoring_types = ['CHANGE_SIGNATURE', 'EXTRACT', 'EXTRACT_MOVE', 'INLINE', 'MOVE', 'RENAME', 'MOVE_RENAME']
    rows = []

    for repo, df_repo in df.groupby('Repo'):
        df_repo = df_repo.copy()
        lang = df_repo['Lang'].iloc[0] if not df_repo.empty else None
        if 'RefactoringType' in df_repo.columns:
            if 'Before' in df_repo.columns:
                entity_names = ENTITY_NAMES_BY_LANG.get(lang, []) if lang is not None else []
                if entity_names:
                    pattern = r"^\{(?:%s)\b" % "|".join(
                        re.escape(name) for name in entity_names
                    )
                    df_repo = df_repo[
                        df_repo["Before"].astype(str).str.match(pattern, na=False)
                    ]

            df_repo['RefactoringType'] = df_repo['RefactoringType'].replace(['INTERNAL_MOVE'], 'MOVE')
            df_repo['RefactoringType'] = df_repo['RefactoringType'].replace(['INTERNAL_MOVE_RENAME'], 'MOVE_RENAME')

            # Filter to allowed refactoring types
            filtered_df = df_repo[df_repo['RefactoringType'].isin(allowed_refactoring_types)]

            # Percentages and counts per repo
            counts = filtered_df['RefactoringType'].value_counts(normalize=True) * 100
            absolute_counts = filtered_df['RefactoringType'].value_counts()
            for ref_type, percent in counts.items():
                rows.append(
                    {
                        'Repo': repo,
                        'Lang': lang,
                        'RefactoringType': ref_type,
                        'Percent': float(percent),
                        'Count': int(absolute_counts.get(ref_type, 0)),
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


if __name__ == '__main__':
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
        cache_df.pivot(index='RefactoringType', columns='Repo', values='Percent')
        .fillna(0)
    )

    # Check if there is data to plot
    if plot_data.empty:
        print("No data available to plot.")
        exit(0)

    # order repos by language then repo name
    repo_lang = cache_df[['Repo', 'Lang']].drop_duplicates().set_index('Repo')['Lang'].to_dict()
    lang_order = ['java', 'c', 'javascript', 'python', 'go', 'php', 'ruby']
    lang_rank = {lang: i for i, lang in enumerate(lang_order)}
    ordered_repos = sorted(repo_lang.keys(), key=lambda r: (lang_rank.get(repo_lang.get(r, ''), 999), r))
    ordered_langs = sorted(
        {repo_lang.get(repo, '') for repo in ordered_repos if repo_lang.get(repo, '')},
        key=lambda l: lang_rank.get(l, 999)
    )

    ts = datetime.datetime.now().strftime("%m-%d_%H-%M")
    saved_paths = []

    for lang in ordered_langs:
        repos_in_lang = [repo for repo in ordered_repos if repo_lang.get(repo) == lang]
        if not repos_in_lang:
            continue

        # Rows: repos in one language, Columns: refactoring types
        plot_df = plot_data.reindex(columns=repos_in_lang).T.fillna(0)
        n_types = len(plot_df.columns)
        if n_types == 0:
            continue

        base_positions = list(range(len(repos_in_lang)))

        fig, ax = plt.subplots(figsize=(11, 7.5))

        # Stack bars manually for each repo
        bottoms = np.zeros(len(repos_in_lang))
        width = 0.8
        for ref_type in plot_df.columns:
            heights = plot_df[ref_type].values
            bars = ax.bar(base_positions, heights, bottom=bottoms, width=width, label=ref_type)

            # annotate with percentage inside each stacked segment
            for rect, percentage in zip(bars, heights):
                if percentage > 1:  # Use a threshold to avoid clutter
                    ax.text(rect.get_x() + rect.get_width() / 2,
                            rect.get_y() + rect.get_height() / 2,
                            f"{percentage:.1f}", ha='center', va='center', color='white', fontweight='bold', fontsize=16)

            bottoms += heights

        # tighten x-limits without changing bar width
        if base_positions:
            pad = 0.2
            ax.set_xlim(min(base_positions) - width / 2 - pad,
                        max(base_positions) + width / 2 + pad)

        # set x tick labels at base positions (repo only)
        ax.set_xticks(base_positions)
        ax.set_xticklabels(repos_in_lang, rotation=60, ha='center')

        plt.title('')
        plt.xlabel('Project')
        plt.ylabel('Percentage (%)')

        # Match dist.py legend layout/order
        handles, labels = ax.get_legend_handles_labels()
        labels = [REFACTORING_DISPLAY.get(l, l) for l in labels]
        handles = list(reversed(handles))
        labels = list(reversed(labels))
        plt.legend(
            handles,
            labels,
            loc='upper left',
            bbox_to_anchor=(1.02, 1),
        )
        plt.tight_layout()
        plt.subplots_adjust(right=0.78)

        out_path = OUT_PATH_TEMPLATE.format(lang=lang, ts=ts)
        ensure_parent_dir(out_path)
        plt.savefig(out_path, bbox_inches='tight')
        plt.close(fig)
        saved_paths.append(out_path)

    if not saved_paths:
        print("No refactoring types to plot.")
        exit(0)

    for out_path in saved_paths:
        print(f"Plot saved to {out_path}")
