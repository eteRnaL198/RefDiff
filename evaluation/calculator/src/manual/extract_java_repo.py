"""Aggregate evaluation CSV by `Commit URL`
Usage:
  python3 evaluation/calculator/src/manual/extract_java_repo.py -i path/to/file.csv -o out.csv
"""

import argparse
import sys
from pathlib import Path
from typing import Optional
from urllib.parse import urlparse

import pandas as pd

DEFAULT_SEED = 42

def main(argv: Optional[list[str]] = None) -> int:
    parser = argparse.ArgumentParser(description="Aggregate CSV by Commit URL and count refactoring types")
    parser.add_argument("--input", "-i", default="../oracle/java/evaluation-data-public.csv", help="Input CSV file (use second line as header)")
    parser.add_argument("--output", "-o", default=None, help="Optional output CSV path; if omitted prints to stdout")
    parser.add_argument("--sample", "-s", type=int, default=20, help="Number of random repositories to output (default: 20)")
    parser.add_argument("--seed", type=int, default=DEFAULT_SEED, help="Optional random seed for reproducible sampling")
    args = parser.parse_args(argv)

    input_path = Path(args.input)
    if not input_path.exists():
        print(f"Input file not found: {input_path}", file=sys.stderr)
        return 2

    try:
        df = pd.read_csv(input_path, header=1)
    except Exception as e:
        print(f"Error reading CSV: {e}", file=sys.stderr)
        return 3

    if df.empty:
        print("Input CSV is empty", file=sys.stderr)
        return 4

    # Ensure the expected column exists
    if "Commit URL" not in df.columns:
        print("CSV does not contain 'Commit URL' column", file=sys.stderr)
        return 5

    # Extract repository from Commit URL (owner/repo)
    def extract_repo(url: str) -> Optional[str]:
        try:
            p = urlparse(url)
            parts = p.path.strip("/").split("/")
            if len(parts) >= 2:
                return f"{parts[0]}/{parts[1]}"
        except Exception:
            return None
        return None

    df["repository"] = df["Commit URL"].astype(str).apply(extract_repo)

    # Get unique repositories (do not count commit rows)
    repos = (
        df["repository"].dropna()
        .astype(str)
        .drop_duplicates()
        .sort_values()
        .reset_index(drop=True)
        .to_frame(name="repository")
    )

    # Convert repository names (owner/repo) to full HTTPS .git URLs
    def to_git_url(repo: str) -> str:
        if not repo:
            return repo
        repo = repo.strip()
        # If already looks like a URL, leave it
        if repo.startswith("http://") or repo.startswith("https://"):
            return repo
        return f"https://github.com/{repo}.git"

    repos["repository"] = repos["repository"].astype(str).apply(to_git_url)

    # Print total unique repositories
    unique_repos = repos["repository"].nunique()
    print(f"Unique repositories: {unique_repos}")

    # Sample repositories randomly (if requested)
    sample_n = args.sample if args.sample and args.sample > 0 else None
    if sample_n is not None:
        sample_n = min(sample_n, len(repos))
        try:
            repos = repos.sample(n=sample_n, random_state=args.seed)
        except ValueError:
            # fallback: if sample fails, keep full list
            pass

    if args.output:
        try:
            repos.to_csv(args.output, index=False)
        except Exception as e:
            print(f"Error writing output CSV: {e}", file=sys.stderr)
            return 6
    else:
        repos.to_csv(sys.stdout, index=False)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
