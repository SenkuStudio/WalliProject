# Walli - Cloudflare-first wallpaper app starter

Production-grade starter for a Kotlin + Compose wallpaper app with a Cloudflare-only backend.

## What is included

- Android app in Kotlin
- MVVM + clean-ish layering (`data / domain / ui`)
- Jetpack Compose + Material 3 UI
- Retrofit + OkHttp networking
- Coil image loading with memory/disk caching
- Room favorites + recents
- Hilt DI
- AdMob banner/interstitial/rewarded scaffolding
- Zedge-style full-screen swipe preview with `HorizontalPager`
- Cloudflare Worker API with D1 metadata + R2/CDN image delivery

## Project structure

```
Walli/
├── app/
│   ├── src/main/java/com/walli/wallpaper/
│   │   ├── ads/
│   │   ├── data/
│   │   │   ├── api/
│   │   │   ├── local/
│   │   │   └── repository/
│   │   ├── di/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   └── usecase/
│   │   ├── preview/
│   │   ├── ui/
│   │   │   ├── common/
│   │   │   ├── components/
│   │   │   ├── navigation/
│   │   │   ├── screens/
│   │   │   └── theme/
│   │   ├── util/
│   │   ├── wallpaper/
│   │   ├── MainActivity.kt
│   │   └── WalliApp.kt
│   └── src/main/res/
├── cloudflare-worker/
│   ├── src/index.ts
│   ├── schema.sql
│   ├── seed.sql
│   └── wrangler.jsonc
└── README.md
```

## Recommended backend architecture

Use Cloudflare like this:

1. **R2** stores original full-resolution wallpapers and compressed thumbnails.
2. **Custom domain on the R2 bucket** serves images over Cloudflare’s edge cache.
3. **D1** stores searchable metadata (`title`, `category`, `tags`, `downloads`, `premium`, `created_at`, `trending_score`).
4. **Workers** expose public REST endpoints for pagination, search, category filtering, trending, and download acknowledgements.

That setup keeps **all media delivery on Cloudflare** and avoids Firebase entirely.

## Worker API shape

- `GET /api/v1/wallpapers?page=1&limit=20&category=Nature&query=mountain&sort=latest`
- `GET /api/v1/categories`
- `GET /api/v1/wallpapers/day`
- `POST /api/v1/wallpapers/{id}/download`

Example response:

```json
{
  "items": [
    {
      "id": "1",
      "title": "Nature Mountain",
      "category": "Nature",
      "image_url": "https://cdn.example.com/full/nature/mountain.jpg",
      "thumbnail_url": "https://cdn.example.com/thumbs/nature/mountain.jpg",
      "downloads": 1200,
      "created_at": "2026-01-01",
      "premium": false
    }
  ],
  "page": 1,
  "limit": 20,
  "hasNext": true
}
```

## Android setup

Update these before you build:

- `app/build.gradle.kts`
  - `BASE_URL` -> your deployed Worker URL, for example `https://walli-api.your-subdomain.workers.dev/`
  - `API_KEY` -> optional public client key if you decide to validate read calls
- `app/src/main/res/values/strings.xml`
  - `admob_app_id` -> your real AdMob app ID
- `BuildConfig.ADMOB_*` values -> your real ad unit IDs

## Cloudflare setup, step by step

### 1) Create the R2 bucket

```bash
npx wrangler r2 bucket create walli-assets
```

Or create it in the dashboard.

Suggested folder layout inside the bucket:

```
full/nature/mountain.jpg
full/amoled/neon-city.jpg
thumbs/nature/mountain.jpg
thumbs/amoled/neon-city.jpg
```

### 2) Upload images

Dashboard upload works well for manual batches. For CLI uploads:

```bash
npx wrangler r2 object put walli-assets/full/nature/mountain.jpg --file ./assets/mountain.jpg --content-type image/jpeg --cache-control "public, max-age=31536000, immutable"
npx wrangler r2 object put walli-assets/thumbs/nature/mountain.jpg --file ./assets/mountain-thumb.jpg --content-type image/jpeg --cache-control "public, max-age=31536000, immutable"
```

### 3) Create D1

```bash
npx wrangler d1 create walli-db
```

Copy the returned `database_id` into `cloudflare-worker/wrangler.jsonc`.

### 4) Apply schema

```bash
cd cloudflare-worker
npm install
npx wrangler d1 execute walli-db --file=./schema.sql
npx wrangler d1 execute walli-db --file=./seed.sql
```

### 5) Bind D1 + R2 in the Worker

The sample `wrangler.jsonc` already shows the structure:

- `r2_buckets` binding -> `ASSETS`
- `d1_databases` binding -> `DB`
- `vars.CDN_BASE_URL` -> your public CDN hostname

### 6) Set up the public CDN domain for R2

In the Cloudflare dashboard:

1. Open **R2**.
2. Select your bucket.
3. Open **Settings**.
4. Add a **Custom Domain**.
5. Point it at a hostname inside the same Cloudflare zone, for example `cdn.example.com`.
6. Enable aggressive caching for static objects.

Put that hostname in `CDN_BASE_URL` in `wrangler.jsonc`.

### 7) Deploy the Worker

```bash
npm run deploy
```

### 8) Point the app to the Worker

In `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "BASE_URL", ""https://walli-api.your-subdomain.workers.dev/"")
```

Sync Gradle and run the app.

## Android feature notes

### Home feed

- Uses thumbnail URLs in a `LazyVerticalGrid`
- Pull-to-refresh
- Infinite scroll
- Search + categories + latest/popular/trending sorting
- Recently viewed row from Room
- Hero card for Wallpaper of the Day styling

### Preview

- `HorizontalPager`
- Keeps the selected grid index
- Adjacent-page prefetching via Coil
- Tap image to hide/show chrome
- Download, set wallpaper, favorite, and share actions

### Favorites / recents

- Favorites are persisted in Room
- Recent views are persisted in Room
- Viewed full-size images are also cached on disk by Coil


## Screen status

- **Home**: fully wired
- **Preview**: fully wired
- **Favorites**: fully wired
- **Categories**: wired to live API categories
- **Search**: intentionally merged into the Home feed query bar for a faster UX
- **Onboarding**: starter placeholder included for DataStore-driven first-run flows

## AdMob integration

This starter includes:

- adaptive banner component (`BannerAd.kt`)
- interstitial gate manager (`AdMobManager.kt`)
- rewarded flow for premium wallpapers

Replace the sample/test IDs before release.

## Build APK

Android Studio:

1. Open the `Walli` folder.
2. Let Gradle sync.
3. Replace the placeholder Worker URL and AdMob IDs.
4. Build -> **Build Bundle(s) / APK(s)** -> **Build APK(s)**.

Command line once Gradle wrapper is added in your local environment:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## Release checklist

- swap test AdMob IDs for production IDs
- add real app icon / brand assets
- set real `BASE_URL`
- upload optimized full + thumb assets to R2
- seed D1 with your production metadata
- wire premium entitlement logic if rewarded unlocks should persist
- add privacy policy + Play Console data disclosure
- tighten Worker rate limiting / bot protection if traffic grows
- add Crash reporting/analytics if desired (non-Firebase option recommended if you want to stay Cloudflare-only on backend media delivery)

## Notes

- This is a strong starter, not a fully Play-ready commercial app out of the box.
- The main architectural pieces are already separated so you can scale to Paging 3, subscription billing, admin ingestion tooling, and WorkManager auto-wallpaper jobs later.
