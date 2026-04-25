# Lock-screen alarm debug report

**Status: unresolved as of 2026-04-19 02:51 PDT**

The alarm rings correctly when Zzzt is **backgrounded** before locking the phone.
It fails when Zzzt is **foregrounded** before locking: the screen doesn't wake,
the AlarmRingActivity is launched briefly under the hood but closes itself within
~1 second, and the user only sees a notification appear after manually unlocking.

This report lists what's been tried, what the logs show, and what to investigate
next.

## Repro

1. Open Zzzt → Manage → New alarm → set for ~2 min out → Enabled + Starred → Save.
2. Return to Bedtime screen (Zzzt foregrounded).
3. Lock the phone with the power button.
4. Wait for the alarm.

**Observed**: display stays off through the trigger time. Later, unlocking the
phone surfaces an alarm notification ~2 seconds after unlock. MainActivity
(Bedtime view) is the top activity, not AlarmRingActivity.

**Expected** (like Google Clock): display wakes, AlarmRingActivity appears
over the lock screen with the Stop button + ringtone playing.

## What we built (current architecture)

Following AOSP DeskClock's pattern:

```
AlarmManager.setAlarmClock(info, broadcastPI)
  → AlarmReceiver.onReceive
    → context.startForegroundService(AlarmRingService)
    → also does one-shot disable / recurring re-arm
  → AlarmRingService.onStartCommand (start)
    → ServiceCompat.startForeground(NOTIF_ID, notif, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    → notif has setFullScreenIntent(activityPI, true) + category = CATEGORY_ALARM
    → plays ringtone on STREAM_ALARM
  → AlarmRingActivity launched by the FSI
    → setShowWhenLocked(true) + setTurnScreenOn(true) + FLAG_KEEP_SCREEN_ON
    → Stop button calls AlarmRingService.stop() then finish()
```

Manifest:
- Activity has `showWhenLocked="true"`, `turnScreenOn="true"`,
  `launchMode="singleInstance"`, `excludeFromRecents="true"`, `taskAffinity=""`.
- Service has `foregroundServiceType="specialUse"` + the required
  `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`.
- Permissions: `USE_FULL_SCREEN_INTENT`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`,
  `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `WAKE_LOCK`,
  `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`.

## What we've tried, in order

| Attempt | Change | Outcome |
|---|---|---|
| 1 | `AlarmReceiver` → `context.startActivity(AlarmRingActivity)` directly | BAL-blocked on Android 14+. Log: `isPendingIntent: false, autoOptInReason: notPendingIntent`. |
| 2 | `AlarmScheduler.buildFireIntent` returns `PendingIntent.getActivity(AlarmRingActivity)` | Still BAL-blocked. Log: `balAllowedByPiCreator: BSP.NONE, balRequireOptInByPendingIntentCreator: true`. |
| 3 | `ActivityOptions.setPendingIntentBackgroundActivityStartMode(ALLOWED)` when creating PI | Still BAL-blocked with same reason. |
| 4 | Rewrote to AOSP DeskClock pattern: receiver → foreground service → FSI notification | Service didn't retain FGS status, activity torn down. Log: `FGS stop call ... has no types!`. |
| 5 | Added `ServiceCompat.startForeground(id, notif, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)` | Alarm rings correctly on **emulator** (locked screen, silent). |
| 6 | Forced `USAGE_ALARM` on Ringtone audio attributes | Emulator audio plays on STREAM_ALARM correctly. |
| 7 | Added `FLAG_KEEP_SCREEN_ON` to AlarmRingActivity window | No change on phone — activity was closing in under 1 s, far before any screen-off timer. |
| 8 | Changed manifest attr `showOnLockScreen` → correct `showWhenLocked` | No change. |
| 9 | Confirmed `USE_FULL_SCREEN_INTENT` was **rejected** on phone via `appops get` | Forced via `adb shell appops set io.dupuis.zzzt USE_FULL_SCREEN_INTENT allow`. |
| 10 | Added in-app warning banner + Settings deep-link when permission not granted | Banner works; user test still fails even with permission **explicitly allowed**. |

## What the phone logs actually showed

From `logcat` window around the alarm-fire moment (phone, 02:42:11):

