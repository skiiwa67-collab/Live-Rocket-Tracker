# relay-log.jsonl schema (one JSON object per line; file starts empty)
JSONL can't hold a comment header, so the schema lives here. **This file is canonical for status names.**

{"ask_id":"HEL-20261013-01","bot":"Helios","ask":"list MiniME drives","undoable":true,"risk":"low","why":"backup-drive setup",
 "sent_at":"ISO8601 CT","relayed_at":null,"answer":"yes|no|null","answered_at":null,
 "parked_at":null,"card_shown_at":null,"card_result":"approved|denied|expired|null","card_at":null,
 "call_dropped":false,"status":"pending|answered|held-for-card|done|failed","notes":""}

## status values (canonical)
| status | meaning | Eve mood (deriver) |
|---|---|---|
| pending | sent to Eve, no answer yet | relay-active |
| answered | Chris answered; no native card needed, or the answer was no | (none) |
| held-for-card | Chris said yes, and the action needs a native card that waits until he's parked | waiting-parked |
| done | finished (card approved, or action done) | clears to idle |
| failed | denied, expired, call dropped, or timed out | clears to idle |

- The log is append-only. The **newest line per ask_id** is that ask's current state, and earlier lines are history.
- Deriver aliases `sent` and `yes-held-for-card` are retired. Use `pending` and `held-for-card` instead.
- Stale rule: a `pending` older than 30 min, or a `held-for-card` older than 12 h (by `sent_at`/`answered_at`), is shown as stale, not active. The requester should then append a `failed` line with `notes:"timeout"`.
