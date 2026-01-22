import pandas as pd
import matplotlib.pyplot as plt
import glob
import os
import datetime
import numpy as np

from src.lib.load_csv import load_csv_files
from src.lib.file_util import ensure_parent_dir

OUT_PATH_TEMPLATE = './output/refactoring_dist_repo_{ts}.pdf'

if __name__ == '__main__':
    df = load_csv_files("../result")

    # Step 1: Calculate percentages and absolute counts
    all_counts = {}
    all_absolute_counts = {}
    allowed_refactoring_types = ['CHANGE_SIGNATURE', 'EXTRACT', 'EXTRACT_MOVE', 'INLINE', 'MOVE', 'RENAME', 'MOVE_RENAME']

    for repo, df_repo in df.groupby('Repo'):
        df_repo = df_repo.copy()
        if 'RefactoringType' in df_repo.columns:
            df_repo['RefactoringType'] = df_repo['RefactoringType'].replace(['INTERNAL_MOVE'], 'MOVE')
            df_repo['RefactoringType'] = df_repo['RefactoringType'].replace(['INTERNAL_MOVE_RENAME'], 'MOVE_RENAME')

            # Filter to allowed refactoring types
            filtered_df = df_repo[df_repo['RefactoringType'].isin(allowed_refactoring_types)]

            # Percentages and absolute counts per repo
            counts = filtered_df['RefactoringType'].value_counts(normalize=True) * 100
            all_counts[repo] = counts

            absolute_counts = filtered_df['RefactoringType'].value_counts()
            all_absolute_counts[repo] = absolute_counts


    # Prepare data for plotting
    plot_data = pd.DataFrame(all_counts).fillna(0)
    absolute_counts_data = pd.DataFrame(all_absolute_counts).fillna(0)

    # Check if there is data to plot
    if plot_data.empty:
        print("No data available to plot.")
        exit(0)

    # order repos by language then repo name
    repo_lang = df[['Repo', 'Lang']].drop_duplicates().set_index('Repo')['Lang'].to_dict()
    ordered_repos = sorted(repo_lang.keys(), key=lambda r: (repo_lang.get(r, ''), r))

    # Rows: repos (ordered_repos), Columns: refactoring types
    plot_df = plot_data.reindex(columns=ordered_repos).T.fillna(0)

    # Align absolute counts with the plot structure
    aligned_absolute_counts = absolute_counts_data.reindex(index=plot_data.index, columns=ordered_repos).fillna(0).astype(int)

    # Base x positions with extra gap between language groups
    base_positions = []
    x = 0.0
    gap = 0.6
    for i, repo in enumerate(ordered_repos):
        base_positions.append(x)
        if i < len(ordered_repos) - 1 and repo_lang.get(ordered_repos[i + 1]) != repo_lang.get(repo):
            x += 1 + gap
        else:
            x += 1

    n_types = len(plot_df.columns)
    if n_types == 0:
        print("No refactoring types to plot.")
        exit(0)

    fig, ax = plt.subplots(figsize=(28, 8))

    # Stack bars manually for each repo
    bottoms = np.zeros(len(ordered_repos))
    width = 0.8
    for j, ref_type in enumerate(plot_df.columns):
        heights = plot_df[ref_type].values
        bars = ax.bar(base_positions, heights, bottom=bottoms, width=width, label=ref_type)

        # annotate with absolute counts inside each stacked segment
        if ref_type in aligned_absolute_counts.index:
            counts = aligned_absolute_counts.loc[ref_type].reindex(ordered_repos).fillna(0).astype(int).values
            for rect, cnt in zip(bars, counts):
                if cnt > 0 and rect.get_height() > 0:
                    ax.text(rect.get_x() + rect.get_width() / 2,
                            rect.get_y() + rect.get_height() / 2,
                            str(cnt), ha='center', va='center', color='black', fontweight='bold', fontsize=8)

        bottoms += heights

    # set x tick labels at base positions
    labels = [f"{repo}\n({repo_lang.get(repo,'')})" for repo in ordered_repos]
    ax.set_xticks(base_positions)
    ax.set_xticklabels(labels, rotation=60, ha='center')

    plt.title('Distribution of Refactoring Types by Repo')
    plt.xlabel('Repo (language in parenthesis)')
    plt.ylabel('Percentage (%)')

    # Place legend as a single horizontal row below the plot (use figure legend to avoid clipping)
    ncol = max(1, len(plot_df.columns))
    handles, labels = ax.get_legend_handles_labels()
    fig.legend(handles, labels, title='Refactoring Type', ncol=ncol,
               loc='lower center', bbox_to_anchor=(0.5, 0.02), bbox_transform=fig.transFigure)
    # Make room at the bottom for the legend
    plt.subplots_adjust(bottom=0.18)
    plt.tight_layout()

    # Save the plot (include month-day and hour-minute, no year)
    out_path = OUT_PATH_TEMPLATE.format(ts=datetime.datetime.now().strftime("%m-%d_%H-%M"))
    ensure_parent_dir(out_path)
    plt.savefig(out_path)

    print(f"Plot saved to {out_path}")
