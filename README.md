# FLY8's Light Cloner

Private-beta rebuild of the EysClone prototype with the original UI/engine preserved as the build base.

## Beta changes

- Original prototype screens and VirtualApp/native engine remain the base.
- Branding changes to **FLY8's Light Cloner**.
- Activation/key gate is disabled for this private-beta build.
- Discord-facing text/URLs are removed.
- A Shizuku control option is injected into the Home and Control Setup screens.
- The existing privileged helper launch path is reused through a Shizuku UserService.

## Build

The GitHub Actions workflow runs on Ubuntu 24.04, installs JDK 17 + Android SDK, builds the small Shizuku bridge, decodes/patches/rebuilds the prototype, zipaligns it, signs it with an ephemeral beta key, and uploads the APK as an artifact.

The prototype APK is fetched from the previous FL8Y/LiteCloner repository during the build because GitHub's repository connector cannot transfer the binary attachment directly.

For a permanent self-contained repo, place the prototype at:

`vendor/EysClone-1.3.59.apk`

The build will prefer that local copy if present.

## Shizuku

The bridge targets Shizuku API 13.1.5. Start Shizuku on the test device, grant the app permission, then tap **Shizuku** inside the app.

## Private beta

This repository is intended for private testing. Do not publish activation keys, personal pairing information, or private tester data.
