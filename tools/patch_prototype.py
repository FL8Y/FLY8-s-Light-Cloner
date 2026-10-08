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

# No original resources or DEX are decompiled. Apktool -r preserves resources.arsc
# and -s keeps the prototype DEX files untouched; only the manifest is changed.
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
