```bash
./gradlew runEvaluation --args='java'
./gradlew runEvaluation --args='c precision'
./gradlew runEvaluation --args='js recall'
```

For a specific directory:
- Switch boolean flag `IS_FOR_REPO`
- Modify the path in `COMMIT_PATH`
- Switch LANGUAGE in `LANG`
- Set the desired LanguagePlugin

```bash
./gradlew runDebug
```
