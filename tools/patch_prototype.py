#!/usr/bin/env python3
import re
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path
import xml.etree.ElementTree as ET

APKTOOL = Path(sys.argv[1]).resolve()
SOURCE = Path(sys.argv[2]).resolve()
OUTPUT = Path(sys.argv[3]).resolve()
BRIDGE_APK = Path(sys.argv[4]).resolve()

WORK = Path("work")
DECODED = WORK / "decoded"
if WORK.exists():
    shutil.rmtree(WORK)
WORK.mkdir(parents=True)

subprocess.run(
    ["java", "-jar", str(APKTOOL), "d", "-r", str(SOURCE), "-o", str(DECODED), "-f"],
    check=True
)

ANDROID_NS = "http://schemas.android.com/apk/res/android"
ANDROID = "{" + ANDROID_NS + "}"

manifest = DECODED / "AndroidManifest.xml"
tree = ET.parse(manifest)
root = tree.getroot()
app = root.find("application")
if app is None:
    raise RuntimeError("application element not found")

package_name = root.get("package")
if not package_name:
    raise RuntimeError("prototype package is missing")

# Keep package and VirtualApp authorities intact for compatibility.
app.set(ANDROID + "label", "FLY8's Light Cloner")

def add_manifest_component(tag, name, **attrs):
    for node in app.findall(tag):
        if node.get(ANDROID + "name") == name:
            return
    node = ET.Element(tag)
    node.set(ANDROID + "name", name)
    for k, v in attrs.items():
        node.set(ANDROID + k, v)
    app.append(node)

add_manifest_component(
    "provider",
    "rikka.shizuku.ShizukuProvider",
    authorities=package_name + ".shizuku",
    exported="true",
    multiprocess="false",
    permission="android.permission.INTERACT_ACROSS_USERS_FULL",
)

ET.register_namespace("android", ANDROID_NS)
tree.write(manifest, encoding="utf-8", xml_declaration=True)

# Preserve prototype resources verbatim for 1:1 UI compatibility.\n\ndef locate_smali(class_descriptor):
    class_path = class_descriptor[1:-1] + ".smali"
    for rootdir in DECODED.glob("smali*"):
        candidate = rootdir / class_path
        if candidate.exists():
            return candidate
    return None

def inject_after_registers(smali_file, invocation):
    if smali_file is None:
        return False
    text = smali_file.read_text(encoding="utf-8")
    if invocation in text:
        return True

    method = re.search(
        r"(?m)^(\.method[^\n]* onCreate\(Landroid/os/Bundle;\)V\n)(.*?)(^\.end method\s*)",
        text,
        re.S,
    )
    if not method:
        return False

    body = method.group(2)
    reg = re.search(r"(?m)^\s*\.(?:locals|registers)\s+\d+\s*$", body)
    if not reg:
        return False

    insert_at = method.start(2) + reg.end()
    new_text = text[:insert_at] + "\n    " + invocation + "\n" + text[insert_at:]
    smali_file.write_text(new_text, encoding="utf-8")
    return True

# Preserve the original UI screens, but insert beta behavior into the
# prototype's own splash/home/control setup Activities.
inject_after_registers(
    locate_smali("Lio/virtualapp/splash/SplashActivity;"),
    "invoke-static {p0}, Lcom/fl8y/lightcloner/bridge/BetaBootstrap;->onSplash(Landroid/app/Activity;)V",
)
inject_after_registers(
    locate_smali("Lio/virtualapp/home/NewHomeActivity;"),
    "invoke-static {p0}, Lcom/fl8y/lightcloner/bridge/ShizukuBridge;->install(Landroid/app/Activity;)V",
)
inject_after_registers(
    locate_smali("Lio/virtualapp/standalone/ControlSetupActivity;"),
    "invoke-static {p0}, Lcom/fl8y/lightcloner/bridge/ShizukuBridge;->install(Landroid/app/Activity;)V",
)

# Disable boolean license/subscription gates in the dedicated license package
# for this private-beta build. This intentionally does not touch unrelated
# engine classes.
license_dir = DECODED / "smali" / "io" / "virtualapp" / "license"
for rootdir in DECODED.glob("smali*"):
    license_dir = rootdir / "io" / "virtualapp" / "license"
    if not license_dir.exists():
        continue
    for smali_file in license_dir.rglob("*.smali"):
        if smali_file.name == "ActivationActivity.smali":
            continue
        text = smali_file.read_text(encoding="utf-8", errors="ignore")
        chunks = re.split(r"(?m)(?=^\.method\b)", text)
        changed = False
        out = []
        for chunk in chunks:
            header = re.match(r"(?s)^\.method[^\n]+", chunk)
            if not header or not header.group(0).rstrip().endswith(")Z"):
                out.append(chunk)
                continue
            if not re.search(r"(?i)license|activat|subscription|allow|valid|verif", chunk):
                out.append(chunk)
                continue

            first_line_end = chunk.find("\n") + 1
            method_header = chunk[:first_line_end]
            end_marker = chunk.rfind(".end method")
            if end_marker == -1:
                out.append(chunk)
                continue

            body = (
                method_header
                + "    const/4 v0, 0x1\n"
                + "    return v0\n"
                + chunk[end_marker:]
            )
            out.append(body)
            changed = True

        if changed:
            smali_file.write_text("".join(out), encoding="utf-8")

# Remove explicit activation entry from the manifest to keep the key screen
# unreachable, while leaving the Activity available for the redirect hook.
for activity in app.findall("activity"):
    if activity.get(ANDROID + "name") == "io.virtualapp.license.ActivationActivity":
        activity.set(ANDROID + "exported", "false")

ET.register_namespace("android", ANDROID_NS)
tree.write(manifest, encoding="utf-8", xml_declaration=True)

WORK.mkdir(exist_ok=True)
if not BRIDGE_APK.exists():
    raise RuntimeError("Bridge APK not found: " + str(BRIDGE_APK))

# Rebuild the original prototype unchanged except for the targeted patches.
UNSIGNED = WORK / "repacked-unsigned.apk"
subprocess.run(
    ["java", "-jar", str(APKTOOL), "b", str(DECODED), "-o", str(UNSIGNED)],
    check=True,
)

# Append bridge dex as classes2.dex; the final workflow signs after this step.
with zipfile.ZipFile(BRIDGE_APK, "r") as z:
    bridge_dex = z.read("classes.dex")

with zipfile.ZipFile(UNSIGNED, "a", compression=zipfile.ZIP_DEFLATED) as z:
    z.writestr("classes2.dex", bridge_dex)

shutil.copy2(UNSIGNED, OUTPUT)
print("Created", OUTPUT)
print("Prototype package:", package_name)
