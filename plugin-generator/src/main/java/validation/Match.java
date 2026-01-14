package validation;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiPredicate;
import java.util.Optional;

import refdiff.core.cst.CstNode;
import refdiff.core.cst.CstRoot;

public class Match {
  private Tag tag;
  private List<CstNode> cstNodeCandidates;
  private Map<Integer, Map<String, Boolean>> matchedFields; // cstNodeId -> (fieldName -> isMatched)
  private Boolean exactMatch;
  private String[] notes;

  Match(Tag tag) {
    this.tag = tag;
    this.cstNodeCandidates = new ArrayList<>();
    this.matchedFields = new HashMap<>();
    this.exactMatch = false;
    this.notes = new String[]{};
  }

  public Tag getTag() {
    return this.tag;
  }
  public List<CstNode> getCstNodeCandidates() {
    return this.cstNodeCandidates;
  }
  public Map<Integer, Map<String, Boolean>> getMatchedFields() {
    return this.matchedFields;
  }
  public Boolean getExactMatch() {
    return this.exactMatch;
  }
  public String[] getNotes() {
    return this.notes;
  }

  private void addNote(String note) {
    String[] newNotes = new String[this.notes.length + 1];
    System.arraycopy(this.notes, 0, newNotes, 0, this.notes.length);
    newNotes[this.notes.length] = note;
    this.notes = newNotes;
  }

  /**
   * Create a Match for the given tag using nodes from the CST root.
   * isTypeEqual: function that returns true when the tag kind and node type should be considered equal.
   */
  public static Match create(Tag tag, CstRoot root, BiPredicate<String, String> isTypeEqual) {
    Match match = new Match(tag);

    root.forEachNode((node, d) -> {
      Map<String, Boolean> fieldMatches = new HashMap<>();
      fieldMatches.put("file", false);
      fieldMatches.put("line", false);
      fieldMatches.put("name", false);
      fieldMatches.put("type", false);

      String tagFile = Optional.ofNullable(tag.getPath()).orElse("");
      String nodeFile = Optional.ofNullable(node.getLocation()).map(l -> l.getFile()).orElse("");
      if (!tagFile.equals(nodeFile)) {
        return; // skip nodes in different files
      }
      fieldMatches.put("file", true);

      Integer tagLine = tag.getLine();
      Integer nodeLine = node.getLocation().getBeginLine();
      if (tagLine != null && nodeLine != null && tagLine.equals(nodeLine)) {
        fieldMatches.put("line", true);
      }

      if (tag.getName() != null && tag.getName().equals(node.getSimpleName())) {
        fieldMatches.put("name", true);
      } else if (tag.getKind().equals("method") && node.getSimpleName().equals("new")) { // constructor special case
        fieldMatches.put("name", true);
      } else {
        return; // name must match to consider further fields
      }

      if (isTypeEqual.test(tag.getKind(), node.getType())) {
        fieldMatches.put("type", true);
      }

      // if any field matched, record this node as a candidate
      boolean anyMatched = fieldMatches.values().stream().anyMatch(b -> b.booleanValue());
      if (anyMatched) {
        match.cstNodeCandidates.add(node);
        match.matchedFields.put(node.getId(), fieldMatches);

        boolean allMatched = fieldMatches.values().stream().allMatch(b -> b.booleanValue());
        if (allMatched) {
          match.exactMatch = true;
        }
      }
    });

    if (match.cstNodeCandidates.isEmpty()) {
      match.addNote("No matching CST nodes found");
    } else if (!match.exactMatch) {
      match.addNote("Partial matches found");
    }

    return match;
  }
}
