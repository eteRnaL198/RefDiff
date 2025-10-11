package evaluation;

import org.junit.Test;
import static org.junit.Assert.assertEquals;;

public class TestCommitUrl {
  @Test
  public void testExtract() {
    String url = "https://github.com/icse18-refactorings/realm-java/commit/6cf596df183b3c3a38ed5dd9bb3b0100c6548ebb";
    String owner = CommitUrl.extractOwner(url);
    String repoName = CommitUrl.extractRepoName(url);
    String sha1 = CommitUrl.extractSha1(url);

    assertEquals("icse18-refactorings", owner);
    assertEquals("realm-java", repoName);
    assertEquals("6cf596df183b3c3a38ed5dd9bb3b0100c6548ebb", sha1);
  }
}