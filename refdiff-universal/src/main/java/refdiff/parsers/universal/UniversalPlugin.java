package refdiff.parsers.universal;

import java.util.Arrays;

import refdiff.core.io.FilePathFilter;
import refdiff.core.io.SourceFile;
import refdiff.core.io.SourceFileSet;
import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstRoot;
import refdiff.core.cst.Stereotype;
import refdiff.parsers.LanguagePlugin;

public class UniversalPlugin implements LanguagePlugin {
  @Override
  public CstRoot parse(SourceFileSet sources) throws Exception {
    return null;
  }


  @Override
  public FilePathFilter getAllowedFilesFilter() {
    return new FilePathFilter(Arrays.asList(".java"));
  }
}
