#!/bin/bash

set -e

APP_BIN="./build/install/investigation/bin/investigation"

# "lang|repo_url".
ITEMS=(
  "java|https://github.com/pockethub/pockethub.git" # App
  "java|https://github.com/nostra13/android-universal-image-loader.git" # Non-web library
  "java|https://github.com/elastic/elasticsearch.git" # Software tool
  "java|https://github.com/reactivex/rxjava.git" # System
  "java|https://github.com/bumptech/glide.git" # Web

  "c|https://github.com/ffmpeg/ffmpeg.git" # App
  "c|https://github.com/bilibili/ijkplayer.git" # Non-web library
  "c|https://github.com/firehol/netdata.git" # Software tool
  "c|https://github.com/torvalds/linux.git" # System
  "c|https://github.com/phpredis/phpredis.git" # Web

  "javascript|https://github.com/adobe/brackets.git" # App
  "javascript|https://github.com/moment/moment.git" # Non-web
  "javascript|https://github.com/gulpjs/gulp.git" # Software tool
  "javascript|https://github.com/nodejs/node.git" # System
  "javascript|https://github.com/facebook/react.git" # Web

  "ruby|https://github.com/jekyll/jekyll.git" # App
  "ruby|https://github.com/plataformatec/devise.git" # Non-web library
  "ruby|https://github.com/gitlabhq/gitlabhq.git" # Software tool
  "ruby|https://github.com/ruby/ruby.git" # System
  "ruby|https://github.com/rails/rails.git" # Web

  "go|https://github.com/getlantern/lantern.git" # App
  "go|https://github.com/labstack/echo.git" # Non-web library
  "go|https://github.com/kubernetes/kubernetes.git" # Software tool
  "go|https://github.com/docker/docker.git" # System
  "go|https://github.com/go-martini/martini.git" # Web

  "php|https://github.com/wordpress/wordpress.git" # App
  "php|https://github.com/phpmailer/phpmailer.git" # Non-web library
  "php|https://github.com/composer/composer.git" # Software tool
  "php|https://github.com/thephpleague/oauth2-server.git" # System
  "php|https://github.com/symfony/symfony.git" # Web

  "python|https://github.com/rg3/youtube-dl.git" # App
  "python|https://github.com/scrapy/scrapy.git" # Non-web library
  "python|https://github.com/jkbrzt/httpie.git" # Software tool
  "python|https://github.com/apenwarr/sshuttle.git" # System
  "python|https://github.com/pallets/flask.git" # Web

)

# Ensure logs directory exists
mkdir -p logs

# Maximum concurrent processes (adjustable)
MAX_CONCURRENT=15

# Active background PIDs
PIDS=()

for entry in "${ITEMS[@]}"; do
  # parse entry "lang|repo_url"
  lang="${entry%%|*}"
  repo_url="${entry#*|}"

  if [ -z "$lang" ] || [ -z "$repo_url" ] || [ "$lang" = "$repo_url" ]; then
    echo "Skipping malformed entry: $entry"
    continue
  fi

  repo_name=$(basename "$repo_url" .git)
  safe_repo_name=$(echo "$repo_name" | sed 's/[^a-zA-Z0-9_.-]/_/g')
  log_file="logs/out_${lang}_${safe_repo_name}.log"

  # Launch one process per repository, passing --repo to the application
  "$APP_BIN" --language "$lang" --repo "$repo_url" > "$log_file" 2>&1 &
  pid=$!
  PIDS+=("$pid")
  echo "Started process for $lang repo $repo_url (PID: $pid) -> $log_file"

  # If we've reached the concurrency limit, wait until at least one PID finishes.
  while :; do
    # Prune finished PIDs from PIDS
    active=()
    for p in "${PIDS[@]}"; do
      if kill -0 "$p" >/dev/null 2>&1; then
        active+=("$p")
      fi
    done
    PIDS=("${active[@]}")

    if [ "${#PIDS[@]}" -lt "$MAX_CONCURRENT" ]; then
      break
    fi

    sleep 10
  done
done

# After starting all entries, wait for any remaining background processes
for p in "${PIDS[@]}"; do
  if kill -0 "$p" >/dev/null 2>&1; then
    wait "$p"
  fi
done

echo "All scripts have completed."