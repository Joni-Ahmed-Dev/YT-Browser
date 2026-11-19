# YT Browser

Android YouTube desktop-style browser starter.

## Stack
- Kotlin and Android WebView
- Desktop-style User-Agent based on the installed Android System WebView
- Chrome Custom Tabs handoff for Google sign-in
- Android native Picture-in-Picture request during fullscreen playback

## GitHub Actions APK build
Push to main or manually run Android APK Build from the Actions tab.
Download yt-browser-debug-apk from the workflow run's Artifacts section.

## Important limitations
- This project does not bundle Chrome. Rendering uses the device's Android System WebView provider.
- Chrome Custom Tabs and WebView have separate cookie stores. Signing in through Chrome Custom Tabs does not automatically sign the embedded WebView into YouTube.
- PiP is requested when the YouTube player enters fullscreen and the user leaves the app. YouTube behavior and device policy can affect availability; it is not guaranteed for every video.
- This is an initial development build, not a Play Store-ready release.
