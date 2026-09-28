#!/usr/bin/env python3
"""
OpenAPI Endpoint Counter
Scans JSON/YAML OpenAPI schema files and counts endpoints by HTTP verb.
Outputs results to a CSV file.
"""

import argparse
import csv
import json
import sys
from collections import Counter
from pathlib import Path

try:
    import yaml
except ImportError:
    print("Error: PyYAML is required. Install with: pip install pyyaml", file=sys.stderr)
    sys.exit(1)


# Standard HTTP verbs per OpenAPI spec + commonly used extensions
STANDARD_VERBS = [
    "get", "put", "post", "delete", "options", "head", "patch", "trace", "query",
]


def load_spec(path: Path) -> dict:
    """Load a JSON or YAML OpenAPI spec file."""
    text = path.read_text(encoding="utf-8")
    if path.suffix.lower() == ".json":
        return json.loads(text)
    # .yaml / .yml
    return yaml.safe_load(text)


def extract_paths(spec: dict) -> dict:
    """
    Extract the paths object from an OpenAPI spec, handling both:
      - Swagger 2.0 / OpenAPI 3.x: spec['paths']
      - OpenAPI 3.1 with webhooks (skip)
    Returns an empty dict if no paths are present.
    """
    paths = spec.get("paths")
    if isinstance(paths, dict):
        return paths
    return {}


def count_verbs(spec: dict) -> Counter:
    """Count HTTP verbs used across all endpoints in the spec."""
    counter = Counter()
    paths = extract_paths(spec)

    for path, path_item in paths.items():
        if not isinstance(path_item, dict):
            continue

        # Skip path-level metadata keys like 'parameters', 'summary', etc.
        for key, value in path_item.items():
            if key.startswith("x-"):
                continue  # extensions
            if not isinstance(value, dict):
                continue

            verb = key.lower()
            if verb in STANDARD_VERBS:
                counter[verb] += 1
            else:
                # Keys that aren't standard verbs but have an operation-like object.
                # Filter out known non-verb path-level keys.
                if verb in {"parameters", "summary", "description", "servers", "$ref", "callbacks"}:
                    continue
                counter["other"] += 1

    return counter


def find_spec_files(folder: Path, recursive: bool = False) -> list:
    """Find all JSON and YAML files in the folder."""
    patterns = ["*.json", "*.yaml", "*.yml"]
    files = []
    for pattern in patterns:
        if recursive:
            files.extend(folder.rglob(pattern))
        else:
            files.extend(folder.glob(pattern))
    return sorted(set(files))


def process_folder(folder: Path, output_csv: Path, recursive: bool = False):
    files = find_spec_files(folder, recursive=recursive)
    if not files:
        print(f"No JSON/YAML files found in {folder}", file=sys.stderr)
        return

    # Column order: standard verbs then OTHER
    columns = [v.upper() for v in STANDARD_VERBS] + ["Others"]
    rows = []

    for file_path in files:
        try:
            spec = load_spec(file_path)
        except Exception as e:
            print(f"Warning: could not parse {file_path}: {e}", file=sys.stderr)
            continue

        if not isinstance(spec, dict):
            print(f"Warning: {file_path} is not a mapping; skipping.", file=sys.stderr)
            continue

        # Only process files that look like OpenAPI specs
        if "paths" not in spec and "openapi" not in spec and "swagger" not in spec:
            print(f"Skipping {file_path}: not an OpenAPI/Swagger spec.", file=sys.stderr)
            continue

        counts = count_verbs(spec)
        row = {"API": file_path.stem}
        for col in columns:
            key = col.lower()
            row[col] = counts.get(key, 0)
        rows.append(row)

    # Write CSV
    with output_csv.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["API"] + columns)
        writer.writeheader()
        for row in rows:
            writer.writerow(row)

    print(f"Wrote {len(rows)} row(s) to {output_csv}")


def main():
    parser = argparse.ArgumentParser(
        description="Count OpenAPI endpoints by HTTP verb across JSON/YAML specs."
    )
    parser.add_argument(
        "folder",
        nargs="?",
        default=".",
        help="Folder containing OpenAPI specs (default: current dir).",
    )
    parser.add_argument(
        "-o", "--output",
        default="http-verbs.csv",
        help="Output CSV file (default: openapi_endpoint_counts.csv).",
    )
    parser.add_argument(
        "-r", "--recursive",
        action="store_true",
        help="Recurse into subfolders.",
    )
    args = parser.parse_args()

    folder = Path(args.folder)
    if not folder.is_dir():
        print(f"Error: {folder} is not a directory.", file=sys.stderr)
        sys.exit(1)

    process_folder(folder, Path(args.output), recursive=args.recursive)


if __name__ == "__main__":
    main()