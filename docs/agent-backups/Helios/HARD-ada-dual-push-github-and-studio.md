# HARD — Ada dual-push: GitHub AND Android Studio every tip

**Chris voice HARD 2026-09-23 ~09:47 CT**

## Rule
Every tip Ada ships must land in **both** destinations in the **same step**:

1. **GitHub** `skiiwa67-collab/Live-Rocket-Tracker` branch `stamp-85` (backup / remote tiphead)
2. **MiniME Android Studio working copy**  
   `C:\Users\skiiw\AndroidStudioProjects\RetroCommandCenter`

GitHub-only = **FAIL**. Studio is what Chris actually opens.

## Done criteria before “Studio ready”
- `git log -1` on MiniME matches GitHub tiphead hash
- `app/build.gradle.kts` on MiniME shows the tip’s `versionCode` / `versionName`
- Chris (or Helios) confirms Studio editor shows that version after Gradle sync — not a stale buffer

## Soft-FAIL
- phone / adb / Play / MiniME device until Chris go (unchanged)
- Claiming READY while MiniME still behind origin

## Owners
- **Ada:** dual-push every tip
- **Helios:** audit both destinations before telling Chris READY
