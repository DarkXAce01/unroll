# Carousel app: Product & Build Plan

Working name: **Scrll (placeholder)**. Pick a real name before anything public (see §9): "SCRL" is
Appostrophe AB's app and trademark, so we can't ship under it or anything that sounds the same.
Started 2026-11-18. Android first, Kotlin + Jetpack Compose. A Grease Pencil Studio app.

**Free forever. Every feature, no trial, no watermark, no account.** Support is optional: a tip jar
(like Pixnix) and an ad you only ever see if you tap "watch an ad to support". Never a mandatory ad.

---

## 1. The vision

> Pick the photos from your trip. The app lays them out across one long canvas that you swipe through
> on Instagram: one picture flowing from slide to slide, no seams in the wrong place, no faces cut in
> half. Add a title, a little map, dates. Post it in one tap, at full quality.

### Why build it
- **SCRL works, but it's paywalled.** 7-day trial, then $4.99–$79.99 plans. Free saves get a watermark,
  templates are locked. The thing people want (seamless carousel) is simple enough to give away.
- **Free is affordable.** Everything renders on the phone. No server, so each user costs nothing.
- **The hard part is known:** export quality and seams. Phase 0 proves those before anything else.

### Principles
1. **Every feature is free.** Support never unlocks anything. A supporter and a non-supporter get the
   same app.
2. **Ads only when asked.** No banners, no interstitials, no "watch to export". One quiet "support with
   an ad" row; tap it and a rewarded ad plays. That's the only way an ad ever appears.
3. **Full quality out.** Instagram's exact pixel sizes, no extra compression, no watermark.
4. **Seams are the product.** What you see on the canvas is exactly where Instagram will cut it, and the
   app warns before a face or text sits on a cut.
5. **Originals are untouched.** Photos are referenced, never modified; every edit is a recipe on top.

---

## 2. Features

Tags: **[MVP]** first usable build · **[v1]** public launch · **[v2]** after launch · **[Moon]** later.
"=" means SCRL has it, "+" means SCRL doesn't (our "much better").

### A. Canvas: the core

| Feature | Tag | | Notes |
|---|---|---|---|
| **Seamless panorama canvas** | MVP | = | One wide canvas made of N slides side by side. Photos placed anywhere, spanning seams. |
| **Slide count 1–20** | MVP | = | Instagram allows 20 per carousel. Add/remove slides at either end or in the middle. |
| **Aspect ratios** | MVP | = | 4:5 (1080×1350), 1:1 (1080×1080), 3:4 (1080×1440, the new grid shape), 9:16 for stories/reels. |
| **Live seam guides** | MVP | = | Dotted cut lines, toggleable, with slide numbers. |
| **Free placement** | MVP | = | Drag, pinch-scale, rotate, layer order, snap to seams/centre/edges/other photos. |
| **Crop inside a frame** | MVP | = | Double-tap a photo to pan/zoom it inside its frame. |
| **Undo / redo** | MVP | = | Unlimited, survives app restart. |
| **Seam safety** | MVP | + | On-device face + text detection (ML Kit). Warns "a face sits on cut 3", offers a one-tap nudge. |
| **Instagram preview** | MVP | + | Swipe the carousel as it will look in the feed, with the real crop and the profile-grid thumbnail (first slide in 3:4). |
| **Backgrounds** | MVP | = | Solid colour, gradient, colour picked from a photo, blurred photo, paper textures. |
| **Borders & spacing** | MVP | = | Frame thickness, corner radius, gap between photos, drop shadow. |
| **Multi-select & group** | v1 | = | Move/align several items together. |
| **Zoomable canvas** | MVP | = | Zoom out to see the whole panorama, in to place details. |

### B. Layouts & templates

| Feature | Tag | | Notes |
|---|---|---|---|
| **Grid layouts per slide** | MVP | = | 1/2/3/4/6-up, with photos that can also break out across seams. |
| **Panorama split** | MVP | = | One wide photo (a real panorama) sliced across N slides, auto-picking N from its width. |
| **Template library** | v1 | = | 40+ in-house templates at launch (collage, carousel, story). All free. Stored as JSON so adding more is cheap. |
| **Auto-layout** | MVP | + | Pick 12 photos, get a full carousel arranged by their shapes, faces kept clear of seams. Shuffle for another arrangement. |
| **Trip mode** | v1 | + | Pick a date range (or an album); the app pulls the photos, groups them by day/place, picks the best ones (sharp, not duplicates), and builds a carousel: title slide, day-by-day, map slide. Built for exactly your mom's use case. |
| **Map slide** | v1 | + | Route of the trip drawn from photo GPS, stylised (no Google Maps API needed: offline vector map tiles or a simple drawn path). |
| **Save as my template** | v1 | + | Turn any project into a reusable template. |
| **Multi-post grid** | v2 | = | Plan a 3/6/9-post grid for the profile (one image split across posts). |

