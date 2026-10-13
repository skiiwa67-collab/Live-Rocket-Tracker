#!/usr/bin/env python3
"""Eve backup mirror poller (GitHub detection + local helpers).

Eve (Chris's Grok-app voice companion) writes session backups to GitHub and Notion.
Helios mirrors them to the box (/workspace/agent-backups/Eve/) and to Dropbox (/Grok-Agent-Backups/Eve/).

What this script does (uses `gh` on the box, authenticated as skiiwa67-collab):
  poll   (default) Lists every file under the GitHub watch paths in mirror-state.json
                   and prints the ones whose (repo, path, blob_sha) isn't recorded yet, as JSON.
                   chris-done backups are flagged priority=true and sorted first.
  build            Builds an eve-session-backup/v1 JSON from CLI args and a summary JSON file.
  dbx-hash FILE    Prints the Dropbox content_hash of a local file, to compare against Dropbox metadata.
  record           Appends one mirrored entry (a JSON file) to mirror-state.json.

What it does NOT do: Notion and Dropbox go through MCP connectors (user-Notion,
user-dropbox), which a script can't call. The agent running the poll must also:
  - notion-search "Eve session-" and compare page ids with mirror-state.json
  - read each new source, build the JSON (build), save it locally
  - Dropbox create_file (new name only, never overwrite) -> get_file_metadata /
    download_link content_hash -> compare with dbx-hash -> record
Read-only toward GitHub. Writes only to the local file named by --out and to mirror-state.json (record).
"""
import argparse, datetime, hashlib, json, os, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
STATE = os.path.join(HERE, "mirror-state.json")


def now_ct():
    return datetime.datetime.now().astimezone().isoformat(timespec="seconds")


def gh_api(path):
    r = subprocess.run(["gh", "api", path], capture_output=True, text=True)
    if r.returncode != 0:
        if "Not Found" in r.stderr or "404" in r.stderr or "Not Found" in r.stdout:
            return None
        raise RuntimeError(f"gh api {path} failed: {r.stderr.strip() or r.stdout.strip()}")
    return json.loads(r.stdout)


def load_state():
    with open(STATE) as f:
        return json.load(f)


def known_keys(state):
    keys = set()
    for m in state.get("mirrored", []):
        for g in m.get("sources", {}).get("github", []):
            keys.add((g["repo"], g["path"], g["blob_sha"]))
    return keys


def known_notion(state):
    ids = set()
    for m in state.get("mirrored", []):
        for i in m.get("sources", {}).get("notion_page_ids", []):
            ids.add(i.replace("-", ""))
    return ids


def list_files(repo, path):
    """Recursively list files under path; returns [] if path doesn't exist yet."""
    items = gh_api(f"repos/{repo}/contents/{path}")
    if items is None:
        return None
    if isinstance(items, dict):
        items = [items]
    out = []
    for it in items:
        if it["type"] == "file":
            out.append(it)
        elif it["type"] == "dir":
            out.extend(list_files(repo, it["path"]) or [])
    return out


def last_commit(repo, path):
    c = gh_api(f"repos/{repo}/commits?path={path}&per_page=1")
    if not c:
        return None, None
    return c[0]["sha"], c[0]["commit"]["committer"]["date"]


def cmd_poll(_):
    state = load_state()
    known = known_keys(state)
    report = {"checked_at": now_ct(), "watches": [], "unmirrored": [], "errors": []}
    for w in state["watch"]["github"]:
        repo, path = w["repo"], w["path"]
        try:
            files = list_files(repo, path)
        except Exception as e:  # no silent failures
            report["errors"].append({"repo": repo, "path": path, "error": str(e)})
            continue
        report["watches"].append({"repo": repo, "path": path, "role": w.get("role"),
                                  "exists": files is not None, "file_count": len(files or [])})
        for f in files or []:
            if (repo, f["path"], f["sha"]) in known:
                continue
            sha, date = last_commit(repo, f["path"])
            name = f["name"].lower()
            report["unmirrored"].append({
                "repo": repo, "path": f["path"], "blob_sha": f["sha"], "size": f["size"],
                "commit": sha, "committed_at": date,
                "priority": "chris-done" in name or "chris_done" in name,
                "html_url": f.get("html_url"),
            })
    report["unmirrored"].sort(key=lambda x: (not x["priority"], x["committed_at"] or ""))
    report["unmirrored_count"] = len(report["unmirrored"])
    report["known_notion_page_ids"] = sorted(known_notion(state))
    report["note"] = "GitHub only. Notion: run notion-search 'Eve session-' and diff page ids against known_notion_page_ids."
    print(json.dumps(report, indent=2))
    return 1 if report["errors"] else 0


