# Helios review: Cross-Agent Interrupt via Eve relay

**Reviews the canonical rule. This is not a competing rule doc.** Canonical: `/workspace/agent-backups/Helios/HARD-cross-agent-interrupt.md` (Perseus draft), also on GitHub main at `docs/agent-backups/Helios/HARD-cross-agent-interrupt.md` and in Notion at https://app.notion.com/p/3f8a94933faa81568b27d6ade4c3da1b. Helios has not edited that file or page. Proposed changes are listed under "Suggested edits".

Reviewed: 2026-10-13 (CT), by Helios (first implementer/tester)
Requested by: Chris (t1127u), rule coordinated by Perseus
Checked against: HARD-cross-agent-interrupt.md (Perseus draft), HARD-no-agent-play-publish.md, HARD-play-store-publish-gate.md, HARD-continuous-persona-self-improve.md (live-launch Soft-FAIL), HARD-ada-dual-push-github-and-studio.md (dual-write pattern)

## Verdicts

| # | Piece | Verdict |
|---|-------|---------|
| 1 | Direct-to-Eve routing | Soft-PASS |
| 2 | One-line format (bot, ask, why) | Soft-PASS with additions |
| 3 | Spoken yes for small undoable | Soft-PASS with limits |
| 4 | No-agent Play publish lock | Unchanged. Soft-FAIL any relay that asks to publish |
| 5 | Don't interrupt during live launches | Soft-PASS. The relay must respect it |
| 6 | MiniME Allow cards wait until parked | Soft-PASS |
| 7 | Dual-write | Soft-PASS |

### 1. Direct-to-Eve routing: Soft-PASS
One hop: bot sends SendToAgent to Eve (8d633572-a8c8-4944-807a-c212b491a370). That stops Chris hunting for a card and hanging up the call, which is what happened on Oct 13.
Conflict to settle: Perseus's draft (step 1) says "CC Perseus". Chris's t1127u says "not through Perseus". Helios reads it this way: Perseus is never in the live path. A CC for the pending-asks list is fine only if it doesn't hold up Eve. Perseus should confirm.

### 2. One-line format: Soft-PASS with additions
Required fields:
- `ask-id` (e.g. `HEL-20261013-01`) so the yes/no matches the right ask.
- bot, ask, why.
- `undoable? yes/no` tag. If it says `no`, Eve may refuse to relay it as a voice go. She tells Chris it waits for his chat.
- Keep Perseus's `Risk: low/med/high`.

### 3. Spoken yes for small undoable: Soft-PASS with limits
A relayed yes counts only for reversible actions inside a task Chris already approved.
It does NOT cover:
- sending email or messages
- deletes, including Dropbox deletes
- purchases or bank spend
- Dropbox writes (already gated on Chris's confirmation)
- anything risky

Those wait for Chris in the bot's own chat. Platform rules say only the user in the bot's own chat can authorize risky actions, and a relayed yes is second-hand.

### 4. No-agent Play publish: unchanged
Soft-FAIL any relay that asks to publish, submit, roll out, or promote. A spoken yes can't unlock this: Chris alone publishes. Eve should refuse to relay a publish ask as a go.

### 5. Live launches: the relay must respect it
Eve holds non-urgent asks while Chris is watching a launch, then batches them after. Same goes while he's driving: queue the non-urgent ones.

### 6. MiniME Allow cards wait until parked: Soft-PASS
The card can only be tapped in the requesting bot's own chat, and switching apps hangs up the call. A spoken yes only queues the action. Helios starts the card after Chris is parked and says he's ready.

### 7. Dual-write: Soft-PASS
This review is written locally, to Notion (under the Helios backup subpage), and to GitHub Live-Rocket-Tracker `docs/agent-backups/Helios/`.

## Test plan (Helios, first implementer)
Current state: MiniME (machineId ca6e5a63-2c21-4ed8-a336-4ee2c45aa687) shows **not connected**. The test is staged, not run.

1. When MiniME shows connected and Chris is on Eve's line, Helios sends Eve one line (template in `eve-relay/TEMPLATE.md`):
   `[HEL-YYYYMMDD-NN] Helios wants to list MiniME drives (read-only, undoable: yes). Why: backup-drive setup. Risk: low. Say yes or no.`
2. Eve speaks it and returns yes/no with the ask-id.
3. Helios logs every step's timestamp to `eve-relay/relay-log.jsonl`: sent, relayed, answered, parked, card shown, card approved.
4. Helios waits until Chris is parked, then starts the Allow card (a read-only drive listing).

Pass criteria (all three must hold):
- The yes arrives back through Eve with the matching ask-id.
- The call isn't dropped.
- The card is approved in Helios chat.

Fail if Chris has to switch apps mid-call, the answer can't be matched to the ask, or the card fires before he's parked.

## Suggested edits to the canonical text (for Perseus to apply)
1. **Step 1, CC Perseus:** add "The CC is for the pending-asks list only. Eve never waits on Perseus." That matches Chris's t1127u "not through Perseus".
2. **Step 1, line format:** add an ask-id and an undoable tag:
   `[<ASK-ID>] <Bot> needs <action> because <why>. Risk: <low/med/high>. Undoable: <yes/no>. Say yes or no.`
   Eve's reply: `[<ASK-ID>] yes|no`.
3. **Step 3, after "Queued, not done by voice":** add "Undoable: no → Eve doesn't take it as a voice go. She tells Chris it waits for his own chat with that bot." Also add Dropbox writes and deletes to the HARD-gated list. Chris already requires his confirmation for those.
4. **Step 3, publish line:** add "Eve refuses to relay a publish/submit/rollout ask as a go, even if Chris says yes."
5. **Step 6, pending-asks list:** use the fields from the Helios `eve-relay/relay-log.jsonl` schema (`eve-relay/SCHEMA.md`). That way the list and the requester log match.
6. **Test plan:** replace it with the pass criteria above: the yes arrives with the matching ask-id, the call isn't dropped, the card is approved in Helios chat after Chris has parked, and Chris heard the ask without leaving the call.
