import pandas as pd
import glob
import os
import csv

OUT_PATH = './output/refactoring_distribution.png'

def load_csv_files():
    base_path = '../result'
    languages = ['c', 'java', 'javascript', 'php', 'python', 'ruby', 'go']
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

    dataframes, commits = load_csv_files()



    # Step 1: Calculate percentages and absolute counts
    all_counts = {}
    all_absolute_counts = {}
    allowed_refactoring_types = ['CHANGE_SIGNATURE', 'EXTRACT', 'EXTRACT_MOVE', 'INLINE', 'MOVE', 'RENAME', 'MOVE_RENAME']

    for lang, df in dataframes.items():
        if 'RefactoringType' in df.columns:
            df['RefactoringType'] = df['RefactoringType'].replace(['INTERNAL_MOVE'], 'MOVE')
            df['RefactoringType'] = df['RefactoringType'].replace(['INTERNAL_MOVE_RENAME'], 'MOVE_RENAME')

            # Filter the DataFrame to include only allowed refactoring types
            filtered_df = df[df['RefactoringType'].isin(allowed_refactoring_types)]
            
            # Calculate normalized counts for the filtered DataFrame
            counts = filtered_df['RefactoringType'].value_counts(normalize=True) * 100
            all_counts[lang] = counts

            # Calculate absolute counts
            absolute_counts = filtered_df['RefactoringType'].value_counts()
            all_absolute_counts[lang] = absolute_counts



    # Prepare data for plotting

    plot_data = pd.DataFrame(all_counts).fillna(0)
    absolute_counts_data = pd.DataFrame(all_absolute_counts).fillna(0)



    # Check if there is data to plot

    if not plot_data.empty:

        try:

            import matplotlib.pyplot as plt



            # Transpose for plotting (languages on x-axis)

            ax = plot_data.T.plot(kind='bar', stacked=True, figsize=(10, 7))

            # To ensure the order of counts matches the plot, we align the absolute counts dataframe
            # with the percentage dataframe, which dictates the plot structure.
            aligned_absolute_counts = absolute_counts_data.reindex(index=plot_data.index, columns=plot_data.columns).fillna(0).astype(int)

            # Add counts on the bars
            for container in ax.containers:
                # The label for each container is the refactoring type
                refactoring_type = container.get_label()
                
                # Get the counts for this type across all languages from the aligned absolute counts
                if refactoring_type in aligned_absolute_counts.index:
                    labels = aligned_absolute_counts.loc[refactoring_type].values
                    
                    # Create labels only for non-zero bars to avoid clutter
                    display_labels = [f'{v}' if v > 0 else '' for v in labels]
                    
                    ax.bar_label(container, labels=display_labels, label_type='center', color='white', weight='bold')


            plt.title('Distribution of Refactoring Types by Language')

            plt.xlabel('Language')

            plt.ylabel('Percentage (%)')

            plt.xticks(rotation=0)

            plt.legend(title='Refactoring Type', bbox_to_anchor=(1.05, 1), loc='upper left')

            plt.tight_layout()



            # Save the plot
            ensure_parent_dir(OUT_PATH)
            plt.savefig(OUT_PATH)

            print(f"Plot saved to {OUT_PATH}")



        except ImportError:

            print("Matplotlib is not installed. Please install it using 'pip install matplotlib'")

    else:

        print("No data available to plot.")