### C. Content on the canvas

| Feature | Tag | | Notes |
|---|---|---|---|
| **Text** | MVP | = | Fonts (bundled open-licence, ~30), size, colour, alignment, line/letter spacing, outline, shadow, background pill. |
| **Stickers & shapes** | v1 | = | Lines, arrows, circles, tape, stamps, travel set (planes, pins, tickets). All bundled, all free. |
| **Photo editor** | MVP | = | Exposure, contrast, saturation, warmth, highlights/shadows, sharpness, vignette, + 12 filters. Non-destructive. |
| **Cut-out subject** | v1 | + | Remove background from a photo (ML Kit subject segmentation, on-device) so people can pop across seams. |
| **Video & Live photos** | v1 | = | Video layers on the canvas, trim, mute. |
| **Dates & place labels** | v1 | + | Auto text from EXIF: "Day 2 · Udaipur · 14 Nov". |
| **Masks & frames** | v2 | = | Photos in circles, arches, polaroid frames, torn paper. |
| **Overlays** | v2 | = | Grain, light leaks, paper textures. |

### D. Export & posting

| Feature | Tag | | Notes |
|---|---|---|---|
| **Export slides** | MVP | = | N images at exact Instagram sizes, high-quality JPEG (or PNG), into a gallery folder named after the project. No watermark. |
| **Post to Instagram** | MVP | = | Android share intent (ACTION_SEND_MULTIPLE) straight into Instagram's carousel composer, slides in order. Also TikTok photo mode, WhatsApp, anything. No Instagram API or login needed. |
| **Panorama scroll video** | v1 | = | MP4 that pans across the whole canvas (Reels/Stories/TikTok), speed + easing + pause per slide, optional music. Media3 Transformer. |
| **Full panorama image** | MVP | + | The whole canvas as one long image. |
| **Export a range** | v1 | + | Re-export just slide 4 after a fix. |
| **Quality check** | MVP | + | Warns when a photo is upscaled too far and will look soft at 1080. |

### E. Projects

| Feature | Tag | | Notes |
|---|---|---|---|
| **Autosave projects** | MVP | = | Room DB + project JSON. Open, duplicate, rename, delete. |
| **Drafts survive** | MVP | + | Photos referenced by MediaStore URI; if one is deleted, the slot shows "missing" instead of breaking. |
| **Share a project file** | v2 | + | One file (layout + photos) so someone else can finish it. |

### F. Support (optional, never a gate)

| Feature | Tag | Notes |
|---|---|---|
| **Tip jar** | v1 | Same as Pixnix: consumable Play in-app products (coffee / lunch / dinner), reuse `TipJar` + flavor pattern. |
| **Support with an ad** | v1 | AdMob **rewarded** ad, shown only when the user taps it. One quiet row in Settings and a small line on the "exported" screen ("Enjoying it? Support with an ad"). Nothing else ever triggers an ad. Reward = a thank-you note + a small "supporter" heart on the about screen (AdMob requires a stated reward; it unlocks no feature). |
| **Ad SDK only in the `play` flavor** | v1 | The `foss` build (F-Droid / GitHub) has no ads, no billing, **no INTERNET permission**, like your other apps. The `play` build needs INTERNET only for the ad. Data Safety form and an in-app line say so honestly. |
| **No personalised ads by default** | v1 | Request non-personalised ads; UMP consent form for EU/UK. Ads fully off for kids/unknown-age content settings. |

---

## 3. Navigation & key flows

**Home:** project list + big "New carousel". New → pick photos → choose: *Auto-layout*, *Trip*,
*Template*, or *Blank* → canvas.

**Canvas screen:** canvas in the middle, slide strip below, bottom toolbar: Add (photo / text /
sticker), Layout, Background, Ratio, Preview, Export.

**First run, under 60 seconds to the wow:** pick photos → auto-layout fills a carousel → swipe the
Instagram preview → "Post". That's the hook.

---

## 4. Tech

- Kotlin + Jetpack Compose, `minSdk 29`, same toolchain/versions as Attic (`gradle/libs.versions.toml`).
- **Canvas model:** plain Kotlin data (items with position/scale/rotation in canvas units), so layout,
  seam maths and auto-layout are unit-tested without Android.
- **Rendering:** one renderer used for both screen and export (Android `Canvas`/`Bitmap` at export
  resolution, slice by slide). What you see = what you export.
