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

public class UniversalPlugin implements LanguagePlugin {

  private enum Language {
    JAVA, C, JAVASCRIPT, RUBY, GO, PYTHON
  }

  @Override
  public CstRoot parse(SourceFileSet sources) throws Exception {
    String firstFilePath = sources.getSourceFiles().get(0).getPath(); // TODO 最初のファイルのパスを取得する方法を改善
    Language language = getLanguageByFileExtension(firstFilePath);
    switch (language) {
      case JAVA: {
        JavaParser javaParser = new JavaParser();
        return javaParser.parse(sources);
      }
      case C:
        CParser cParser = new CParser();
        return cParser.parse(sources);
      case JAVASCRIPT:
        JsParser jsParser = new JsParser();
        return jsParser.parse(sources);
      case RUBY:
        RubyParser rubyParser = new RubyParser();
        return rubyParser.parse(sources);
      case GO:
        GoParser goParser = new GoParser();
        return goParser.parse(sources);
      case PYTHON:
        PythonParser pythonParser = new PythonParser();
        return pythonParser.parse(sources);
      default:
        throw new IllegalArgumentException("Unsupported language: " + language);
    }
  }

  @Override
  public FilePathFilter getAllowedFilesFilter() {
    return new FilePathFilter(Arrays.asList(".java", ".c", ".js", ".rb", ".go", ".py"));
  }

  private Language getLanguageByFileExtension(String filePath) {
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
    } else {
      throw new IllegalArgumentException("Unsupported file type: " + filePath);
    }
  }

}
