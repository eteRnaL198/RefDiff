package validation;

import plugingenerator.Language;
import refdiff.parsers.LanguagePlugin;
import refdiff.parsers.universal.java.JavaPlugin;
import refdiff.parsers.universal.java.JavaNodeTypes;

public class JavaValidator extends BaseValidator {

  @Override
  protected LanguagePlugin getPlugin() {
    return new JavaPlugin();
  }

  @Override
  protected Language getLanguage() {
    return Language.JAVA;
  }

  @Override
  protected String[] getRepos() {
    return new String[] {
      // "mall",
      // "spring-boot",
      "elasticsearch",
      "ghidra",
      "spring-framework",
    };
  }

  @Override
  protected boolean isTypeEqual(String tagKind, String nodeType) {
    if (tagKind.equalsIgnoreCase("class")) {
      return JavaNodeTypes.CLASS.equals(nodeType);
    }
    if (tagKind.equalsIgnoreCase("interface")) {
      return JavaNodeTypes.INTERFACE.equals(nodeType);
    }
    if (tagKind.equalsIgnoreCase("enum")) {
      return JavaNodeTypes.ENUM.equals(nodeType);
    }
    if (tagKind.equalsIgnoreCase("method")) {
      return JavaNodeTypes.METHOD.equals(nodeType);
    }
    return false;
  }

  public static void main(String[] args) throws Exception {
    JavaValidator validator = new JavaValidator();
    validator.run();
  }

}
