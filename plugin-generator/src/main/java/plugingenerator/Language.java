package plugingenerator;

import java.util.function.Supplier;

import org.treesitter.*;

public enum Language {
  PYTHON("python", ".py", "--kinds-Python=cfm", () -> new TreeSitterPython()),
  GO("go", ".go", "--kinds-Go=f", () -> new TreeSitterGo()),
  PHP("php", ".php", "--php-kinds=f", () -> new TreeSitterPhp()),
  JAVA("java", ".java", "--kinds-Java=pigacm", () -> new TreeSitterJava()),
  TS("ts", ".ts", "--kinds-TypeScript=fmGa", () -> new TreeSitterTypescript());
  // CS("cs", ".cs", "--kinds-CSharp=cfm", () -> new TreeSitterCSharp()),
  // CPP("cpp", ".cpp", "--kinds-C++=cfm", () -> new TreeSitterCpp());

  private final String name;
  private final String extension;
  private final String ctagsOption;
  private final Supplier<TSLanguage> tsSupplier;

  Language(String name, String extension, String ctagsOption, Supplier<TSLanguage> tsSupplier) {
    this.name = name;
    this.extension = extension;
    this.ctagsOption = ctagsOption;
    this.tsSupplier = tsSupplier;
  }

  String getName() {
    return name;
  }

  String getExtension() {
    return extension;
  }

  String getCtagsOption() {
    return ctagsOption;
  }

  TSLanguage getTSLanguage() {
    return tsSupplier.get();
  }

  static Language fromName(String n) {
    for (Language l : values()) {
      if (l.name.equalsIgnoreCase(n))
        return l;
    }
    throw new IllegalArgumentException("Unknown language: " + n);
  }
}