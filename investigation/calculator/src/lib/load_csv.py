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
            repo_name = os.path.splitext(os.path.basename(file))[0].split('-')[0]  # take part before '-'
            df_file = None
            # Try reading normally (let pandas infer header). If that fails, fallback to python engine and skip bad lines.
            try:
                df_file = pd.read_csv(file, dtype=str, encoding="utf-8")
            except Exception as e:
                print(f"Primary read failed for {file}: {e}. Trying python engine with relaxed parsing.")
                try:
                    df_file = pd.read_csv(file, dtype=str, encoding="utf-8", engine="python", on_bad_lines="skip")
                except Exception as e2:
                    print(f"Fallback read failed for {file}: {e2}. Skipping file.")
                    continue

            if df_file is None:
                continue

            df_file = df_file.fillna("")  # Fill NaN with empty string

            # If file had no header and exactly 6 columns, assign expected column names
            if df_file.shape[1] == 6:
                df_file.columns = ["Commit", "RefactoringType", "Before", "After", "BeforeLOC", "AfterLOC"]

            # Ensure required columns exist; add empty columns if missing
            for required in ["Commit", "RefactoringType", "Before", "After", "BeforeLOC", "AfterLOC"]:
                if required not in df_file.columns:
                    df_file[required] = ""

            df_file["Lang"] = lang
            df_file["Repo"] = repo_name
            all_records.append(df_file)
    if not all_records:
        return pd.DataFrame(columns=column_names)
    return pd.concat(all_records, ignore_index=True)
