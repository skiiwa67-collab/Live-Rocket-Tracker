# Grok voice chat UI: layout defects and silent failure with no diagnostic path

**Reporter:** Chris Olsen
**Date:** 2026-09-23 ~07:47 CT (America/Chicago)
**Env:** Android Grok app, voice mode (Helios agent)
**Screenshot:** user-attached layout capture (pinned avatar, external timestamp, redundant icons, red fail banner)

## 1. SUMMARY
Grok voice chat UI shows layout defects plus a "bot failed to respond" silent failure with no report/export mechanism.

## 2. LAYOUT
- Helios avatar pinned top-screen outside chat container, not draggable
- Conversation duration timestamp renders above chat box instead of inside it, also not draggable
- Chat box itself cannot be repositioned
- Three redundant Helios icons appear with no hierarchy

## 3. SILENT FAILURE
- App showed "bot failed to respond" with no button to send a diagnostic report, export chat logs, or indicate failure origin (client/app/server)
- Internal logs: not a crash, not a network drop; cause was a malformed reply tool call during tip140 verify missing a required field; no crash dump written locally or to Dropbox; session recoverable on new call

## 3b. ROOT CAUSE
Internal investigation traced the "bot failed to respond" event to a malformed reply tool call during a tip140 verify step that missed a required field. Not a process crash. Not a network drop. No crash dump was written locally or to Dropbox. Session was recoverable on a new call.
This may stem from fleet-side wiring (Helios or team agents) and is shared so xAI/Cursor can correlate.

## 4. REQUESTED FIXES
- Draggable chat container
- Timestamp inside chat box
- Remove or relocate top-bar icon
- Add report/export-logs button on failures
- Surface failure origin

## 5. REPRODUCTION
1. Open Grok voice chat
2. Observe pinned avatar and external timestamp
3. Try dragging chat box (fails)
4. Trigger failure and note missing report option

## 6. ENV
Android Grok app, voice mode, reporter Chris Olsen; reference his screenshot of the layout.


## 7. ADDENDUM — VOICE PIPELINE PREEMPTS APPROVAL CARDS
See follow-up emails 1a0ce54be45a2e55 (Cursor) and 1a0ce54cac5f2dbf (xAI).
