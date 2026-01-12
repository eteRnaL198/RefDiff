#!/bin/bash

APP_BIN="./build/install/investigation/bin/investigation"

# List of target languages
# languages=("java" "c" "javascript" "go" "php" "python" "ruby" "php")
languages=("java" "c")

for lang in "${languages[@]}"; do
  # Execute each process in the background, redirecting output to separate log files
  $APP_BIN --language $lang --resume  > "logs/out_$lang.log" 2>&1 &
  echo "Started process for $lang (PID: $!)"
done

# Wait for all background processes to complete
wait
echo "All scripts have completed."