package refdiff.parsers.universal;

import refdiff.parsers.universal.c.CParser;
import refdiff.parsers.universal.common.Parser;
import refdiff.parsers.universal.go.GoParser;
import refdiff.parsers.universal.java.JavaParser;
import refdiff.parsers.universal.js.JsParser;
import refdiff.parsers.universal.php.PhpParser;
import refdiff.parsers.universal.python.PythonParser;
import refdiff.parsers.universal.ruby.RubyParser;
import refdiff.parsers.universal.ts.TsParser;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public enum Language {
  JAVA(JavaParser::new, ".java"),
  C(CParser::new, ".c", ".h"),
  JAVASCRIPT(JsParser::new, ".js", ".jsx"),
  RUBY(RubyParser::new, ".rb"),
  GO(GoParser::new, ".go"),
  PYTHON(PythonParser::new, ".py"),
  PHP(PhpParser::new, ".php"),
  TS(TsParser::new, ".ts"),
  CS(() -> { throw new UnsupportedOperationException("C# is not supported yet."); }, ".cs"),
  CPP(() -> { throw new UnsupportedOperationException("C++ is not supported yet."); }, ".cpp", ".hpp");

  private final Supplier<Parser> parserSupplier;
  private final List<String> extensions;

  Language(Supplier<Parser> parserSupplier, String... extensions) {
    this.parserSupplier = parserSupplier;
    this.extensions = Arrays.asList(extensions);
  }

  public Parser createParser() {
    return parserSupplier.get();
  }

  public List<String> getExtensions() {
    return extensions;
  }

  public static Language fromFilePath(String filePath) {
    return Stream.of(values())
        .filter(lang -> lang.getExtensions().stream().anyMatch(filePath::endsWith))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported file type: " + filePath));
  }
}