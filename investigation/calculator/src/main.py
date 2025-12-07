import pandas as pd
import glob
import os
import csv

def load_csv_files():
    base_path = '../result'
    languages = ['C', 'Java', 'JavaScript']
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

if __name__ == '__main__':
    dataframes, commits = load_csv_files()
    for lang, df in dataframes.items():
        print(f"--- {lang} Refactoring Type Counts ---")
        if 'RefactoringType' in df.columns:
            print(df['RefactoringType'].value_counts())
        else:
            print("RefactoringType column not found.")
        print("\n")