def cmd_build(a):
    summary = {"topics": [], "decisions": [], "ideas": [], "bugs": [], "actions": [], "open_items": []}
    if a.summary_json:
        with open(a.summary_json) as f:
            src = json.load(f)
        # Eve may write "open" or "open items"; normalize to open_items
        for alt in ("open", "open items", "openItems"):
            if alt in src:
                src.setdefault("open_items", src.pop(alt))
        summary.update(src)
    if not a.begin:
        sys.exit("--begin is required: Eve's begin_timestamp copied verbatim, or 'unknown' (never estimated)")
    if a.begin.lower() in ("unknown", "null", "none"):
        a.begin = None
    notion_ids = a.notion_id or []
    d = {
        "schema": "eve-session-backup/v1",
        "agent": "Eve",
        "session_point": a.session_point,
        "begin_timestamp": a.begin,
        "updated_at": a.updated,
        "trigger": a.trigger,
        "chris_done": a.chris_done,
        "summary": summary,
        "local_path": os.path.abspath(a.out),
        "notion_page_url": a.notion_url,
        "source": {
            "github_repo": a.repo, "github_path": a.path, "github_commit": a.commit,
            "github_blob_sha": a.blob,
            "notion_page_id": notion_ids[0] if len(notion_ids) == 1 else notion_ids,
        },
        "mirrors": {
            "local": {"status": "ok", "path_or_url": os.path.abspath(a.out)},
            "dropbox": {"status": "pending", "path_or_url": a.dropbox_path},
            "github": {"status": "ok" if a.commit else "n/a", "path_or_url": a.path},
            "notion": {"status": "ok" if notion_ids else "n/a", "path_or_url": a.notion_url},
        },
        "mirrored_by": "Helios",
        "mirrored_at": now_ct(),
        "notes": a.note or [],
    }
    if a.chris_done and a.session_point != "end":
        print("warning: chris_done=true but session_point != end", file=sys.stderr)
    if os.path.exists(a.out) and not a.force:
        sys.exit(f"refusing to overwrite {a.out} (use --force)")
    with open(a.out, "w") as f:
        json.dump(d, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(a.out)


def dbx_hash(p):
    b = open(p, "rb").read()
    blocks = b"".join(hashlib.sha256(b[i:i + 4194304]).digest() for i in range(0, len(b), 4194304))
    return hashlib.sha256(blocks).hexdigest()


def cmd_dbx_hash(a):
    print(dbx_hash(a.file))


def cmd_record(a):
    state = load_state()
    with open(a.entry_json) as f:
        entry = json.load(f)
    state["mirrored"].append(entry)
    state["updated_at"] = now_ct()
    tmp = STATE + ".tmp"
    with open(tmp, "w") as f:
        json.dump(state, f, indent=2, ensure_ascii=False)
        f.write("\n")
    os.replace(tmp, STATE)
    print("recorded", entry.get("session_key"))


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sp = p.add_subparsers(dest="cmd")
    sp.add_parser("poll")
    b = sp.add_parser("build")
    b.add_argument("--out", required=True)
    b.add_argument("--session-point", choices=["begin", "mid", "end"], required=True)
    b.add_argument("--begin", required=True, help="ISO with CT offset")
    b.add_argument("--updated", required=True, help="ISO with CT offset")
    b.add_argument("--trigger", required=True)
    b.add_argument("--chris-done", action="store_true")
    b.add_argument("--summary-json")
    b.add_argument("--repo"); b.add_argument("--path"); b.add_argument("--commit"); b.add_argument("--blob")
    b.add_argument("--notion-id", action="append"); b.add_argument("--notion-url")
    b.add_argument("--dropbox-path")
    b.add_argument("--note", action="append")
    b.add_argument("--force", action="store_true")
    h = sp.add_parser("dbx-hash"); h.add_argument("file")
    r = sp.add_parser("record"); r.add_argument("entry_json")
    a = p.parse_args()
    if a.cmd in (None, "poll"):
        sys.exit(cmd_poll(a))
    {"build": cmd_build, "dbx-hash": cmd_dbx_hash, "record": cmd_record}[a.cmd](a)


if __name__ == "__main__":
    main()
