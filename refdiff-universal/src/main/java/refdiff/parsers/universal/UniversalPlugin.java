package refdiff.parsers.universal;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import refdiff.core.cst.CstRoot;
import refdiff.core.io.FilePathFilter;
import refdiff.core.io.SourceFileSet;
import refdiff.parsers.LanguagePlugin;
import refdiff.parsers.universal.common.Parser;

public class UniversalPlugin implements LanguagePlugin {
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

    Parser parser = language.createParser();
    return parser.parse(sources);
  }

  @Override
  public FilePathFilter getAllowedFilesFilter() {
    if (this.language == null) {
      List<String> allExtensions = Stream.of(Language.values())
          .flatMap(lang -> lang.getExtensions().stream())
          .collect(Collectors.toList());
      return new FilePathFilter(allExtensions);
    } else {
      return new FilePathFilter(language.getExtensions());
    }
  }

  private Language getLanguageByFileExtension(SourceFileSet sources) {
    String filePath = sources.getSourceFiles().get(0).getPath();
    return Language.fromFilePath(filePath);
  }

}
