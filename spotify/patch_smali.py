"""Best-effort smali patches for Spotify APK ad removal.

Strategy:
1. Stub onCreate() of all com.spotify.adsdisplay.* and com.spotify.adsinternal.adscommon.inappbrowser.*
   Activities so that they immediately call finish() instead of rendering ads.
2. Replace bodies of selected ad-trigger smali files with empty no-ops (best effort).

This will not block audio ads (those are baked into the audio stream) but should remove
visual interstitial / display ads. Login flow is left untouched.
"""

from __future__ import annotations
import os
import re
import sys
from pathlib import Path

ROOT = Path(os.environ.get("SPOTIFY_PATCH_ROOT", "/home/ubuntu/spotify-patch/spotify-decompiled"))

# Activities that wrap visible ad UI — we want them to finish immediately.
TARGET_ACTIVITIES = [
    "smali_classes2/com/spotify/adsdisplay/display/DisplayAdActivity.smali",
    "smali_classes2/com/spotify/adsdisplay/products/cmp/CMPActivity.smali",
    "smali_classes2/com/spotify/adsinternal/adscommon/inappbrowser/InAppBrowserLauncherActivity.smali",
    "smali_classes2/com/spotify/adsdisplay/browser/inapp/InAppBrowserActivity.smali",
    "smali_classes6/com/spotify/nativeadshomeformats/nativeadshomeformats/impl/help/HelpWebViewActivity.smali",
]


ONCREATE_RE = re.compile(
    r"\.method\s+public\s+(?:final\s+)?onCreate\(Landroid/os/Bundle;\)V\b"
)


def stub_oncreate(smali_path: Path) -> bool:
    """Replace onCreate body with: super.onCreate(bundle); finish(); return."""
    if not smali_path.exists():
        print(f"  SKIP (missing): {smali_path}")
        return False
    text = smali_path.read_text()
    m = ONCREATE_RE.search(text)
    if not m:
        print(f"  SKIP (no onCreate): {smali_path.name}")
        return False

    # Find super class.
    super_m = re.search(r"^\.super\s+(L[^;]+;)", text, re.MULTILINE)
    if not super_m:
        print(f"  SKIP (no super): {smali_path.name}")
        return False
    super_cls = super_m.group(1)

    method_start = m.start()
    # Find matching .end method.
    end_m = re.search(r"^\.end method", text[method_start:], re.MULTILINE)
    if not end_m:
        print(f"  SKIP (no end method): {smali_path.name}")
        return False
    method_end = method_start + end_m.end()

    new_method = (
        ".method public final onCreate(Landroid/os/Bundle;)V\n"
        "    .locals 0\n\n"
        f"    invoke-super {{p0, p1}}, {super_cls}->onCreate(Landroid/os/Bundle;)V\n\n"
        "    invoke-virtual {p0}, Landroid/app/Activity;->finish()V\n\n"
        "    return-void\n"
        ".end method"
    )
    new_text = text[:method_start] + new_method + text[method_end:]
    smali_path.write_text(new_text)
    print(f"  PATCHED: {smali_path.relative_to(ROOT)}")
    return True


def main() -> int:
    patched = 0
    for rel in TARGET_ACTIVITIES:
        if stub_oncreate(ROOT / rel):
            patched += 1
    print(f"Done. {patched}/{len(TARGET_ACTIVITIES)} activities stubbed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
