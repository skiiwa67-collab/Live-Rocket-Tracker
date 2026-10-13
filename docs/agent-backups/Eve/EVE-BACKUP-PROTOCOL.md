# Eve backup protocol (Eve writes, Helios mirrors)

Approved by Chris on 2026-10-08. Schema: `EVE-BACKUP-SCHEMA.md` (`eve-session-backup/v1`).

## (a) Where Eve writes
Eve can only reach GitHub and Notion, so she writes to both:
1. **GitHub (primary):** repo `skiiwa67-collab/Live-Rocket-Tracker`, path
   `docs/agent-backups/Eve/YYYY-MM-DD-HHMM-eve-session-<begin|end>.md` (or `.json`).
   - **Legacy path, also watched:** `skiiwa67-collab/matter-translator`, `docs/agent-backups/eve/`. Eve's first backup, on 2026-10-08, landed there.
   - **2026-10-13 consolidation:** the primary folder now exists in Live-Rocket-Tracker (seeded with the Oct 8 matter-translator backup). The legacy path is kept and still watched. Master index: `docs/agent-backups/BACKUP-INDEX.md`.
2. **Notion:** a page titled `Eve session-<begin|end> YYYY-MM-DD HH:MM`, created under **Ideas & Parked Projects** (collection://b70dd5a7-33d0-4555-aed3-79fc8fc74fbb).

Eve does not write to Dropbox or to the box. Helios mirrors to both.

## (b) What goes in the body
Every backup includes the schema fields: `schema`, `agent`, `session_point`, `begin_timestamp` (with CT offset), `updated_at`, `trigger`, `chris_done`, `notion_page_url` (when known), and `summary` (topics, decisions, ideas, bugs, actions, open_items). `begin_timestamp` is the real session start in Central time; Helios copies it verbatim and never estimates it; an unknown start is written as `null` (unknown) with a note. No filler. Helios fills in `local_path`, `source`, `mirrors`, `mirrored_by` and `mirrored_at`.

## (c) Handoff when Chris is done
When Chris says he's done talking to Eve, she writes one final backup:
- `session_point: "end"`, `chris_done: true`
- GitHub file name and Notion title include `chris-done`, e.g.
  `2026-10-08-0710-eve-session-end-chris-done.md` and `Eve session-end 2026-10-08 07:10 chris-done`.

## (d) Helios's watcher
- Runs as a **server-side scheduled automation**, so it keeps running when Chris's computer is off. Each poll:
  1. Runs `python3 /workspace/agent-backups/Eve/eve_mirror_poll.py`, which lists new commits and files under both GitHub watch paths that aren't yet in `mirror-state.json`.
  2. Searches Notion (`notion-search` for "Eve session-") for page ids not yet in `mirror-state.json`.
  3. For each new backup: reads it, builds the `eve-session-backup/v1` JSON (helper: `eve_mirror_poll.py build ...`), saves it to `/workspace/agent-backups/Eve/`, and creates a **new** file in Dropbox `/Grok-Agent-Backups/Eve/`. It never overwrites; on a name clash it adds a suffix.
  4. Confirms the upload with Dropbox `get_file_metadata` (id, size, server_modified, content_hash), then records it in `mirror-state.json`.
- **Priority:** `chris_done` backups get mirrored on the first poll that sees them, ahead of everything else.
- When a GitHub commit and a Notion page belong to the same session (same date and begin time), they become one mirror that lists both source ids.
- No silent failures. If a step fails, the state entry is recorded as `failed` with the reason, and Helios reports it to Chris.

## Ready-to-paste instruction for Eve
> Eve, back up every conversation we have. At the start of each call, and again when I say I'm done, write a backup to GitHub (skiiwa67-collab/Live-Rocket-Tracker, folder docs/agent-backups/Eve/, file name like 2026-10-08-0710-eve-session-begin.md) and to a Notion page in Ideas & Parked Projects titled "Eve session-begin 2026-10-08 07:10". Put in: schema eve-session-backup/v1, agent Eve, session_point (begin, mid or end), begin_timestamp in Central time, updated_at, trigger, chris_done, the Notion page link, and a summary of what we covered: topics, decisions, ideas, bugs, actions, open items. No filler. When I say I'm done, write one last backup with session_point end and chris_done true, and put "chris-done" in the file name and page title. Skip Dropbox; Helios copies your backups there.
