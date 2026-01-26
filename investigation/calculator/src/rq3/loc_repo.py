import pandas as pd
import glob
import os
import csv
import matplotlib.pyplot as plt
import datetime
import numpy as np

from src.lib.load_csv import load_csv_files
from src.lib.file_util import ensure_parent_dir

OUT_BEFORE_PATH = './output/extract_before_loc_repo_{ts}.pdf'
OUT_AFTER_PATH = './output/extract_after_loc_repo_{ts}.pdf'

BIN_SIZE = 5
MAX_BINS = 200

def _bin_index(loc_value):
    loc_int = int(loc_value)
    if loc_int < 0:
        loc_int = 0
    return min(loc_int // BIN_SIZE, MAX_BINS - 1)

def filter_abnormal_loc(df, loc_col):
    group_cols = ['Repo', 'Commit', 'Before', 'After']
    bin_col = f"{loc_col}_bin"

    df = df.copy()
    df[bin_col] = df[loc_col].apply(_bin_index)

    stats = []
    for key, grp in df.groupby(group_cols):
        counts = grp[bin_col].value_counts()
        total = int(counts.sum())
        max_count = int(counts.max()) if total > 0 else 0
        max_bins = set(counts[counts == max_count].index.tolist()) if total > 0 else set()
        p = (max_count / total) if total > 0 else 0.0
        stats.append((key, total, max_count, p, max_bins))

    p_values = [s[3] for s in stats]
    if not p_values:
        return df.drop(columns=[bin_col]), 0, None

    threshold = float(np.percentile(p_values, 99.5))

    flagged = {}
    for key, total, max_count, p, max_bins in stats:
        if total >= 30 and max_count >= 10 and p >= threshold:
            flagged[key] = max_bins

    if not flagged:
        return df.drop(columns=[bin_col]), 0, threshold

    def should_drop(row):
        key = (row['Repo'], row['Commit'], row['Before'], row['After'])
        bins = flagged.get(key)
        if not bins:
            return False
        return row[bin_col] in bins

    drop_mask = df.apply(should_drop, axis=1)
    dropped = int(drop_mask.sum())
    df = df.loc[~drop_mask].drop(columns=[bin_col])
    return df, dropped, threshold

if __name__ == '__main__':
    df_all = load_csv_files("../result")

    extract_data_list = []

    if not df_all.empty and 'Lang' in df_all.columns:
        for lang, df_lang in df_all.groupby('Lang'):
            if 'RefactoringType' in df_lang.columns and 'BeforeLOC' in df_lang.columns and 'AfterLOC' in df_lang.columns:
                extract_df = df_lang[df_lang['RefactoringType'] == 'EXTRACT'].copy()
                extract_data_list.append(extract_df)

    if extract_data_list:
        combined_extract_df = pd.concat(extract_data_list, ignore_index=True)

        combined_extract_df['BeforeLOC'] = pd.to_numeric(combined_extract_df['BeforeLOC'], errors='coerce')
        combined_extract_df['AfterLOC'] = pd.to_numeric(combined_extract_df['AfterLOC'], errors='coerce')

        combined_extract_df.dropna(subset=['BeforeLOC', 'AfterLOC'], inplace=True)

        # Clip BeforeLOC to a maximum.
        # combined_extract_df['BeforeLOC'] = combined_extract_df['BeforeLOC'].clip(upper=200)

        combined_extract_df['BeforeLOC'] = combined_extract_df['BeforeLOC'].astype(int)
        combined_extract_df['AfterLOC'] = combined_extract_df['AfterLOC'].astype(int)

        combined_extract_df, dropped_before, thr_before = filter_abnormal_loc(combined_extract_df, 'BeforeLOC')
        combined_extract_df, dropped_after, thr_after = filter_abnormal_loc(combined_extract_df, 'AfterLOC')
        if thr_before is not None:
            print(f"Filtered BeforeLOC rows: {dropped_before} (p>=P99.5={thr_before:.4f})")
        if thr_after is not None:
            print(f"Filtered AfterLOC rows: {dropped_after} (p>=P99.5={thr_after:.4f})")

        if not combined_extract_df.empty:
            plt.style.use('ggplot')

            # Order repos by language then repo name
            repo_lang = combined_extract_df[['Repo', 'Lang']].drop_duplicates().set_index('Repo')['Lang'].to_dict()
            ordered_repos = sorted(repo_lang.keys(), key=lambda r: (repo_lang.get(r, ''), r))

            # BeforeLOC boxplot by repo
            fig1, ax1 = plt.subplots(figsize=(28, 10))
            combined_extract_df.boxplot(column='BeforeLOC', by='Repo', ax=ax1, showfliers=False, showmeans=True,
                                        positions=range(len(ordered_repos)))
            ax1.set_title('LOC for EXTRACT Refactoring by Project (Before)')
            ax1.set_xlabel('Project (language in parenthesis)')
            ax1.set_ylabel('Lines of Code')
            labels = [f"{repo}\n({repo_lang.get(repo,'')})" for repo in ordered_repos]
            ax1.set_xticks(range(len(ordered_repos)))
            ax1.set_xticklabels(labels, rotation=60, ha='center')
            plt.suptitle('')
            plt.tight_layout()
            out_before_path = OUT_BEFORE_PATH.format(ts=datetime.datetime.now().strftime("%m%d_%H-%M"))
            ensure_parent_dir(out_before_path)
            plt.savefig(out_before_path, bbox_inches='tight')
            print(f"Saved BeforeLOC boxplot to {out_before_path}")
            plt.close(fig1)

            # AfterLOC boxplot by repo
            fig2, ax2 = plt.subplots(figsize=(28, 10))
            combined_extract_df.boxplot(column='AfterLOC', by='Repo', ax=ax2, showfliers=False, showmeans=True,
                                        positions=range(len(ordered_repos)))
            ax2.set_title('LOC for EXTRACT Refactoring by Project (After)')
            ax2.set_xlabel('Project (language in parenthesis)')
            ax2.set_ylabel('Lines of Code')
            ax2.set_xticks(range(len(ordered_repos)))
            ax2.set_xticklabels(labels, rotation=60, ha='center')
            plt.suptitle('')
            plt.tight_layout()
            out_after_path = OUT_AFTER_PATH.format(ts=datetime.datetime.now().strftime("%m%d_%H-%M"))
            ensure_parent_dir(out_after_path)
            plt.savefig(out_after_path, bbox_inches='tight')
            print(f"Saved AfterLOC boxplot to {out_after_path}")
            plt.close(fig2)
        else:
            print("No valid data to plot after cleaning.")
    else:
        print("No 'EXTRACT' refactoring data found to plot.")
