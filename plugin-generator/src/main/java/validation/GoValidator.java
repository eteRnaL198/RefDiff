package validation;

import plugingenerator.Language;
import refdiff.parsers.LanguagePlugin;
import refdiff.parsers.universal.go.GoParser;
import refdiff.parsers.universal.go.GoNodeTypes;

public class GoValidator extends BaseValidator {

  @Override
  protected LanguagePlugin getPlugin() {
    return new GoParser();
  }

  @Override
  protected Language getLanguage() {
    return Language.GO;
  }

  @Override
  protected String[] getRepos() {
    return new String[] {
        "ollama",
        "go",
        "kubernetes",
        "frp",
        "gin",
    };
  }

  @Override
  protected boolean isTypeEqual(String tagKind, String nodeType) {
    if (tagKind.equalsIgnoreCase("file")) {
      return GoNodeTypes.FILE.equals(nodeType);
    }
    if (tagKind.equalsIgnoreCase("function")) {
      return GoNodeTypes.FUNCTION.equals(nodeType);
    }
    if (tagKind.equalsIgnoreCase("method")) {
      return GoNodeTypes.METHOD.equals(nodeType);
    }
    return false;
  }

  public static void main(String[] args) throws Exception {
    GoValidator validator = new GoValidator();
    validator.run();
  }

}
