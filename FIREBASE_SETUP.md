# Eraka – Firebase: LIVE or EMULATOR

The **debug** app can talk to either:

| Mode | What it is | Needs |
|---|---|---|
| **Live** | The online project `otta-project-1ee57` | Internet on the phone. Nothing on the laptop. |
| **Emulator** | Fake Firebase running on this laptop | Same Wi-Fi as the laptop, laptop awake, emulators running |

The **release** APK for participants is always **Live**.

## How to choose

- **On the phone (debug builds):** long-press the **logo on the sign-in screen**, or the **top of the side menu** when already signed in.
  - Pick Live or Emulator and type the laptop's IP.
  - Tap **Save & restart**. This signs you out, because the two servers have separate accounts.
  - On every start, the sign-in screen briefly shows `DEV: LIVE Firebase` or `DEV: EMULATOR <ip>`.
- **Default when the app is first installed:** set in `gradle.properties`:
  ```
  erakaFirebaseMode=live        # or emulator
  erakaEmulatorHost=            # e.g. 192.168.1.23 (only for emulator)
  ```
  A choice made on the phone overrides this.

## Using the emulators
1. Double-click **`firebase-emulators-start.bat`**. It prints this laptop's IPv4 and starts the emulators.
2. The first time, copy your old export folder `firebase_data_new_user` into this project folder and rename it **`firebase_emulator_data`**, so the starter pictures are there. The data is saved back into it when you stop the emulators with Ctrl+C.
3. On the phone, choose **Emulator** and type the IPv4 shown.
4. The emulator UI is at http://localhost:4000.
5. If the IP changes (new Wi-Fi), just type the new one on the phone. No rebuild is needed.

## Using live Firebase (one-time setup)
1. **Plan and cost.** In the Firebase console, go to Usage and billing. Cloud Storage needs the Blaze plan. Then in Google Cloud → Billing → Budgets & alerts, create a budget (e.g. $1) with email alerts. The alert emails you; it doesn't stop charges.
2. **Rules.** The first time, run `firebase login`, then double-click **`firebase-deploy-rules.bat`**. This uploads `storage.rules` and `database.rules.json`: each user can only reach their own data, and the starter files are read-only.
3. **Starter files.** In the Storage tab, check for these, and upload them from the export's `storage_export` if they're missing:
   - `Archivos_Paises/pictos/pictos_en.txt`
   - `Archivos_Paises/grupos/grupos_en.txt`
   - `Archivos_Sugerencias/pictos_*.txt`
4. **Google sign-in.**
   - Run `gradlew signingReport` and copy the debug **SHA1**.
   - Go to Firebase → Project settings → app `com.stonefacesoft.ottaa.debug` → Add fingerprint.
   - Download the new `google-services.json` into `app/`.

The emulators also use the same rule files, so what works on the emulator will work live.