- **Memory:** 20 slides × 1080×1440 is ~31 Mpx. Render slide by slide with sampled sources so
  low-end phones don't run out of memory.
- **On-device ML:** ML Kit face detection, text recognition, subject segmentation (Play services in
  `play`; `foss` falls back to no seam-safety for text / no cut-out, or bundled OpenCV YuNet for faces
  like Attic).
- **Video export:** AndroidX Media3 Transformer.
- **Storage:** Room for projects, photos by MediaStore URI, exports to `Pictures/<App>/<Project>`.
- **Flavors:** `play` (billing + rewarded ads) and `foss` (neither, no INTERNET), copied from Pixnix.
- **Tests:** JUnit for the model; Roborazzi screenshots for screens (same as Attic).

---

## 5. Build phases (features first, UI last)

| Phase | What ships | Done when |
|---|---|---|
| **0. Prove it** | Project skeleton, canvas model, place photos across N slides, export slices, share to Instagram. | A 5-slide trip carousel posted from your phone looks seamless on Instagram at full quality. |
| **1. Core editor (MVP)** | All MVP rows in A, B, C, D, E: free placement, grids, panorama split, text, photo editor, backgrounds, undo, seam safety, Instagram preview, auto-layout, projects. | Mom can build and post her whole trip without help. |
| **2. Launch set (v1)** | Trip mode, map slide, templates, stickers, cut-out, video layers, scroll video export, tip jar, opt-in rewarded ad, foss/play flavors. | Feature parity with SCRL plus the "+" rows. |
| **2a. Templates & motion** (in progress) | Themes with colour/font roles, 16 stretchy templates with empty slots, photo mounts, routes (auto through photos, per slide, freehand) with vehicles, per-item motion, live player, Reel + animated-carousel MP4 export. | Done in code. |
| **2b. Content library** | 1,500+ code-drawn stickers in 19 packs, 95 frames, 14 textures, 227 templates (kits × layouts) with a browsable gallery: shelves, search, favourites, slide-by-slide preview, colour and format remix. | Done, 98 tests. Previews use 68 freely licensed real photos (credited in Settings) and category-fitting words; one vertical feed. Next: text mask, photo-in-photo, more fonts, 2x export, TikTok share. |
| **3. UI pass** | Grease Pencil Studio look: signature kit from `../album/docs/grease-pencil/`, Pencil red accent, Attic's taste rules (true neutrals, one accent, sentence case, no gradients/glass in chrome). Roborazzi critique loop. | Screens pass the taste rules; looks like a sibling of Attic and Pixnix. |
| **4. Ship** | Store listing, privacy policy, Data Safety, F-Droid metadata, README. | Live on Play (and F-Droid for foss). |
| **v2+** | Multi-post grid, masks, overlays, project sharing, more templates. | |

---

## 6. Risks

- **Instagram crops/compresses.** Exact sizes and sRGB JPEG at q≈95 keep it clean; verify on a real
  post in Phase 0.
- **Instagram share intent quirks.** Multi-image share opens the carousel composer on current
  versions; fallback is "saved to gallery, open Instagram".
- **Ad policy.** AdMob rewarded ads must state a reward and be user-initiated, which fits. Families
  policy: don't target kids. Keep ads entirely out of `foss`.
- **Scope.** Ship Phase 1 as a complete app before touching Phase 2.

## 7. Not doing

- No subscriptions, no trial, no locked templates, no watermark, ever.
- No account or cloud. No Instagram login / scheduling (would need a server and Meta app review).
- No interstitial, banner or "watch to unlock" ads.

## 8. Platforms

Android first (that's where the GreasePencil stack is). iOS only if the Android app takes off.

## 9. Open decisions

- **Name.** Must not be SCRL-like. Ideas: *Panlo*, *Swipeline*, *Seamly*, *Trailpost*, *Unroll*.
- **Repo** public under DarkXAce01 like the others? Licence GPL-3.0 like Pixnix?

## 10. Research notes (2026-11-18): what the creative apps offer

- **Unfold, Mojo**: hundreds of animated story/carousel templates, almost all behind a subscription. Text
  and photos animate in; templates restyle with brand colours. Unroll copies the idea (themes + motion),
  not their designs, and keeps it free.
- **Route/journey posts** (e.g. Jitter's "The Route" template): a line drawing itself from place to place.
  In a carousel the trick is one continuous line across the slides, so each swipe continues it.
- **Seamless carousels** (SCRL, guides like Krumzi's): the panorama + elements crossing the cut is the core.
- **Scrapbook look** is what travel posts use most: polaroids, tape, stamps, film strips, handwriting.
