package refdiff.parsers.universal.common;

import refdiff.core.cst.CstRoot;
import refdiff.core.io.SourceFileSet;

public interface Parser {

  CstRoot parse(SourceFileSet sources) throws Exception;

}
