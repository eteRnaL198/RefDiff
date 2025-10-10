package refdiff.parsers.universal;

import java.util.Arrays;

import refdiff.core.cst.CstRoot;
import refdiff.core.io.FilePathFilter;
import refdiff.core.io.SourceFileSet;
import refdiff.parsers.LanguagePlugin;
import refdiff.parsers.universal.c.CParser;
import refdiff.parsers.universal.java.JavaParser;
import refdiff.parsers.universal.js.JsParser;
import refdiff.parsers.universal.python.PythonParser;
import refdiff.parsers.universal.ruby.RubyParser;
import refdiff.parsers.universal.go.GoParser;
import refdiff.parsers.universal.php.PhpParser;
import refdiff.parsers.universal.common.Parser;


public class UniversalPlugin implements LanguagePlugin {

  private enum Language {
    JAVA, C, JAVASCRIPT, RUBY, GO, PYTHON, PHP
  }
  private Language language;

  public UniversalPlugin() {
    this.language = null;
  }

  public UniversalPlugin(Language lang) {
    this.language = lang;
  }

  @Override
  public CstRoot parse(SourceFileSet sources) throws Exception {
    if (this.language == null) {
      this.language = getLanguageByFileExtension(sources);
    }

    Parser parser;
    switch (language) {
      case JAVA:
        parser = new JavaParser();
        break;
      case C:
        parser = new CParser();
        break;
      case JAVASCRIPT:
        parser = new JsParser();
        break;
      case RUBY:
        parser = new RubyParser();
        break;
      case GO:
        parser = new GoParser();
        break;
      case PYTHON:
        parser = new PythonParser();
        break;
      case PHP:
        parser = new PhpParser();
        break;
      default:
        throw new IllegalArgumentException("Unsupported language: " + language);
    }
    return parser.parse(sources);
  }

  @Override
  public FilePathFilter getAllowedFilesFilter() {
    return new FilePathFilter(Arrays.asList(".java", ".c", ".js", ".rb", ".go", ".py", ".php"));
  }

  private Language getLanguageByFileExtension(SourceFileSet sources) {
    String filePath = sources.getSourceFiles().get(0).getPath(); // TODO 最初のファイルのパスを取得する方法を改善
    if (filePath.endsWith(".java")) {
      return Language.JAVA;
    } else if (filePath.endsWith(".c")) {
      return Language.C;
    } else if (filePath.endsWith(".js")) {
      return Language.JAVASCRIPT;
    } else if (filePath.endsWith(".rb")) {
      return Language.RUBY;
    } else if (filePath.endsWith(".go")) {
      return Language.GO;
    } else if (filePath.endsWith(".py")) {
      return Language.PYTHON;
    } else if (filePath.endsWith(".php")) {
      return Language.PHP;
    } else {
      throw new IllegalArgumentException("Unsupported file type: " + filePath);
    }
  }

}
