package validation;

import plugingenerator.Language;
import refdiff.parsers.LanguagePlugin;
import refdiff.parsers.universal.python.PythonParser;
import refdiff.parsers.universal.python.PythonNodeTypes;

public class PythonValidator extends BaseValidator {

  @Override
  protected LanguagePlugin getPlugin() {
    return new PythonParser();
  }

  @Override
  protected Language getLanguage() {
    return Language.PYTHON;
  }

  @Override
  protected String[] getRepos() {
    return new String[] {
      "AutoGPT",
      "stable-diffusion-webui",
      "transformers",
      "langflow",
      "youtube-dl",
    };
  }

  @Override
  protected boolean isTypeEqual(String tagKind, String nodeType) {
    if (tagKind.equalsIgnoreCase("function")) {
      return PythonNodeTypes.FUNCTION.equals(nodeType);
    }
    return false;
  }

  public static void main(String[] args) throws Exception {
    PythonValidator validator = new PythonValidator();
    validator.run();
  }

}
