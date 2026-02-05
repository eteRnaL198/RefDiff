import pandas as pd
import os
import matplotlib.pyplot as plt
import datetime
import argparse

from src.lib.load_csv import load_csv_files
from src.lib.file_util import ensure_parent_dir

OUT_PATH = './output/extract_loc_{ts}.pdf'
CACHE_PATH = './output/extract_loc_cache.csv'

CACHE_COLUMNS = ['Lang', 'BeforeLOC', 'AfterLOC']

def build_cache_df():
    df_all = load_csv_files("../result")
    if df_all.empty or 'Lang' not in df_all.columns:
        print("No 'EXTRACT' refactoring data found to plot.")
        return pd.DataFrame()

    extract_data_list = []
    for _, df_lang in df_all.groupby('Lang'):
        if 'RefactoringType' not in df_lang.columns:
            continue
        if 'BeforeLOC' not in df_lang.columns or 'AfterLOC' not in df_lang.columns:
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
    combined_extract_df.dropna(subset=['BeforeLOC', 'AfterLOC'], inplace=True)
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
    parser = argparse.ArgumentParser(description="Plot LOC stats for EXTRACT refactoring.")
    parser.add_argument('--cache', default=CACHE_PATH, help='Path to cache CSV.')
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

    lang_order = ['java', 'c', 'javascript', 'python', 'go', 'php', 'ruby']
    lang_display = {
        'java': 'Java',
        'c': 'C',
        'javascript': 'JS',
        'python': 'Python',
        'go': 'Go',
        'php': 'PHP',
        'ruby': 'Ruby',
    }
    combined_extract_df['Lang'] = pd.Categorical(combined_extract_df['Lang'], categories=lang_order, ordered=True)
    combined_extract_df = combined_extract_df.sort_values('Lang')

    if combined_extract_df.empty:
        print("No valid data to plot after cleaning.")
        return

    stats = (
        combined_extract_df
        .groupby('Lang')
        .agg(
            before_min=('BeforeLOC', 'min'),
            before_q25=('BeforeLOC', lambda s: s.quantile(0.25)),
            before_median=('BeforeLOC', 'median'),
            before_q75=('BeforeLOC', lambda s: s.quantile(0.75)),
            before_max=('BeforeLOC', 'max'),
            before_mean=('BeforeLOC', 'mean'),
            after_min=('AfterLOC', 'min'),
            after_q25=('AfterLOC', lambda s: s.quantile(0.25)),
            after_median=('AfterLOC', 'median'),
            after_q75=('AfterLOC', lambda s: s.quantile(0.75)),
            after_max=('AfterLOC', 'max'),
            after_mean=('AfterLOC', 'mean'),
        )
        .reset_index()
    )
    print("LOC summary by language (mean/median):")
    for _, row in stats.iterrows():
        print(
            f"  {row['Lang']}: "
            f"BeforeLOC mean={row['before_mean']:.2f} "
            f"min={int(row['before_min'])} q25={int(row['before_q25'])} "
            f"median={int(row['before_median'])} q75={int(row['before_q75'])} "
            f"max={int(row['before_max'])}, "
            f"AfterLOC mean={row['after_mean']:.2f} "
            f"min={int(row['after_min'])} q25={int(row['after_q25'])} "
            f"median={int(row['after_median'])} q75={int(row['after_q75'])} "
            f"max={int(row['after_max'])}"
        )

    plt.style.use('ggplot')
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

    # Before/After boxplots in one figure
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(18, 8), sharey=False)
    before_order = (
        combined_extract_df
        .groupby('Lang', observed=False)['BeforeLOC']
        .median()
        .sort_values(ascending=False)
        .index
        .tolist()
    )
    before_order_labels = [lang_display.get(lang, lang) for lang in before_order]
    df_before = combined_extract_df.copy()
    df_before['LangLabel'] = df_before['Lang'].map(lang_display).astype('object')
    df_before['LangLabel'] = df_before['LangLabel'].where(
        df_before['LangLabel'].notna(),
        df_before['Lang'].astype(str),
    )
    df_before['LangLabel'] = pd.Categorical(df_before['LangLabel'], categories=before_order_labels, ordered=True)
    df_before.boxplot(column='BeforeLOC', by='LangLabel', ax=ax1, showfliers=False, showmeans=True)
    ax1.set_title('')
    ax1.set_xlabel('Language')
    ax1.set_ylabel('LOC')
    plt.suptitle('')
    plt.tight_layout()
    after_order = (
        combined_extract_df
        .groupby('Lang', observed=False)['AfterLOC']
        .median()
        .sort_values(ascending=False)
        .index
        .tolist()
    )
    after_order_labels = [lang_display.get(lang, lang) for lang in after_order]
    df_after = combined_extract_df.copy()
    df_after['LangLabel'] = df_after['Lang'].map(lang_display).astype('object')
    df_after['LangLabel'] = df_after['LangLabel'].where(
        df_after['LangLabel'].notna(),
        df_after['Lang'].astype(str),
    )
    df_after['LangLabel'] = pd.Categorical(df_after['LangLabel'], categories=after_order_labels, ordered=True)
    df_after.boxplot(column='AfterLOC', by='LangLabel', ax=ax2, showfliers=False, showmeans=True)
    ax2.set_title('')
    ax2.set_xlabel('Language')
    ax2.set_ylabel('LOC')
    ax2.yaxis.set_label_position('right')
    ax2.yaxis.tick_right()
    plt.suptitle('')
    plt.tight_layout()
    out_path = OUT_PATH.format(ts=datetime.datetime.now().strftime("%m%d_%H-%M"))
    ensure_parent_dir(out_path)
    plt.savefig(out_path)
    print(f"Saved combined LOC boxplots to {out_path}")
    plt.close(fig)


if __name__ == '__main__':
    main()
