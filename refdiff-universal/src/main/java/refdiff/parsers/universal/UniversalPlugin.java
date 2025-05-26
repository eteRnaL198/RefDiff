package refdiff.parsers.universal;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import refdiff.core.cst.CstRoot;
import refdiff.core.io.FilePathFilter;
import refdiff.core.io.SourceFile;
import refdiff.core.io.SourceFileSet;
import refdiff.parsers.LanguagePlugin;
import refdiff.parsers.universal.c.CParser;
import refdiff.parsers.universal.java.JavaParser;

public class UniversalPlugin implements LanguagePlugin {

  private enum Language {
    JAVA, C
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
      default:
        throw new IllegalArgumentException("Unsupported language: " + language);
    }
  }

  @Override
  public FilePathFilter getAllowedFilesFilter() {
    return new FilePathFilter(Arrays.asList(".java", ".c"));
  }

  private Language getLanguageByFileExtension(String filePath) {
    if (filePath.endsWith(".java")) {
      return Language.JAVA;
    } else if (filePath.endsWith(".c")) {
      return Language.C;
    } else {
      throw new IllegalArgumentException("Unsupported file type: " + filePath);
    }
  }

}
