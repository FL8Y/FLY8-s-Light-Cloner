# Publishing

The repository is configured for private beta releases through GitHub Actions.

Push a version tag such as `v1.0.0-beta1` to create a GitHub release containing the signed APK.

The workflow uses an ephemeral CI signing key for beta artifacts. For a production release, replace this with a protected repository secret and a persistent release keystore.