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

public class UniversalPlugin implements LanguagePlugin {

  @Override
  public CstRoot parse(SourceFileSet sources) throws Exception {
    UniversalParser parser = new UniversalParser();
    return parser.parse(sources);
  }

  @Override
  public FilePathFilter getAllowedFilesFilter() {
    return new FilePathFilter(Arrays.asList(".java")); // TODO 言語切替
    // return new FilePathFilter(Arrays.asList(".c"));
  }
}
