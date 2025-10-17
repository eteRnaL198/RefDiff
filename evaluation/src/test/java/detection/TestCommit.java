package detection;

import org.junit.Test;

import executor.Commit;

import static org.junit.Assert.assertEquals;;

public class TestCommit {
  @Test
  public void testExtract() {
    String url = "https://github.com/owner/name/commit/abcdefghijklmnopqrstuvwxyz123456";
    String owner = Commit.extractOwner(url);
    String repoName = Commit.extractRepoName(url);
    String sha1 = Commit.extractSha1(url);

    assertEquals("owner", owner);
    assertEquals("name", repoName);
    assertEquals("abcdefghijklmnopqrstuvwxyz123456", sha1);
  }
}
