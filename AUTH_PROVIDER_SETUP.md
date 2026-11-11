# Topline sign-in provider setup

Provider credentials must be created under accounts you control. Never paste passwords, API keys, OAuth client secrets, or signing keys into chat or source code.

## Google sign-in

1. Open [Google Cloud Console credentials](https://console.cloud.google.com/apis/credentials) and create/select a project.
2. Configure the OAuth consent screen / Google Auth Platform, including the app name, support email, and authorized test users while the app is in testing.
3. Create an **Android** OAuth client for package `com.kisimoni.topline` and add the SHA-1 fingerprint for the signing certificate used by your build.
4. Create a **Web application** OAuth client for the server audience used by Google Credential Manager.
5. Add the Web client ID as `TOPLINE_GOOGLE_WEB_CLIENT_ID` in your local Gradle properties. Add the Android and Web client IDs, comma-separated, to `GOOGLE_ALLOWED_CLIENT_IDS` in the server environment.
6. For the local Android signing fingerprint, run `.\gradlew.bat signingReport` from `ToplineAndroid`. For Play App Signing, use the app-signing certificate fingerprint shown in Play Console. The client IDs are public identifiers, not secrets.

## Facebook sign-in

1. Create an app at [Meta for Developers](https://developers.facebook.com/apps/), add the Facebook Login product, and configure its Android platform.
2. For Android, enter the package name and the signing key hashes required by Meta. Add the app to test mode and assign your test accounts as app roles until review is complete.
3. Set the public App ID as `TOPLINE_FACEBOOK_APP_ID` in local Gradle properties.
4. Set `FACEBOOK_APP_ID` and the **App Secret** as server environment variables. The App Secret must only exist on the server.
5. Request the email permission and test with an account that has an email address. Meta may require app review before non-role users can sign in.

## Confirmation and recovery email (Gmail SMTP)

1. Choose a Gmail or Google Workspace mailbox that you control and enable [2-Step Verification](https://myaccount.google.com/signinoptions/two-step-verification).
2. Open [Google Account App Passwords](https://myaccount.google.com/apppasswords), create an app password (for example, name it `Topline backend`), and copy it when Google shows it. If Google does not offer App Passwords for this account, use an SMTP provider that supports app-specific credentials instead.
3. In the backend hosting environment, set `SMTP_EMAIL` to that mailbox and `SMTP_APP_PASSWORD` to its app password. Do not use the normal Google account password, put the app password in the Android app, commit it, or send it in chat.
4. The backend sends first-time Google confirmation codes and password-reset codes from this mailbox to the email address requested. Google confirmation codes expire after ten minutes, allow five attempts, and can be resent once per minute.

## Server environment

Add these values to the existing server environment (do not replace its MongoDB or JWT configuration):

```env
AUTH_CHALLENGE_SECRET=<a unique random secret of at least 32 bytes>
SMTP_EMAIL=
SMTP_APP_PASSWORD=
GOOGLE_ALLOWED_CLIENT_IDS=
FACEBOOK_APP_ID=
FACEBOOK_APP_SECRET=
FACEBOOK_GRAPH_VERSION=v23.0
```

Generate `AUTH_CHALLENGE_SECRET` locally with PowerShell:

```powershell
node -e "console.log(require('node:crypto').randomBytes(32).toString('base64'))"
```

Set server variables in the hosting provider's secret/environment-variable settings for production. Do not commit `.env`. Deploy the backend before expecting these endpoints to work on the hosted API. For Render, add `SMTP_EMAIL` and `SMTP_APP_PASSWORD` under the backend service's **Environment** settings, then deploy. Rebuild the Android app after changing its local Gradle client IDs.

## QR sign-in

On the device showing the login QR, Topline creates a random, two-minute session. Scan it using **Settings → Scan login QR code** on a device already signed in, then approve only if you initiated the login. The approval device must be signed in to the same API environment. The QR token is single-use and expires quickly.

## Current supported API flows

- Phone-number OTP sign-in is not currently supported.
- `POST /api/auth/password/reset/request` and `POST /api/auth/password/reset/confirm`
- `POST /api/auth/oauth/google` sends a confirmation code for a first-time Google account; returning Google accounts sign in directly.
- `POST /api/auth/oauth/google/verify` confirms the first-time Google account using the emailed code.
- `POST /api/auth/oauth/facebook`
- `POST /api/auth/qr/start`, `/qr/poll`, `/qr/approve`, and `/qr/deny`

First-time Google sign-in creates a Topline account with a generated username after the emailed confirmation code is verified. Returning Google accounts sign in directly. Facebook sign-in creates an account on first use. If a provider email already belongs to another account, sign in with that account first; automatic linking is intentionally blocked.
