# Walking Skeleton 2 — Native Dependency Setup

Run these commands from the `mobile/` directory to install the new native dependencies:

```sh
# Google Sign-In
npx expo install @react-native-google-signin/google-signin

# Apple Authentication (already in Expo SDK but needs explicit install)
npx expo install expo-apple-authentication
```

After installing, update `app.json` with the required config:

```json
{
  "expo": {
    "plugins": [
      "@react-native-google-signin/google-signin",
      "expo-apple-authentication"
    ],
    "extra": {
      "googleWebClientId": "YOUR_GOOGLE_WEB_CLIENT_ID.apps.googleusercontent.com"
    },
    "ios": {
      "usesAppleSignIn": true
    }
  }
}
```

Replace `YOUR_GOOGLE_WEB_CLIENT_ID` with the Web Client ID from the Google Cloud Console
(OAuth 2.0 credentials for the project). This value is read at runtime via `expo-constants`.
