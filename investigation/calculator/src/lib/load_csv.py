import pandas as pd
import glob
import os

"""
read all csv files from base_path/<language>/*.csv
return a DataFrame containing all records with additional columns 'Lang' and 'Repo'
"""
def load_csv_files(base_path):
    languages = ["java", "c", "javascript", "python", "go", "php", "ruby"]
    column_names = [
        "Commit",
        "RefactoringType",
        "Before",
        "After",
        "BeforeLOC",
        "AfterLOC",
        "Lang",
        "Repo",
    ]

    all_records = []
    for lang in languages:
        path = os.path.join(base_path, lang)
        csv_files = glob.glob(os.path.join(path, "*.csv"))
        if not csv_files:
            print(f"No CSV files found for {lang}")
            continue

        for file in csv_files:
            repo_name = os.path.splitext(os.path.basename(file))[
                0
            ]  # repo_name-MMDD-HHMM
            try:
                df_file = pd.read_csv(
                    file, header=None, names=column_names, dtype=str, encoding="utf-8"
                )
                df_file = df_file.fillna("")  # Fill NaN with empty string
                df_file["Lang"] = lang
                df_file["Repo"] = repo_name
                all_records.append(df_file)
            except Exception as e:
                print(f"Error reading {file}: {e}")
    return pd.concat(all_records, ignore_index=True)
