#!/bin/sh
# Phase 5 verification helper (run on the machine the TV is plugged into, e.g. Termux):
#
#   ./tools/tv-phase5-check.sh app-foss-release.apk phase5-out
#
# It installs the APK, then collects the evidence Gate 5 asks for and cannot be gathered here:
# focused-element dumps after D-pad presses (P5-4 focus order), gfxinfo numbers (P5-3) and
# screenshots (P5-7). Nothing is modified on the device beyond installing/launching the app.
set -u

PKG=com.craftworks.music
APK=${1:-}
OUT=${2:-phase5-out}

command -v adb >/dev/null 2>&1 || { echo "adb is not on PATH" >&2; exit 1; }
mkdir -p "$OUT"

echo "== device =="
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell dumpsys tv_input 2>/dev/null | head -3
# uiMode with TELEVISION set means the TV branches are active (a phone will show car/normal).
adb shell dumpsys activity | grep -m1 -i "mCurrentConfig\|uiMode" | head -2

if [ -n "$APK" ]; then
  echo "== install =="
  if ! adb install -r "$APK"; then
    cat <<'EOF' >&2

Install refused. INSTALL_FAILED_UPDATE_INCOMPATIBLE means the installed copy was signed with a
different certificate (each old CI build used a fresh debug key). Remove it once and re-run:

    adb uninstall com.craftworks.music

Every build after that reuses the same key, so updates install in place.
EOF
    exit 2
  fi
fi

echo "== launch =="
adb shell am force-stop "$PKG"
adb shell am start -n "$PKG/.MainActivity" >/dev/null
sleep 6

# Dumps the node that currently holds focus, which is the whole point of the D-pad checks.
focused_node() {
  adb shell uiautomator dump /sdcard/window_dump.xml >/dev/null 2>&1
  adb shell cat /sdcard/window_dump.xml 2>/dev/null \
    | tr '>' '\n' \
    | grep 'focused="true"' \
    | sed -e 's/.*text="\([^"]*\)".*/\1/' -e 's/.*content-desc="\([^"]*\)".*/\1/' \
    | head -1
}

echo "== focus walk (P5-4) =="
for key in DPAD_DOWN DPAD_DOWN DPAD_RIGHT DPAD_DOWN DPAD_UP; do
  adb shell input keyevent "$key"
  sleep 1
  printf '%-12s -> %s\n' "$key" "$(focused_node)"
  adb exec-out screencap -p > "$OUT/focus-$key.png"
done

echo "== gfxinfo (P5-3) =="
adb shell dumpsys gfxinfo "$PKG" reset >/dev/null
for _ in 1 2 3 4 5 6 7 8 9 10; do
  adb shell input keyevent DPAD_DOWN
  adb shell input keyevent DPAD_UP
done
adb shell dumpsys gfxinfo "$PKG" | grep -E "Total frames rendered|Janky frames|50th percentile|90th percentile|95th percentile|99th percentile" | head -8

echo "== signatures =="
adb shell dumpsys package "$PKG" | grep -A3 -m1 "signatures" | head -6
if [ -n "$APK" ] && command -v apksigner >/dev/null 2>&1; then
  apksigner verify --print-certs "$APK" | grep -m1 "SHA-256 digest"
fi

echo "== screenshots =="
for screen in home library browse nowplaying settings; do
  adb exec-out screencap -p > "$OUT/$screen.png"
done
echo "Saved $OUT/*.png — attach them to the Phase 5 log."
