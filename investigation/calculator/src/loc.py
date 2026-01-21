import pandas as pd
import glob
import os
import csv
import matplotlib.pyplot as plt
import datetime

OUT_BEFORE_PATH = './output/extract_before_loc.png'
OUT_AFTER_PATH = './output/extract_after_loc.png'

def load_csv_files():
    base_path = '../result'
    languages = ["c", "java", "javascript", "php", "python", "ruby", "go"]
    all_dfs = {}
    all_commits = {}

    column_names = ['Commit', 'RefactoringType', 'Before', 'After', 'BeforeLOC', 'AfterLOC']

    for lang in languages:
        path = os.path.join(base_path, lang)
        csv_files = glob.glob(os.path.join(path, '*.csv'))

        if not csv_files:
            print(f'No CSV files found for {lang}')
            continue

        df_list = []
        commit_list = []
        for file in csv_files:
            rows_6_cols = []
            rows_1_col = []
            try:
                with open(file, 'r', newline='', encoding='utf-8') as f:
                    reader = csv.reader(f)
                    for row in reader:
                        if len(row) == 6:
                            rows_6_cols.append(row)
                        elif len(row) == 1:
                            rows_1_col.append(row[0])
            except Exception as e:
                print(f"Error reading {file}: {e}")

            if rows_6_cols:
                df = pd.DataFrame(rows_6_cols, columns=column_names)
                df_list.append(df)

            if rows_1_col:
                commit_list.extend(rows_1_col)

        if df_list:
            all_dfs[lang] = pd.concat(df_list, ignore_index=True)

        if commit_list:
            all_commits[lang] = commit_list

    return all_dfs, all_commits

def ensure_parent_dir(filepath):
    dirpath = os.path.dirname(filepath)
    if dirpath and not os.path.exists(dirpath):
        try:
            os.makedirs(dirpath, exist_ok=True)
        except Exception as e:
            print(f"Could not create directory {dirpath}: {e}")

if __name__ == '__main__':
    dataframes, _ = load_csv_files()

    extract_data_list = []

    for lang, df in dataframes.items():
        if 'RefactoringType' in df.columns and 'BeforeLOC' in df.columns and 'AfterLOC' in df.columns:
            extract_df = df[df['RefactoringType'] == 'EXTRACT'].copy()
            if not extract_df.empty:
                extract_df['Language'] = lang
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

        if not combined_extract_df.empty:
            plt.style.use('ggplot')

            # BeforeLOC boxplot
            fig1, ax1 = plt.subplots(figsize=(12, 8))
            combined_extract_df.boxplot(column='BeforeLOC', by='Language', ax=ax1, showfliers=False, showmeans=True)
            ax1.set_title('BeforeLOC for EXTRACT Refactoring by Language')
            ax1.set_xlabel('Language')
            ax1.set_ylabel('Lines of Code')
            plt.suptitle('')
            plt.tight_layout()
            out_before_path = OUT_BEFORE_PATH.format(ts=datetime.datetime.now().strftime("%m%d_%H-%M"))
            ensure_parent_dir(out_before_path)
            plt.savefig(out_before_path)
            print(f"Saved BeforeLOC boxplot to {out_before_path}")
            plt.close(fig1)

            # AfterLOC boxplot
            fig2, ax2 = plt.subplots(figsize=(12, 8))
            combined_extract_df.boxplot(column='AfterLOC', by='Language', ax=ax2, showfliers=False, showmeans=True)
            ax2.set_title('AfterLOC for EXTRACT Refactoring by Language')
            ax2.set_xlabel('Language')
            ax2.set_ylabel('Lines of Code')
            plt.suptitle('')
            plt.tight_layout()
            out_after_path = OUT_AFTER_PATH.format(ts=datetime.datetime.now().strftime("%m%d_%H-%M"))
            ensure_parent_dir(out_after_path)
            plt.savefig(out_after_path)
            print(f"Saved AfterLOC boxplot to {out_after_path}")
            plt.close(fig2)
        else:
            print("No valid data to plot after cleaning.")
    else:
        print("No 'EXTRACT' refactoring data found to plot.")
