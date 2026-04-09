# Next steps for a Play-ready launch

## Advanced features to wire next

### 1) Auto wallpaper changer
- Use `WorkManager` with a periodic worker.
- Store schedule preferences in `DataStore`.
- Reuse `WallpaperApplier` inside the worker.
- Recommended file locations:
  - `app/src/main/java/com/walli/wallpaper/worker/AutoWallpaperWorker.kt`
  - `app/src/main/java/com/walli/wallpaper/data/datastore/SettingsDataStore.kt`

### 2) Blur + set wallpaper
- Add a bitmap transform pipeline before `WallpaperManager.setBitmap`.
- Keep the original image cached and render a blurred copy off the main thread.
- Suggested additions:
  - `wallpaper/WallpaperBitmapProcessor.kt`
  - blur radius selector in `ui/screens/preview/PreviewScreen.kt`

### 3) Premium unlock persistence
- Current sample uses rewarded ads as a gate.
- If you want a session or account-based unlock, add a local entitlement store and optionally a signed Worker endpoint.

### 4) Onboarding persistence
- Hook `ui/screens/onboarding/OnboardingScreen.kt` to `DataStore`.
- Route first-launch users to onboarding before `Home`.

### 5) Analytics / crash reporting
- This starter intentionally avoids Firebase.
- If you want observability, add a non-Firebase provider or keep analytics server-side through your Worker.

### 6) Paging 3
- Current feed uses manual pagination for clarity.
- For very large catalogs, a `RemoteMediator` + Paging 3 can replace the manual page loader while keeping the current repository contract.

### 7) Admin ingestion pipeline
- Add a simple admin tool or script that:
  - generates thumbnails
  - uploads originals + thumbnails to R2
  - inserts metadata into D1
  - marks premium / featured / category / tags

## Suggested release hardening
- Add screenshot tests for the feed and preview UI.
- Add repository tests with mocked Retrofit responses.
- Add Worker API auth/rate limits if the API will be public.
- Add stricter Worker input validation and abuse throttling.
- Add a privacy policy and Play data safety disclosure.
