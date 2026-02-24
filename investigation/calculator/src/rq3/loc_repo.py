import pandas as pd
import os
import matplotlib
import matplotlib.pyplot as plt
import datetime
import argparse

from src.lib.load_csv import load_csv_files
from src.lib.file_util import ensure_parent_dir

OUT_PATH = './output/extract_repo/extract_loc_repo_{lang}_{ts}.pdf'
CACHE_PATH = './output/extract_loc_repo_cache.csv'

CACHE_COLUMNS = ['Lang', 'Repo', 'BeforeLOC', 'AfterLOC']


def build_cache_df():
    df_all = load_csv_files("../result")
    if df_all.empty or 'Lang' not in df_all.columns:
        print("No 'EXTRACT' refactoring data found to plot.")
        return pd.DataFrame()

    extract_data_list = []
    for _, df_lang in df_all.groupby('Lang'):
        if 'RefactoringType' not in df_lang.columns:
            continue
        if 'Repo' not in df_lang.columns or 'BeforeLOC' not in df_lang.columns or 'AfterLOC' not in df_lang.columns:
            continue
        extract_df = df_lang[df_lang['RefactoringType'] == 'EXTRACT'].copy()
        extract_data_list.append(extract_df)

    if not extract_data_list:
        print("No 'EXTRACT' refactoring data found to plot.")
        return pd.DataFrame()

    combined_extract_df = pd.concat(extract_data_list, ignore_index=True)
    combined_extract_df = combined_extract_df[CACHE_COLUMNS].copy()
    combined_extract_df['BeforeLOC'] = pd.to_numeric(combined_extract_df['BeforeLOC'], errors='coerce')
    combined_extract_df['AfterLOC'] = pd.to_numeric(combined_extract_df['AfterLOC'], errors='coerce')
    combined_extract_df.dropna(subset=['Repo', 'BeforeLOC', 'AfterLOC'], inplace=True)
    combined_extract_df['BeforeLOC'] = combined_extract_df['BeforeLOC'].astype(int)
    combined_extract_df['AfterLOC'] = combined_extract_df['AfterLOC'].astype(int)
    return combined_extract_df


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


def main():
    parser = argparse.ArgumentParser(description="Plot LOC-by-repo stats for EXTRACT refactoring.")
    parser.add_argument('--cache', default=CACHE_PATH, help='Path to cache CSV (shared with loc.py).')
    parser.add_argument('--rebuild-cache', action='store_true', help='Rebuild cache from raw results.')
    parser.add_argument('--cache-only', action='store_true', help='Only build cache and exit.')
    args = parser.parse_args()

    combined_extract_df = None
    if not args.rebuild_cache:
        combined_extract_df = load_cache_df(args.cache)
        if combined_extract_df is not None:
            print(f"Loaded cache from {args.cache} ({len(combined_extract_df)} rows)")

    if combined_extract_df is None:
        combined_extract_df = build_cache_df()
        if combined_extract_df.empty:
            print("No valid data to plot after cleaning.")
            return
        save_cache_df(combined_extract_df, args.cache)

    if args.cache_only:
        return

    if combined_extract_df.empty:
        print("No valid data to plot after cleaning.")
        return

    plt.style.use('ggplot')
    matplotlib.rc('pdf', fonttype=42)
    plt.rcParams.update({
        'font.size': 24,
        'axes.titlesize': 24,
        'axes.labelsize': 24,
        'xtick.labelsize': 24,
        'ytick.labelsize': 24,
        'legend.fontsize': 24,
        'text.color': 'black',
        'axes.labelcolor': 'black',
        'xtick.color': 'black',
        'ytick.color': 'black',
    })
    lang_order = ['java', 'c', 'javascript', 'python', 'go', 'php', 'ruby']
    ts = datetime.datetime.now().strftime("%m%d_%H-%M")

    for lang in lang_order:
        lang_df = combined_extract_df[combined_extract_df['Lang'] == lang].copy()
        if lang_df.empty:
            continue

        ordered_repos = sorted(lang_df['Repo'].dropna().unique().tolist())
        if not ordered_repos:
            continue

        fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(24, 10), sharey=False)
        labels = ordered_repos

        lang_df.boxplot(
            column='BeforeLOC',
            by='Repo',
            ax=ax1,
            showfliers=False,
            showmeans=True,
            positions=range(len(ordered_repos)),
        )
        ax1.set_title('Extraction Target Method')
        ax1.set_xlabel('Project')
        ax1.set_ylabel('Lines of Code')
        ax1.set_xticks(range(len(ordered_repos)))
        ax1.set_xticklabels(labels, rotation=60, ha='center')

        lang_df.boxplot(
            column='AfterLOC',
            by='Repo',
            ax=ax2,
            showfliers=False,
            showmeans=True,
            positions=range(len(ordered_repos)),
        )
        ax2.set_title("Newly Extracted Method")
        ax2.set_xlabel('Project')
        ax2.set_ylabel('Lines of Code')
        ax2.set_xticks(range(len(ordered_repos)))
        ax2.set_xticklabels(labels, rotation=60, ha='center')

        fig.suptitle('')
        plt.tight_layout()
        out_path = OUT_PATH.format(lang=lang, ts=ts)
        ensure_parent_dir(out_path)
        fig.savefig(out_path, bbox_inches='tight')
        print(f"Saved LOC boxplot for {lang} to {out_path}")
        plt.close(fig)


if __name__ == '__main__':
    main()
