import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MD = os.path.join(ROOT, "modrinth-description.md")
JSON_PATH = os.path.join(ROOT, "modrinth.json")

with open(MD, encoding="utf-8") as f:
    body = f.read().rstrip() + "\n"

with open(JSON_PATH, encoding="utf-8") as f:
    data = json.load(f)

if data.get("body") == body:
    print("已是最新，无需改动")
else:
    data["body"] = body
    with open(JSON_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("已把 modrinth-description.md 同步进 modrinth.json")
