# RefDiff-n

RefDiff-n is a refactoring detection tool applicable to diverse programming languages, built on top of [RefDiff](https://github.com/aserg-ufmg/RefDiff).
It mitigates RefDiff's language dependence so that refactorings can be detected uniformly across languages.
Plugins are currently provided for seven languages: **Java, C, JavaScript, PHP, Python, Go, and Ruby**.

Key design points:

- **Parsing**: a single parsing infrastructure (Tree-sitter) replaces per-language parsers.
- **Definition extraction**: the only language-specific part is a set of declarative Tree-sitter queries that convert syntax trees into RefDiff's language-independent Code Structure Tree (CST).
- **Call graph construction**: lightweight string matching between method/function names and tokens, avoiding per-language semantic analysis.
- **LLM-assisted plugin implementation**: tests and queries for a new language are generated with an LLM (Gemini) against a Universal Ctags pseudo-oracle; see `GEMINI.md` and `plugin-generator/`.

## Repository layout

| Directory | Contents |
|---|---|
| `refdiff-core` | Detection core (from RefDiff) |
| `refdiff-universal` | Language-common plugin (Tree-sitter based) with per-language queries and tests |
| `plugin-generator` | Assets for LLM-assisted plugin generation (procedure documented in `GEMINI.md`) |
| `evaluation` | Accuracy evaluation harness (precision/recall on Java; `evaluation/oracle` contains the RefDiff evaluation datasets) |
| `investigation` | Empirical study harness (refactoring-type distribution and Extract LOC across seven languages) |
| `refdiff-java`, `refdiff-c`, `refdiff-go` | Original RefDiff plugins (kept for comparison) |
| `doc` | Additional documentation |

## Requirements

- git
- JDK 8 or later
- python3 (with venv)

## Reproducing the accuracy evaluation (RQ1)

Precision:

```bash
./gradlew :evaluation:installDist
cd evaluation
bash ./script/detect.sh                      # detection over the sampled Java projects
cd calculator
python3 -m venv venv && venv/bin/pip install -r requirements.txt
venv/bin/python -m src.manual.random         # sample instances per type (seed=42)
# annotate the sampled CSVs with TP/FP in the "manual check result" column, then:
venv/bin/python -m src.manual.count
```

Recall:

```bash
cd evaluation
./build/install/evaluation/bin/evaluation java
cd calculator
venv/bin/python -m src.main -d ../detection-result/java/java-<timestamp>.csv
```

## Reproducing the empirical study (RQ2/RQ3)

```bash
./gradlew :investigation:installDist
cd investigation
bash ./script/investigate.sh                 # detection over the 69 subject projects
# The number of commits analyzed per project is configured by COMMIT_DEPTH in
# investigation/src/main/java/investigation/Executor.java.
cd calculator
python3 -m venv venv && venv/bin/pip install -r requirements.txt
venv/bin/python -m src.rq2.dist              # RQ2: distribution per language
venv/bin/python -m src.rq2.dist_repo         # RQ2: distribution per repository
# RQ3 requires no additional detection run: the RQ2 results already contain LOC.
venv/bin/python -m src.rq3.loc               # RQ3: LOC of extraction sources/targets
venv/bin/python -m src.rq3.loc_repo          # RQ3: per repository
```

## License

MIT, inherited from RefDiff.
The original RefDiff README is preserved in [README-refdiff.md](README-refdiff.md).
