# Run with gradle
```bash
./gradlew runInvestigation
./gradlew runInvestigation --args='--resume'
./gradlew runInvestigation --args='-l <language>'
./gradlew runInvestigation --args='-l <language> -r <repo_url>'
```

# Build distribution
```bash
./gradlew installDist
```

# Run with script
Premise: Make sure tmux is installed.
```bash
bash ./script/investigation.sh
```