#!/usr/bin/env python3

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
SPEC_DIR = ROOT / "api" / "specs" / "nats"
OUT_FILE = ROOT / "scripts" / "nats-events.md"


def schema_type(prop):
    if "$ref" in prop:
        return prop["$ref"].split("/")[-1]

    t = prop.get("type")
    if isinstance(t, list):
        return " | ".join(t)
    return t or "unknown"


def render_properties(properties, required):
    lines = []
    lines.append("| Field | Type | Required | Description |")
    lines.append("|-------|------|----------|-------------|")

    for name, prop in properties.items():
        typ = schema_type(prop)
        req = "✓" if name in required else ""
        desc = prop.get("description", "")

        if "enum" in prop:
            typ += f" (enum: {', '.join(map(str, prop['enum']))})"

        lines.append(
            f"| `{name}` | {typ} | {req} | {desc} |"
        )

    return "\n".join(lines)


def generate():
    files = sorted(SPEC_DIR.glob("*.json"))

    output = [
        "# NATS Event Contracts",
        "",
        f"Generated from `{SPEC_DIR.relative_to(ROOT)}`",
        "",
    ]

    for file in files:
        with open(file) as f:
            schema = json.load(f)

        title = file.stem

        output.append(f"## {title}")
        output.append("")

        if schema.get("title"):
            output.append(f"**Schema Title:** {schema['title']}")
            output.append("")

        if schema.get("description"):
            output.append(schema["description"])
            output.append("")

        required = schema.get("required", [])
        properties = schema.get("properties", {})

        output.append("### Properties")
        output.append("")
        output.append(render_properties(properties, required))
        output.append("")

        if required:
            output.append("### Required Fields")
            output.append("")
            for field in required:
                output.append(f"- `{field}`")
            output.append("")

        if "example" in schema:
            output.append("### Example")
            output.append("")
            output.append("```json")
            output.append(json.dumps(schema["example"], indent=2))
            output.append("```")
            output.append("")

        output.append("---")
        output.append("")

    OUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    OUT_FILE.write_text("\n".join(output))

    print(f"Generated {OUT_FILE}")


if __name__ == "__main__":
    generate()