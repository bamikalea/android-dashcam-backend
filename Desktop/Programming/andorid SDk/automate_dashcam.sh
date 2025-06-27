#!/bin/bash

# --- Configuration ---
# Set your APK path here if you want to install it automatically
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"  # Latest debug APK

# --- 1. Disable Hotspot (may require root) ---
echo "Disabling hotspot..."
adb shell svc wifi disable
adb shell svc usb setFunctions none
adb shell settings put global tether_dun_required 0
adb shell settings put global tethering_on 0
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS

# --- 2. Enable Mobile Data (may require root) ---
echo "Enabling mobile data (SIM internet)..."
adb shell svc data enable

# --- 3. (Optional) Install APK ---
if [ -f "$APK_PATH" ]; then
  echo "Installing APK: $APK_PATH"
  adb install -r "$APK_PATH"
else
  echo "APK install skipped. Set APK_PATH in the script if you want to auto-install."
fi

# --- 3.5. Re-disable and Block Unwanted Apps ---
# List of unwanted packages
UNWANTED_PACKAGES=(
  "com.car.cloud"
  "com.car.jt808service"
)

echo "Disabling and force-stopping unwanted apps..."
for pkg in "${UNWANTED_PACKAGES[@]}"; do
  adb shell pm disable-user --user 0 "$pkg"
  adb shell am force-stop "$pkg"
done

echo "Attempting to block network access for unwanted apps (if rooted and iptables available)..."
for pkg in "${UNWANTED_PACKAGES[@]}"; do
  uid=$(adb shell dumpsys package "$pkg" | grep userId= | awk -F= '{print $2}' | awk '{print $1}')
  if [ -n "$uid" ]; then
    adb shell su -c "iptables -A OUTPUT -m owner --uid-owner $uid -j DROP"
    adb shell su -c "iptables -A INPUT -m owner --uid-owner $uid -j DROP"
  fi
done

# --- 4. Reboot Device ---
echo "Rebooting device..."
adb reboot

echo "All steps completed: Hotspot disabled, mobile data enabled, APK installed (if provided), and device rebooted." 