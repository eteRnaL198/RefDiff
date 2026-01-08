package validation;

import plugingenerator.Language;
import refdiff.parsers.LanguagePlugin;
import refdiff.parsers.universal.php.PhpPlugin;
import refdiff.parsers.universal.php.PhpNodeTypes;

public class PhpValidator extends BaseValidator {

  @Override
  protected LanguagePlugin getPlugin() {
    return new PhpPlugin();
  }

  @Override
  protected Language getLanguage() {
    return Language.PHP;
  }

  @Override
  protected String[] getRepos() {
    return new String[] {
      "coolify",
      "framework",
      "server",
      "symfony",
      "jQuery-File-Upload",
    };
  }

  @Override
  protected boolean isTypeEqual(String tagKind, String nodeType) {
    if (tagKind.equalsIgnoreCase("trait")) {
      return PhpNodeTypes.TRAIT.equals(nodeType);
    } else if (tagKind.equalsIgnoreCase("interface")) {
      return PhpNodeTypes.INTERFACE.equals(nodeType);
    } else if (tagKind.equalsIgnoreCase("function")) {
      return PhpNodeTypes.FUNCTION.equals(nodeType);
    } else if (tagKind.equalsIgnoreCase("method")) {
      return PhpNodeTypes.METHOD.equals(nodeType);
    }

    return false;
  }

  public static void main(String[] args) throws Exception {
    RubyValidator validator = new RubyValidator();
    validator.run();
  }

}