```
02:42:11.404  I LegacyActivityStarterInternalImpl: Invoking dismissKeyguardThenExecute, afterKeyguardGone: false
02:42:11.414  I ActivityTaskManager: START ... cmp=io.dupuis.zzzt/.alarm.AlarmRingActivity
                                     (BAL_ALLOW_NON_APP_VISIBLE_WINDOW [realCaller]) result code=0
02:42:11.471  I ActivityTaskManager: Displayed io.dupuis.zzzt/.alarm.AlarmRingActivity for user 0: +63ms
02:42:11.977  D VRI[MainActivity]:   visibilityChanged oldVisibility=true newVisibility=false
02:42:11.979  D KeyguardService:     setOccluded(true)
02:42:12.305  V WindowManagerShell:  Transition requested (#7517): CLOSE [AlarmRingActivity]
02:42:12.348  W TransitionChain:     Combining kgOccludeChg into #7517(CLOSE|R|0x0) from AR.finish-app-request
02:42:12.338  W ForegroundServiceTypeLoggerModule: FGS stop call for: 10490 has no types!
02:42:12.931  D KeyguardService:     setOccluded(false)
```

Key facts:
- BAL exemption **was** granted (`BAL_ALLOW_NON_APP_VISIBLE_WINDOW`) — the
  activity was allowed to launch.
- `dismissKeyguardThenExecute` was called before launch.
- Activity displayed successfully (+63 ms).
- Keyguard became **occluded** by AlarmRingActivity (state change confirmed).
- **~834 ms later, the activity was CLOSEd via `AR.finish-app-request`** —
  this is Android's tag for "the app's own ActivityRecord requested finish."
  We do not have any `finish()` call on this path.
- Display never turned on for the user despite `setTurnScreenOn(true)` being
  set and the activity being marked visible.
- `FGS stop call ... has no types!` appeared even though we're now calling
  `startForeground(..., FOREGROUND_SERVICE_TYPE_SPECIAL_USE)` with the correct
  type — possibly logged during the service tear-down path rather than the
  start path.
- Android version on the phone: **16** (from `ro.build.version.release`).

## What is NOT the problem

- BAL. It's being granted.
- Foreground service type. The service starts fine on the emulator with the same
  code, and foreground started successfully per `Background started FGS: Allowed`.
- Ringtone routing. STREAM_ALARM is being used now.
- The `showWhenLocked` attribute. Correct name, also set programmatically.
- Missing permission in the AndroidManifest. All expected entries are present.
- `USE_FULL_SCREEN_INTENT` being in "default" (rejected) state — we forced
  it to "allow" via adb and behavior didn't change.

## Main open questions

1. **Who is calling `finish()` on AlarmRingActivity?** The `AR.finish-app-request`
   tag means the activity's own process requested the finish, but we do not
   explicitly call `finish()` outside the Stop button. Candidates to
   investigate:
   - Something in `setContent { AlarmRingContent(...) }` that throws, causing
     the system to tear down the activity silently. Compose + `LocalContext`
     + `ZzztApp` cast + `container.alarmRepository.getById` all run on launch.
   - `launchMode="singleInstance"` interaction with MainActivity's existing
     task when both belong to the same uid. Worth trying `singleTask` or the
     default mode.
   - `taskAffinity=""` forcing a brand-new task that the system then dismisses
     when the keyguard un-occludes.

2. **Why doesn't `setTurnScreenOn(true)` wake the display on this device?**
   Google Pixel phones on Android 14+ have tightened when an app is allowed
   to wake the screen. Possibilities:
   - The `USE_FULL_SCREEN_INTENT` permission flip via `appops set` may not
     give the full grant the system treats as "alarm clock category". The
     way apps get auto-categorized as alarm-clocks on Android 14+ isn't
     entirely public.
   - The activity might be winning the BAL race but losing the
     "can wake screen" check, which is a separate policy path
     (`PowerManagerService.ShouldWakeUp`).

3. **Why does it work on the emulator but not the phone?** Emulators do not
   enforce keyguard / screen-wake policies the same way. The emulator test
   verifies the receiver → service → FSI chain works in principle; the phone
   tests the real policy. The bug is phone-only.

## Diagnostic commands to run tomorrow

