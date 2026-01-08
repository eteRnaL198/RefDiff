package validation;

import plugingenerator.Language;
import refdiff.parsers.LanguagePlugin;
import refdiff.parsers.universal.c.CPlugin;
import refdiff.parsers.universal.c.CNodeTypes;

public class CValidator extends BaseValidator {

  @Override
  protected LanguagePlugin getPlugin() {
    return new CPlugin();
  }

  @Override
  protected Language getLanguage() {
    return Language.C;
  }

  @Override
  protected String[] getRepos() {
    return new String[] {
      // "linux",
      // "scrcpy",
      "netdata",
      "Ventoy",
      "redis",
    };
  }

  @Override
  protected boolean isTypeEqual(String tagKind, String nodeType) {
    if (tagKind.equalsIgnoreCase("file")) {
      return CNodeTypes.FILE.equals(nodeType);
    }
    if (tagKind.equalsIgnoreCase("function")) {
      return CNodeTypes.FUNCTION.equals(nodeType);
    }
    return false;
  }

  public static void main(String[] args) throws Exception {
    CValidator validator = new CValidator();
    validator.run();
  }

}
