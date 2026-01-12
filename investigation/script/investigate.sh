#!/bin/bash

set -e

# If not already running inside tmux, re-run this script inside a new detached tmux session.
# This makes the processes continue running even if the SSH connection is lost.
if [ -z "$TMUX" ] && [ "$1" != "--inside-tmux" ]; then
  if ! command -v tmux >/dev/null 2>&1; then
    echo "tmux is not installed. Running directly (will stop on SSH disconnect)."
  else
    session="investigation_$(date +%s)"
    mkdir -p logs
    tmux new-session -d -s "$session" "$0 --inside-tmux"
    echo "Started tmux session '$session'. Attach with: tmux attach -t $session"
    exit 0
  fi
fi

APP_BIN="./build/install/investigation/bin/investigation"
# Space-separated list of target languages
LANGUAGES="java c javascript go php python ruby"

# Ensure logs directory exists
mkdir -p logs

for lang in $LANGUAGES; do
  # Execute each process in the background, redirecting output to separate log files
  "$APP_BIN" --language "$lang" --resume > "logs/out_$lang.log" 2>&1 &
  echo "Started process for $lang (PID: $!)"
done

# Wait for all background processes to complete
wait
echo "All scripts have completed."