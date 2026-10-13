# HARD: Cross-Agent Interrupt

Status: LOCKED Oct 13, 2026. Soft-PASS: Eve, Tinkabot, Helios (all edits applied).
Owner/maintainer: Perseus (fleet coordinator). First implementer/tester: Helios.
Drafted: 2026-10-13 (CT)

## Why this exists
On Oct 13, Chris was on a voice call with Eve when Helios needed MiniME access approval. The approval card only showed up in Helios's chat. Chris didn't know about it, switched chats to find it, and the Eve call hung up. Chris should never have to guess which bot needs something.

## Platform fact (honest limit)
Native approval cards (MiniME Shell/Read/CopyToBox, Auto-review cards, draft cards) appear ONLY in the requesting bot's own chat. No bot can move a card into another chat. This rule makes sure Chris HEARS about the ask on whatever line he's on. It doesn't move the card.

## How it works (direct path: Perseus is not in the live loop)
1. When any agent needs Chris's go, it sends SendToAgent priority:true to **Eve** (8d633572-a8c8-4944-807a-c212b491a370) and CCs **Perseus** with priority:false (informational, so Perseus isn't woken). The CC only feeds the pending-asks list; Eve never waits on Perseus.
   It's one voice-friendly line: `[ASK-ID] <Bot> needs <action> because <why>. Risk: <low/med/high>. Undoable: yes/no. Say yes or no.` Eve won't take an Undoable: no ask by voice.
2. Eve speaks it to Chris and relays his spoken yes/no back to the requester with SendToAgent priority:true (so the waiting bot wakes).
3. What a relayed verbal "yes" covers:
   - **Counts as Chris's go** only for low-risk, reversible actions inside a task he already approved.
   - **Queued, not done by voice:** anything that needs a native card (any MiniME command), and HARD-gated items: bank spend, sending external messages, deletions.
     - The requester holds the action.
     - Eve tells Chris: "<Bot> has a card waiting. Approve it when you're parked."
   - **Play Store publish/submit/rollout: NEVER, for any agent.** Chris alone publishes.
4. Timing:
   - Never interrupt during a live launch window Chris is watching.
   - While Chris is driving, queue anything non-urgent and batch it for when he parks.
5. The existing driving-proxy rule (route to Helios when Chris is unavailable) still applies to non-HARD items.
6. Perseus keeps this rule and a pending-asks list at /workspace/agent-backups/Perseus/interrupt-pending.json (bot, ask, time, status); this is the only queue. Requesters mark items resolved.

## Recommendation
Batch MiniME work into one sitting, so Chris approves cards once instead of getting interrupted over and over.

## Test plan (Helios)
The next time Helios needs MiniME, it pings Eve under this rule. Then it reports whether Chris heard the ask without leaving the call.

## HARD requirement: Allow cards visible across chats (Chris, Oct 13, 2026)
Chris calls this a MUST-HAVE that blocks fleet-wide work. Allow cards must show up wherever Chris is, including Eve's active voice line, where a pending card should be read aloud or shown without him switching chats. This is a platform change. The feature request went to the Grok Bot (SpaceXAI) team on Oct 13, 2026 at 6:24 AM CT. Product feedback gets no reference number. Until the platform supports it, the Eve relay and the hold-until-parked steps above stay in force.

## Helios edits (locked)
- A spoken yes never covers sends, deletes, spending, or Dropbox writes and deletes. Those wait for Chris in the requesting bot's own chat.
- Eve refuses any Play publish relay, even with a spoken yes.
- Answers carry the matching ASK-ID.
- The test is staged: when MiniME connects, Helios sends a read-only drive-listing ask. Helios's relay files are at /workspace/agent-backups/Helios/eve-relay/.
