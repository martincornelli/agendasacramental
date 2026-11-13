// Run against an installed, optimized APK, including APKs built from a release AAB.
// Example: node tests/release-startup.cjs emulator-5554 com.martincornelli.agendasacramental
const { execFileSync } = require("node:child_process");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const adb = process.env.ADB || path.join(process.env.USERPROFILE, "AppData/Local/Android/Sdk/platform-tools/adb.exe");
const [serial, appId] = process.argv.slice(2);
assert.ok(serial && /^com\.martincornelli\.agendasacramental(\.qa)?$/.test(appId), "Provide device serial and app ID");
const run = (...args) => execFileSync(adb, ["-s", serial, ...args], { encoding: "utf8", timeout: 30000 });
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
(async () => {
  const folder = path.join("output", "release-startup", serial);
  fs.mkdirSync(folder, { recursive: true });
  for (let attempt = 1; attempt <= 3; attempt++) {
    run("shell", "am", "force-stop", appId);
    const result = run("shell", "am", "start", "-W", "-n", appId + "/com.example.agendasacramental.MainActivity");
    assert.ok(!/Error:|Exception/.test(result), result);
    await delay(5000);
    const pid = run("shell", "pidof", appId).trim();
    assert.match(pid, /^\d+$/);
    const logs = run("logcat", "-d", "--pid=" + pid);
    fs.writeFileSync(path.join(folder, "launch-" + attempt + ".log"), logs);
    assert.ok(!/FATAL EXCEPTION|Failed to create an instance|Could not instantiate/.test(logs), "Startup crash in logcat");
    const activity = run("shell", "dumpsys", "activity", "activities");
    assert.ok(activity.split("\n").some(line => /mResumedActivity|topResumedActivity/.test(line) && line.includes(appId)), "App must be foreground");
    console.log("Cold start", attempt, "passed; PID", pid);
  }
  console.log("Optimized app survived three cold starts:", appId);
})().catch(error => { console.error(error.message); process.exitCode = 1; });
