#!/bin/bash

# スクリプトを実行するベースディレクトリ
# このディレクトリ内に 'repo-for-analysis' があることを想定しています。
BASE_DIR="."
REPO_ANALYSIS_DIR="$BASE_DIR/repo-for-analysis"

# repo-for-analysis ディレクトリの存在をチェック
if [ ! -d "$REPO_ANALYSIS_DIR" ]; then
  echo "エラー: ディレクトリ '$REPO_ANALYSIS_DIR' が見つかりません。" >&2
  echo "先に 'kyutech.experiment.Analysis' を実行して、リポジトリをクローンしてください。" >&2
  exit 1
fi

echo "'$REPO_ANALYSIS_DIR' 内のリポジトリを処理します..."

# 各リポジトリディレクトリでループ
for repo_path in "$REPO_ANALYSIS_DIR"/*; do
  # ディレクトリかどうかを判定
  if [ -d "$repo_path" ]; then
    echo "処理中: $repo_path"
    
    # サブシェル内でコマンドを実行することで、カレントディレクトリの移動による影響を防ぎます
    (
      cd "$repo_path" || exit 1
      
      # git log コマンドを実行し、結果を commits.txt に出力
      git log -n 200 --format='%H' > commits.txt
      
      if [ $? -eq 0 ]; then
        echo "  -> '$repo_path/commits.txt' を作成しました。"
      else
        echo "  -> エラー: '$repo_path' でgit logの実行に失敗しました。" >&2
      fi
    )
  fi
done

echo "すべての処理が完了しました。"
