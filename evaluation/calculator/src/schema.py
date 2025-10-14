oracle_java_schema = {
    "index": int,
    "Commit URL": str,
    "Refactoring Type": str,
    "Description": str,
    "Relationship Type": str,
    "CST Node before": str,
    "CST Node After": str,
    "Expected?": str,
    "RefDiff 2.0": str,
    "RefDiff 1.0": str,
    "RMiner": str,
    "Evaluators": str,
    "Evaluators' classification": str,
}

detected_java_schema = {
    "index": int,
    "repository": str,
    "commit": str,
    "type": str,
    "before": str,
    "after": str,
}

result_java_schema = {
    "commit url": str,
    "refactoring type": str,
    "relationship type": str,
    "before": str,
    "after": str,
    "expected": str,
    "baseline result": str,
    "detected result": str,
    "equal to baseline": bool,
    "oracle index": int,
    "detected index": int,
    "note": str,
}
