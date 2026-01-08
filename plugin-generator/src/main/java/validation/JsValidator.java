package validation;

import plugingenerator.Language;
import refdiff.parsers.LanguagePlugin;
import refdiff.parsers.universal.js.JsPlugin;
import refdiff.parsers.universal.js.JsNodeTypes;

public class JsValidator extends BaseValidator {

  @Override
  protected LanguagePlugin getPlugin() {
    return new JsPlugin();
  }

  @Override
  protected Language getLanguage() {
    return Language.JS;
  }

  @Override
  protected String[] getRepos() {
    return new String[] {
      "react",
      "next.js",
      "node",
      "three.js",
      "axios",
    };
  }

  @Override
  protected boolean isTypeEqual(String tagKind, String nodeType) {
    if (tagKind.equalsIgnoreCase("file")) {
      return JsNodeTypes.FILE.equals(nodeType);
    }
    if (tagKind.equalsIgnoreCase("function")) {
      return JsNodeTypes.FUNCTION.equals(nodeType);
    }
    if (tagKind.equalsIgnoreCase("class")) {
      return JsNodeTypes.CLASS.equals(nodeType);
    }
    return false;
  }

  public static void main(String[] args) throws Exception {
    JsValidator validator = new JsValidator();
    validator.run();
  }

}
