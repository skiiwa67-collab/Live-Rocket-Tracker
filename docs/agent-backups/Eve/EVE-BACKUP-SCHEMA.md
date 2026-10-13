# eve-session-backup/v1

Backup format for **Eve**, Chris Olsen's voice companion in the regular Grok app.
Eve has no Dropbox access and no box disk. She writes each backup to **GitHub and Notion**.
**Helios** mirrors it to the box (`/workspace/agent-backups/Eve/`) and to Dropbox (`/Grok-Agent-Backups/Eve/`).
Modeled on `tinkabot-session-backup/v1` (`/workspace/agent-backups/tinkabot/session-begin-2026-10-07.json`).

Created 2026-10-08 by Helios with Chris's approval.

## Fields

| Field | Type | Required | Who fills it | Meaning |
|---|---|---|---|---|
| `schema` | string | yes | Eve | Always `"eve-session-backup/v1"`. |
| `agent` | string | yes | Eve | Always `"Eve"`. |
| `session_point` | `"begin"` \| `"mid"` \| `"end"` | yes | Eve | When in the conversation this backup was taken. |
| `begin_timestamp` | ISO 8601 with CT offset, or `null` if unknown | yes | Eve | When the session actually started, in Central time, e.g. `2026-10-08T07:10:00-05:00` (CDT `-05:00`, CST `-06:00`). Helios copies Eve's value verbatim and never estimates it. If the start time is unknown or unverified, the value is `null` (unknown) with a note saying so; estimates are never used. |
| `updated_at` | ISO 8601 with CT offset | yes | Eve | When this backup was written. |
| `trigger` | string | yes | Eve | What caused the backup, e.g. "Chris started a voice call", "Chris said he's done". |
| `chris_done` | bool | yes | Eve | `true` only when Chris said he is done talking to Eve. That makes it the final backup, with `session_point: "end"`. |
| `summary` | object | yes | Eve | What was discussed. No filler. Has the sub-keys `topics`, `decisions`, `ideas`, `bugs`, `actions` and `open_items`, each a list of short strings. Use an empty list when there's nothing. |
| `local_path` | string | yes | Helios | Box path of the mirrored JSON. Eve writes `null`. |
| `notion_page_url` | string | yes | Eve (or Helios) | URL of the Notion page for this backup. |
| `source` | object | yes | Helios | `github_repo`, `github_path`, `github_commit`, `github_blob_sha`, `notion_page_id` (string, or a list when one session has several pages). |
| `mirrors` | object | yes | Helios | `local`, `dropbox`, `github`, `notion`. Each is `{ "status": "ok" \| "uploaded" \| "pending" \| "failed" \| "n/a", "path_or_url": ..., ... }`. `uploaded` means the Dropbox file was created and its verified metadata is in `mirror-state.json`. |
| `mirrored_by` | string | yes | Helios | Always `"Helios"`. |
| `mirrored_at` | ISO 8601 with CT offset | yes | Helios | When Helios wrote the mirror. |
| `notes` | list of strings | no | either | Caveats, such as an approximate start time or duplicate pages. |

## Naming
- GitHub: `docs/agent-backups/Eve/YYYY-MM-DD-HHMM-eve-session-<begin|mid|end>[-chris-done].md` (or `.json`)
- Notion title: `Eve session-<begin|mid|end> YYYY-MM-DD HH:MM` (add ` chris-done` for the final backup)
- Box and Dropbox mirror: `YYYY-MM-DD-HHMM-eve-session-<point>[-chris-done].json`. HHMM is the CT time the source backup was written. Existing Dropbox files are never overwritten; on a name clash, add `-2`, `-3` and so on.

## Example skeleton
```json
{
  "schema": "eve-session-backup/v1",
  "agent": "Eve",
  "session_point": "end",
  "begin_timestamp": "<real session start, e.g. 2026-10-08T07:10:00-05:00>",
  "updated_at": "2026-10-08T07:10:00-05:00",
  "trigger": "Chris said he's done talking to Eve",
  "chris_done": true,
  "summary": {"topics": [], "decisions": [], "ideas": [], "bugs": [], "actions": [], "open_items": []},
  "local_path": null,
  "notion_page_url": "https://app.notion.com/p/<id>",
  "source": {"github_repo": "skiiwa67-collab/Live-Rocket-Tracker", "github_path": "docs/agent-backups/Eve/...", "github_commit": null, "github_blob_sha": null, "notion_page_id": "<id>"},
  "mirrors": {"local": {"status": "pending"}, "dropbox": {"status": "pending"}, "github": {"status": "ok"}, "notion": {"status": "ok"}},
  "mirrored_by": "Helios",
  "mirrored_at": null
}
```

## Known corrections
- 2026-10-08 first mirror (`2026-10-08-0622-eve-session-begin.json`): `begin_timestamp` is unknown (`null`). Eve's "~05:42" was a rough estimate and 06:42 CT was when Chris's follow-up conversation started; neither is a verified start. From then on Eve writes the actual start time in every backup.
