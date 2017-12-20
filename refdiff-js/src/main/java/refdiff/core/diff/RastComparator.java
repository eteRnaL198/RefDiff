package refdiff.core.diff;

import static refdiff.core.diff.RastRootHelper.anonymous;
import static refdiff.core.diff.RastRootHelper.findByFullName;
import static refdiff.core.diff.RastRootHelper.fullName;
import static refdiff.core.diff.RastRootHelper.sameName;
import static refdiff.core.diff.RastRootHelper.sameNamespace;
import static refdiff.core.diff.RastRootHelper.sameSignature;
import static refdiff.core.diff.RastRootHelper.sameType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import refdiff.core.diff.similarity.SourceRepresentationBuilder;
import refdiff.core.io.SourceFile;
import refdiff.core.rast.HasChildrenNodes;
import refdiff.core.rast.Location;
import refdiff.core.rast.RastNode;
import refdiff.core.rast.RastNodeRelationshipType;
import refdiff.parsers.RastParser;
import refdiff.parsers.SourceTokenizer;

public class RastComparator<T> {
	
	private final RastParser parser;
	private final SourceTokenizer tokenizer;
	private final SourceRepresentationBuilder<T> srb;
	RastComparatorThresholds thresholds;
	
	public RastComparator(RastParser parser, SourceTokenizer tokenizer, SourceRepresentationBuilder<T> srb, RastComparatorThresholds thresholds) {
		this.parser = parser;
		this.tokenizer = tokenizer;
		this.srb = srb;
		this.thresholds = thresholds;
	}
	
	public RastDiff compare(List<SourceFile> filesBefore, List<SourceFile> filesAfter) throws Exception {
		return new DiffBuilder(filesBefore, filesAfter).computeDiff();
	}
	
	private class DiffBuilder {
		private RastDiff diff;
		private RastRootHelper<T> before;
		private RastRootHelper<T> after;
		private Set<RastNode> removed;
		private Set<RastNode> added;
		private final Map<RastNode, T> srMap = new HashMap<>();
		private final Map<RastNode, RastNode> mapBeforeToAfter = new HashMap<>();
		private final Map<RastNode, RastNode> mapAfterToBefore = new HashMap<>();
		private final Map<RastNode, Integer> depthMap = new HashMap<>();
		
		DiffBuilder(List<SourceFile> filesBefore, List<SourceFile> filesAfter) throws Exception {
			this.diff = new RastDiff(parser.parse(filesBefore), parser.parse(filesAfter));
			this.before = new RastRootHelper<T>(this.diff.getBefore());
			this.after = new RastRootHelper<T>(this.diff.getAfter());
			this.removed = new HashSet<>();
			this.diff.getBefore().forEachNode((node, depth) -> {
				this.removed.add(node);
				computeSourceRepresentation(filesBefore, node);
				depthMap.put(node, depth);
			});
			this.added = new HashSet<>();
			this.diff.getAfter().forEachNode((node, depth) -> {
				this.added.add(node);
				computeSourceRepresentation(filesAfter, node);
				depthMap.put(node, depth);
			});
		}
		
