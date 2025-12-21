- Install Universal Ctags CLI
- Add tree sitter dependency to build.gradle
- Set Language
- Add sample file to context/{langage}/src/
- Run Plugin Maker with argument "{language}"
For example, to generate a plugin for C language, run:
```bash
./gradlew :plugin-generator:run --args="c"
```
This will create a AST 