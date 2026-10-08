#!/usr/bin/env python3
import re
import shutil
import subprocess
import sys
from pathlib import Path

APKTOOL = Path(sys.argv[1]).resolve()
SOURCE = Path(sys.argv[2]).resolve()
OUTPUT = Path(sys.argv[3]).resolve()
BRANDING = Path(sys.argv[4]).resolve()

WORK = Path("work")
DECODED = WORK / "decoded"

if WORK.exists():
    shutil.rmtree(WORK)
WORK.mkdir(parents=True)

subprocess.run(
    ["java", "-jar", str(APKTOOL), "d", "-r", str(SOURCE), "-o", str(DECODED), "-f"],
    check=True,
)

def locate_smali(descriptor):
    rel = descriptor[1:-1] + ".smali"
    for root in DECODED.glob("smali*"):
        candidate = root / rel
        if candidate.exists():
            return candidate
    return None

# Fix the previous crash: Android requires Activity.onCreate() to call super.
target = locate_smali("Lio/virtualapp/license/ActivationActivity;")
if target is None:
    raise RuntimeError("ActivationActivity.smali was not found")

text = target.read_text(encoding="utf-8", errors="ignore")
pattern = re.compile(
    r"(?ms)^(\.method\s+[^\n]*onCreate\(Landroid/os/Bundle;\)V\n)"
    r".*?^\.end method\s*$"
)
match = pattern.search(text)
if not match:
    raise RuntimeError("ActivationActivity.onCreate(Bundle) was not found")

replacement = (
    match.group(1)
    + "    .locals 2\n"
    + "    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V\n"
    + "    const-class v0, Lio/virtualapp/home/NewHomeActivity;\n"
    + "    new-instance v1, Landroid/content/Intent;\n"
    + "    invoke-direct {v1, p0, v0}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V\n"
    + "    const/high16 v0, 0x4000000\n"
    + "    invoke-virtual {v1, v0}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;\n"
    + "    invoke-virtual {p0, v1}, Landroid/app/Activity;->startActivity(Landroid/content/Intent;)V\n"
    + "    invoke-virtual {p0}, Landroid/app/Activity;->finish()V\n"
    + "    return-void\n"
    + ".end method"
)
target.write_text(text[:match.start()] + replacement + text[match.end():], encoding="utf-8")

# Replace user-visible Eys/EYS branding while deliberately preserving
# internal package/class identifiers such as io/eysclone for compatibility.
for smali in DECODED.glob("smali*/**/*.smali"):
    try:
        s = smali.read_text(encoding="utf-8", errors="ignore")
        s2 = s.replace("EysClone", "FLY8").replace("EYS", "FLY8").replace("Eys", "FLY8")
        if s2 != s:
            smali.write_text(s2, encoding="utf-8")
    except UnicodeDecodeError:
        pass

# Use the supplied image as the launcher artwork. These are the prototype's
# existing icon resources, so no new resource-table entries are required.
for name in ("Dj.webp", "iN.webp", "7E.webp", "Vd.webp", "xC.webp"):
    dst = DECODED / "res" / name
    if dst.exists():
        shutil.copyfile(BRANDING, dst)

subprocess.run(
    ["java", "-jar", str(APKTOOL), "b", str(DECODED), "-o", str(OUTPUT)],
    check=True,
)

if not OUTPUT.is_file() or OUTPUT.stat().st_size == 0:
    raise RuntimeError("Apktool produced no output APK")

print("Patched ActivationActivity:", target)
print("Applied FLY8 branding")
print("Applied supplied launcher artwork")
print("Created:", OUTPUT)
