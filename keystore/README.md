# Debug Keystore

This directory contains the shared debug keystore for the PetExplorer Android app.

## Purpose
This keystore ensures that all developers have the same SHA-1 fingerprint when building the app, which is required for Google Sign-In to work correctly.

## Keystore Details
- **File**: `debug.keystore`
- **Alias**: `androiddebugkey`
- **Store Password**: `android`
- **Key Password**: `android`

## SHA-1 Fingerprint
```
E3:FF:4F:44:58:9D:C9:7D:39:EB:0B:B1:5E:4F:A8:D1:36:B0:0A:27
```

## Google Cloud Console Setup

**IMPORTANT**: To enable Google Sign-In, you MUST register this SHA-1 fingerprint in Google Cloud Console:

1. Go to: https://console.cloud.google.com/
2. Select your project
3. Navigate to: **APIs & Services → Credentials**
4. Find or create an **OAuth 2.0 Client ID** for Android
5. Configure it with:
   - **Application type**: Android
   - **Package name**: `petexplorer.petexplorerclients`
   - **SHA-1 certificate fingerprint**: `E3:FF:4F:44:58:9D:C9:7D:39:EB:0B:B1:5E:4F:A8:D1:36:B0:0A:27`

## For New Developers

After cloning the repository:
1. The keystore is already configured in `build.gradle`
2. Just build and run the app - it will automatically use this keystore
3. No additional setup needed!

## Security Note

This is a **debug keystore only** - safe to commit to version control.
**NEVER** commit production/release keystores to git.