		private void computeSourceRepresentation(List<SourceFile> filesBefore, RastNode node) {
			try {
				Location location = node.getLocation();
				for (SourceFile file : filesBefore) {
					if (file.getPath().equals(location.getFile())) {
						String sourceCode = file.getContent().substring(location.getBodyBegin(), location.getBodyEnd());
						T sourceCodeRepresentation = srb.buildForNode(node, tokenizer.tokenize(sourceCode));
						srMap.put(node, sourceCodeRepresentation);
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		RastDiff computeDiff() {
			matchExactChildren(diff.getBefore(), diff.getAfter());
			matchMovesOrRenames();
			matchExtract();
			matchInline();
			return diff;
		}
		
		private void matchMovesOrRenames() {
			List<PotentialMatch> candidates = new ArrayList<>();
			for (RastNode n1 : removed) {
				for (RastNode n2 : added) {
					if (sameType(n1, n2) && !anonymous(n1) && !anonymous(n2)) {
						double score = srb.similarity(sourceRep(n1), sourceRep(n2));
						if (score > thresholds.moveOrRename) {
							PotentialMatch candidate = new PotentialMatch(n1, n2, Math.max(depth(n1), depth(n2)), score);
							candidates.add(candidate);
						}
					}
				}
			}
			Collections.sort(candidates);
			for (PotentialMatch candidate : candidates) {
				RastNode n1 = candidate.getNodeBefore();
				RastNode n2 = candidate.getNodeAfter();
				if (sameLocation(n1, n2)) {
					if (sameName(n1, n2)) {
						// change signature
					} else {
						addMatch(new Relationship(RelationshipType.RENAME, n1, n2));
					}
				} else {
					if (sameSignature(n1, n2)) {
						addMatch(new Relationship(RelationshipType.MOVE, n1, n2));
					} else {
						// move and rename / move and rename and change signature
					}
				}
			}
		}
		
		private void matchExtract() {
			for (Iterator<RastNode> iter = added.iterator(); iter.hasNext();) {
				RastNode n2 = iter.next();
				boolean extracted = false;
				for (RastNode n1After : after.findReverseRelationships(RastNodeRelationshipType.USE, n2)) {
					Optional<RastNode> optMatchingNode = matchingNodeBefore(n1After);
					if (optMatchingNode.isPresent()) {
						RastNode n1 = optMatchingNode.get();
						if (sameType(n1, n2)) {
							T sourceN1After = sourceRep(n1After);
							T sourceN1Before = sourceRep(n1);
							T removedSource = srb.minus(sourceN1Before, sourceN1After);
							double score = srb.partialSimilarity(sourceRep(n2), removedSource);
							if (score > thresholds.extract) {
								diff.addRelationships(new Relationship(RelationshipType.EXTRACT, n1, n2));
								extracted = true;
							}
						}
					}
				}
				if (extracted) {
					iter.remove();
				}
			}
		}
		
		private void matchInline() {
			for (Iterator<RastNode> iter = removed.iterator(); iter.hasNext();) {
				RastNode n1 = iter.next();
				boolean inlined = false;
				for (RastNode n1Caller : before.findReverseRelationships(RastNodeRelationshipType.USE, n1)) {
					Optional<RastNode> optMatchingNode = matchingNodeAfter(n1Caller);
					if (optMatchingNode.isPresent()) {
						RastNode n2 = optMatchingNode.get();
						if (sameType(n1, n2)) {
							T sourceN1 = sourceRep(n1);
							T sourceN1Caller = sourceRep(n1Caller);
							T sourceN1CallerAfter = sourceRep(n2);
							T addedCode = srb.minus(sourceN1CallerAfter, sourceN1Caller);
							double score = srb.partialSimilarity(sourceN1, addedCode);
							if (score > thresholds.inline) {
								diff.addRelationships(new Relationship(RelationshipType.INLINE, n1, n2));
							}
						}
					}
				}
				if (inlined) {
					iter.remove();
				}
			}
		}
		
		private void matchExactChildren(HasChildrenNodes node1, HasChildrenNodes node2) {
			List<RastNode> removedChildren = children(node1, this::removed);
			List<RastNode> addedChildren = children(node2, this::added);
			for (RastNode n1 : removedChildren) {
				for (RastNode n2 : addedChildren) {
					if (sameNamespace(n1, n2) && sameSignature(n1, n2) && sameType(n1, n2)) {
						addMatch(new Relationship(RelationshipType.SAME, n1, n2));
					}
				}
			}
		}

		private void matchPullUpAndPushDownMembers(RastNode nBefore, RastNode nAfter) {
			// Pull up
			for (RastNode addedMember : children(nAfter, this::added)) {
				for (RastNode subtype : before.findReverseRelationships(RastNodeRelationshipType.SUBTYPE, nBefore)) {
					Optional<RastNode> optNode = findByFullName(subtype, fullName(addedMember));
					if (optNode.isPresent() && removed(optNode.get())) {
						diff.addRelationships(new Relationship(RelationshipType.PULL_UP, optNode.get(), addedMember));
						unmarkRemoved(optNode.get());
						unmarkAdded(addedMember);
					}
				}
			}
			
			// Push down
			for (RastNode removedMember : children(nBefore, this::removed)) {
				for (RastNode subtype : after.findReverseRelationships(RastNodeRelationshipType.SUBTYPE, nAfter)) {
					Optional<RastNode> optNode = findByFullName(subtype, fullName(removedMember));
					if (optNode.isPresent() && added(optNode.get())) {
						diff.addRelationships(new Relationship(RelationshipType.PUSH_DOWN, removedMember, optNode.get()));
						unmarkRemoved(removedMember);
						unmarkAdded(optNode.get());
					}
				}
			}
		}
		
		private void addMatch(Relationship relationship) {
			RastNode nBefore = relationship.getNodeBefore();
			RastNode nAfter = relationship.getNodeAfter();
			if (removed(nBefore) && added(nAfter)) {
				diff.addRelationships(relationship);
				mapBeforeToAfter.put(nBefore, nAfter);
				mapAfterToBefore.put(nAfter, nBefore);
				unmarkRemoved(nBefore);
				unmarkAdded(nAfter);
				matchExactChildren(nBefore, nAfter);
				matchPullUpAndPushDownMembers(nBefore, nAfter);
			}
		}
		

		private T sourceRep(RastNode n) {
			return srMap.get(n);
		}
		
		private int depth(RastNode n) {
			return depthMap.get(n);
		}
		
		private Optional<RastNode> matchingNodeBefore(RastNode n2) {
			return Optional.ofNullable(mapAfterToBefore.get(n2));
		}
		
		private Optional<RastNode> matchingNodeAfter(RastNode n1) {
			return Optional.ofNullable(mapBeforeToAfter.get(n1));
		}
		
		private boolean sameLocation(RastNode n1, RastNode n2) {
			if (n1.getParent().isPresent() && n2.getParent().isPresent()) {
				return matchingNodeAfter(n1.getParent().get()).equals(n2.getParent());
			} else if (!n1.getParent().isPresent() && !n1.getParent().isPresent()) {
				return sameNamespace(n1, n2);
			} else {
				return false;
			}
		}
		
		private boolean removed(RastNode n) {
			return this.removed.contains(n);
		}
		
		private boolean added(RastNode n) {
			return this.added.contains(n);
		}
		
		private boolean unmarkRemoved(RastNode n) {
			return this.removed.remove(n);
		}
		
		private boolean unmarkAdded(RastNode n) {
			return this.added.remove(n);
		}
		
		private List<RastNode> children(HasChildrenNodes nodeWithChildren, Predicate<RastNode> predicate) {
			return nodeWithChildren.getNodes().stream().filter(predicate).collect(Collectors.toList());
		}
	}
	
}
