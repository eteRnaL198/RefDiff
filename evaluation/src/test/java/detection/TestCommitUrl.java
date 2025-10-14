package detection;

import org.junit.Test;

import detection.CommitUrl;

import static org.junit.Assert.assertEquals;;

public class TestCommitUrl {
  @Test
  public void testExtract() {
    String url = "https://github.com/owner/name/commit/abcdefghijklmnopqrstuvwxyz123456";
    String owner = CommitUrl.extractOwner(url);
    String repoName = CommitUrl.extractRepoName(url);
    String sha1 = CommitUrl.extractSha1(url);

    assertEquals("owner", owner);
    assertEquals("name", repoName);
    assertEquals("abcdefghijklmnopqrstuvwxyz123456", sha1);
  }
}