```bash
# Confirm FSI permission state
adb shell appops get io.dupuis.zzzt USE_FULL_SCREEN_INTENT

# Confirm alarm is actually scheduled with setAlarmClock
adb shell dumpsys alarm | grep -B1 -A5 'io.dupuis.zzzt'

# Fire a test alarm 20 seconds out (silent, debug builds only)
adb shell am start -n io.dupuis.zzzt/.alarm.DebugAlarmTrigger --ei sec 20

# Pull logs filtered on the relevant tags after a failed test
adb logcat -d -t 3000 | grep -iE 'dupuis.zzzt|AlarmRing|FGS|FullScreen|BAL|keyguard|AR\.finish|walarm'

# Check notification channel settings
adb shell dumpsys notification --noredact | grep -A20 'io.dupuis.zzzt'

# See power manager / screen wake reasoning
adb shell dumpsys power | grep -iE 'wakefulness|wakeLock|lastWake'
```

## Things worth trying next (ranked by likelihood)

1. **Remove `launchMode="singleInstance"` and `taskAffinity=""`.** Let it use
   a default task. See if AlarmRingActivity stays up.
2. **Acquire a `SCREEN_BRIGHT_WAKE_LOCK` in `AlarmRingService.onStartCommand`
   for ~5 s before the FSI fires.** Deprecated but still honored. This is what
   Telegram / Twilio do. Release after a short timeout.
3. **Add `Log.d` calls to every lifecycle method of `AlarmRingActivity`**
   (`onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onDestroy`) and
   re-run the test. The log will show whether `onStop`/`onDestroy` gets
   called without our intervention.
4. **Try without the `singleInstance`/`taskAffinity` combo but KEEP
   `excludeFromRecents` and `showWhenLocked`.** Some reports tie the finish
   to empty task affinity.
5. **Check `canUseFullScreenIntent()` at the moment the alarm fires
   (in `AlarmRingService.start`).** If false, post a normal heads-up
   notification instead, so at least the user sees something. Currently the
   service just builds the FSI regardless.
6. **Declare `<application android:appCategory="accessibility">`** or the
   like in the manifest. This can influence how the system categorizes the
   app for screen-wake and FSI policies.
7. **Consider a different notification strategy**: use
   `NotificationCompat.Builder.setVisibility(VISIBILITY_PUBLIC)` and ensure
   the channel is `IMPORTANCE_HIGH` (it is). Verify the channel has lights
   and vibration enabled.

## File map for the alarm ringing path

- `app/src/main/java/io/dupuis/zzzt/alarm/AlarmScheduler.kt` — builds
  `AlarmClockInfo` + broadcast fire PendingIntent for `AlarmReceiver`.
- `app/src/main/java/io/dupuis/zzzt/alarm/AlarmReceiver.kt` — receives
  broadcast, calls `startForegroundService(AlarmRingService)`, handles
  one-shot/recurring re-arm.
- `app/src/main/java/io/dupuis/zzzt/alarm/AlarmRingService.kt` — posts FSI
  notification, holds wake-lock, plays ringtone.
- `app/src/main/java/io/dupuis/zzzt/alarm/AlarmRingActivity.kt` — UI shown
  by FSI (or when user taps the notification body).
- `app/src/main/java/io/dupuis/zzzt/alarm/BootReceiver.kt` — re-schedules
  on `BOOT_COMPLETED`.
- `app/src/debug/java/io/dupuis/zzzt/alarm/DebugAlarmTrigger.kt` — adb
  entry point: `am start -n io.dupuis.zzzt/.alarm.DebugAlarmTrigger
  --ei sec 20`.
- `app/src/main/AndroidManifest.xml` — permissions + component declarations.
- `app/src/main/java/io/dupuis/zzzt/ui/bedtime/BedtimeScreen.kt` —
  `FullScreenIntentBanner` composable that surfaces the FSI permission
  warning to the user.

## Test coverage status

- Pure logic: 31 unit tests green (`AlarmTimeCalc`: 21, `BedtimeNextAlarm`:
  10).
- No tests exist for `AlarmScheduler`, `AlarmReceiver`, `AlarmRingService`,
  or `AlarmRingActivity` (would need Robolectric or instrumented tests).
- Emulator smoke test: lock-screen ring path passes with the current code.
- Phone smoke test: lock-screen ring path **fails** with Zzzt foregrounded
  before lock; **passes** when Zzzt is backgrounded before lock.
