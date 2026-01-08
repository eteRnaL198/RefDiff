package validation;

import plugingenerator.Language;
import refdiff.parsers.LanguagePlugin;
import refdiff.parsers.universal.ruby.RubyParser;
import refdiff.parsers.universal.ruby.RubyNodeTypes;

public class RubyValidator extends BaseValidator {

  @Override
  protected LanguagePlugin getPlugin() {
    return new RubyParser();
  }

  @Override
  protected Language getLanguage() {
    return Language.RUBY;
  }

  @Override
  protected String[] getRepos() {
    return new String[] {
      "rails",
      "maybe",
      "jekyll",
      "mastodon",
      "huginn",
    };
  }

  @Override
  protected boolean isTypeEqual(String tagKind, String nodeType) {
    if (tagKind.equalsIgnoreCase("method")) {
      return RubyNodeTypes.METHOD.equals(nodeType);
    }
    return false;
  }

  public static void main(String[] args) throws Exception {
    RubyValidator validator = new RubyValidator();
    validator.run();
  }

}
