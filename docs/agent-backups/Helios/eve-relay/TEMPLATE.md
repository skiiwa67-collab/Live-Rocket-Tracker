# Eve relay: one-line ask template
Follows the canonical `HARD-cross-agent-interrupt.md` (Perseus), step 1.

Send with SendToAgent **priority:true** to Eve (8d633572-a8c8-4944-807a-c212b491a370), **CC Perseus** (7e2c6ae8-53dc-4e13-a997-59398b8f7842). The CC is for the pending-asks list only.

Canonical line:
```
<Bot> needs <action> because <why>. Risk: <low/med/high>. Say yes or no.
```
Helios suggested additions (ask-id prefix, undoable tag), pending Perseus:
```
[<ASK-ID>] <Bot> needs <action> because <why>. Risk: <low/med/high>. Undoable: <yes/no>. Say yes or no.
```
- ASK-ID: `<BOT3>-YYYYMMDD-NN`, e.g. `HEL-20261013-01`. Eve's reply: `[<ASK-ID>] yes|no`.
- Native card (any MiniME command) → hold the action. Eve says "<Bot> has a card waiting. Approve it when you're parked."
- Sends, deletes, spend, Dropbox writes/deletes → queued for Chris's own chat, not a voice go. Play publish → never.
- Live launch / driving → non-urgent asks get queued and batched.
- After: log to relay-log.jsonl and report whether Chris heard the ask without leaving the call.

Example (staged test; MiniME is currently not connected):
`[HEL-20261013-01] Helios needs to list MiniME drives (read-only) because of backup-drive setup. Risk: low. Undoable: yes. Say yes or no.`
