# FLY8's Light Cloner

Private-beta Android build based on the supplied EysClone 1.3.59 prototype.

## Design goal

The build preserves the prototype APK's existing UI, resources, assets, native libraries, and compiled application engine. The beta layer is added separately instead of rebuilding the whole resource tree.

### Beta changes

- Product label: **FLY8's Light Cloner**
- Activation/key screen is bypassed at runtime for private testing.
- Discord/webhook-labelled UI controls are hidden at runtime.
- A small **Shizuku** control is added to the prototype Home and Control Setup screens.
- Shizuku starts the existing `io.virtualapp.standalone.HelperMain` control path with the privileges granted by Shizuku.

## Build pipeline

GitHub Actions uses Ubuntu 24.04, JDK 17, current Android build tools, and Gradle 9.5.1.

The production packaging pipeline builds the bridge, patches only the binary Android manifest, adds the bridge as `classes2.dex`, aligns, signs, verifies, and uploads the APK.

This avoids a full resource rebuild, which is important for preserving the prototype UI/resource table.

## Shizuku

The bridge targets Shizuku API 13.1.5. The app must be authorized by Shizuku before the control action can run.

## Private beta

This repository is intended for private testing. Do not commit production signing keys, private tester information, or license credentials.