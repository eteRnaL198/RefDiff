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
      // fieldMatches.put("line", false);
      fieldMatches.put("type", false);
      fieldMatches.put("name", false);
      // fieldMatches.put("namespace", false);

      String tagFile = Optional.ofNullable(tag.getPath()).orElse("");
      String nodeFile = Optional.ofNullable(node.getLocation()).map(l -> l.getFile()).orElse("");
      if (!tagFile.equals(nodeFile)) {
        return; // skip nodes in different files
      }
      fieldMatches.put("file", true);

      if (tag.getName() != null && tag.getName().equals(node.getSimpleName())) {
        fieldMatches.put("name", true);
      } else if (tag.getKind().equals("method") && node.getSimpleName().equals("new")) { // constructor special case
        fieldMatches.put("name", true);
      } else {
        return; // name must match to consider further fields
      }

      // Integer tagLine = tag.getLine();
      // Integer nodeLine = null;
      // if (node.getLocation() != null) {
      //   try {
      //     nodeLine = node.getLocation().getBeginLine();
      //   } catch (Exception ex) {
      //     nodeLine = null;
      //   }
      // }
      // if (tagLine != null && nodeLine != null && tagLine.equals(nodeLine)) {
      //   fieldMatches.put("line", true);
      // } else if (tagLine != null && nodeLine != null && (tagLine + 1 == nodeLine)) {
      //   // treat off-by-one (tag points to previous line) as a match but record a note
      //   fieldMatches.put("line", true);
      //   match.addNote("line_offset(+1) (tagLine=" + tagLine + ", nodeLine=" + nodeLine + ")");
      // } else {
      //   match.addNote("line_mismatch (tagLine=" + tagLine + ", nodeLine=" + nodeLine + ")");
      // }

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

    return match;
  }

  public String toNdjson() {
    StringBuilder sb = new StringBuilder();
    sb.append("{").append('\n');
    sb.append("\t\"exactMatch\":").append(exactMatch).append(',').append('\n');
    sb.append("\t\"notes\": [");
    for (int i = 0; i < notes.length; i++) {
      sb.append(jsonStr(notes[i]));
      if (i < notes.length - 1) {
        sb.append(", ");
      }
    }
    sb.append("]").append(',').append('\n');
    sb.append("\t\"tagNdjsonLine\":").append(tag.getNdjsonLine()).append(',').append('\n');
    sb.append("\t\"tagName\":").append(jsonStr(tag.getName())).append(',').append('\n');
    sb.append("\t\"tagKind\":").append(jsonStr(tag.getKind())).append(',').append('\n');
    sb.append("\t\"tagPath\":").append(jsonStr(tag.getPath())).append(',').append('\n');
    sb.append("\t\"tagLine\":").append(jsonIntOptional(tag.getLine())).append(',').append('\n');
    sb.append("\t\"tagPattern\":").append(jsonStr(tag.getPattern())).append(',').append('\n');
    sb.append("\t\"cstNodeCandidates\": [").append('\n');
    for (int i = 0; i < cstNodeCandidates.size(); i++) {
      CstNode node = cstNodeCandidates.get(i);
      Map<String, Boolean> fieldMatches = matchedFields.get(node.getId());
      if (fieldMatches == null) {
        fieldMatches = new HashMap<>();
        fieldMatches.put("file", false);
        // fieldMatches.put("line", false);
        fieldMatches.put("type", false);
        fieldMatches.put("name", false);
      }
      sb.append("\t\t{").append('\n');
      sb.append("\t\t\t\"nodeId\":").append(jsonIntOptional(node.getId())).append(',').append('\n');
      sb.append("\t\t\t\"type\":").append(jsonStr(node.getType())).append(',').append('\n');
      sb.append("\t\t\t\"simpleName\":").append(jsonStr(node.getSimpleName())).append(',').append('\n');
      sb.append("\t\t\t\"localName\":").append(jsonStr(node.getLocalName())).append(',').append('\n');
      String nodeFile = Optional.ofNullable(node.getLocation()).map(l -> l.getFile()).orElse("");
      sb.append("\t\t\t\"file\":").append(jsonStr(nodeFile)).append(',').append('\n');
      Integer startLine = node.getLocation() != null ? Integer.valueOf(node.getLocation().getBeginLine()) : null;
      sb.append("\t\t\t\"startLine\":").append(jsonIntOptional(startLine)).append(',').append('\n');
      sb.append("\t\t\t\"matchedFields\": {").append('\n');
      sb.append("\t\t\t\t\"file\": ").append(fieldMatches.get("file")).append(',').append('\n');
      // sb.append("\t\t\t\t\"line\": ").append(fieldMatches.get("line")).append(',').append('\n');
      sb.append("\t\t\t\t\"type\": ").append(fieldMatches.get("type")).append(',').append('\n');
      sb.append("\t\t\t\t\"name\": ").append(fieldMatches.get("name")).append(',').append('\n');
      sb.append("\t\t\t}\n");
      sb.append("\t\t}");
      if (i < cstNodeCandidates.size() - 1) {
        sb.append(',');
      }
      sb.append('\n');
    }
    sb.append("\t]\n");
    sb.append("}");
    return sb.toString();
  }

  private static String jsonStr(Object o) {
    if (o == null)
      return "null";
    String s = o.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    return "\"" + s + "\"";
  }

  private static String jsonIntOptional(Object o) {
    if (o == null)
      return "null";
    return o.toString();
  }

}
