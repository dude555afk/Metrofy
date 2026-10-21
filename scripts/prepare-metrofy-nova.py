#!/usr/bin/env python3
"""Prepare the pinned SoundPod checkout as the isolated Metrofy Nova preview."""

from pathlib import Path
import sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else "nova-source").resolve()
if not (root / "settings.gradle.kts").is_file():
    raise SystemExit(f"Not a SoundPod project root: {root}")


def replace_once(relative_path: str, old: str, new: str) -> None:
    path = root / relative_path
    content = path.read_text(encoding="utf-8")
    occurrences = content.count(old)
    if occurrences != 1:
        raise SystemExit(
            f"Expected one occurrence of {old!r} in {relative_path}, found {occurrences}"
        )
    path.write_text(content.replace(old, new, 1), encoding="utf-8")


replace_once(
    "app/build.gradle.kts",
    'applicationId = "com.github.soundpod"',
    'applicationId = "com.metrofy.nova"',
)
replace_once("app/build.gradle.kts", "versionCode = 32", "versionCode = 1")
replace_once(
    "app/build.gradle.kts",
    'versionName = "1.3.6"',
    'versionName = "0.1.0-nova.1"',
)
replace_once(
    "app/build.gradle.kts",
    "isUniversalApk = false",
    "isUniversalApk = true",
)
replace_once(
    "app/src/main/res/values/strings.xml",
    '<string name="app_name" translatable="false">SoundPod</string>',
    '<string name="app_name" translatable="false">Metrofy Nova</string>',
)
replace_once(
    "app/src/main/kotlin/com/github/soundpod/ui/screens/home/HomeScreen.kt",
    'text = "SoundPod",',
    'text = "Metrofy Nova",',
)

readme = root / "README.md"
readme_text = readme.read_text(encoding="utf-8")
marker = "## Metrofy Nova preview build"
if marker not in readme_text:
    readme_text += (
        "\n\n"
        + marker
        + "\n\n"
        + "This isolated preview is built from SoundPod source at commit "
        + "[03dcb75b0158eb5c8dccbd82a131e8bce09a876d]"
        + "(https://github.com/arunnechully/SoundPod/commit/03dcb75b0158eb5c8dccbd82a131e8bce09a876d). "
        + "The preview build changes the Android application ID to "
        + "`com.metrofy.nova`, the launcher name to **Metrofy Nova**, "
        + "the version to `0.1.0-nova.1`, and enables a universal APK. "
        + "SoundPod is licensed under GPL-3.0; see the included `LICENSE`. "
        + "The source and these preparation changes are included with the preview artifact.\n"
    )
    readme.write_text(readme_text, encoding="utf-8")

print(f"Prepared Metrofy Nova source in {root}")
