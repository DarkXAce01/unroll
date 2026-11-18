# Unroll

Seamless Instagram carousels, free. Pick photos from a trip and Unroll lays them across slides that flow
into each other when you swipe: one picture running from slide to slide, no seams in the wrong place.
Every feature is free. No trial, no watermark, no account.

A Grease Pencil Studio app. See [PLAN.md](PLAN.md) for the product plan and roadmap.

## Status: Phase 0–1 done, Phase 2 under way (templates, stickers, frames, routes, motion, video)

**Make a carousel**
- Pick photos (or share them to Unroll from the gallery) and get a seamless layout straight away: photos in
  columns across the slides, stacked in pairs where they fit, the strip ending exactly on a slide edge
- Shuffle for another arrangement. When faces are known, Unroll tries several and keeps the one with the
  fewest faces on a cut
- Or one photo per slide, or one panorama across as many slides as its shape needs
- Grids for any single slide (2 across, 2 down, 1 + 2, 1 over 2, 3, 4, 6, framed)
- Instagram shapes: 4:5, 1:1, 3:4 (the new grid shape) and 9:16. Changing the shape keeps everything on its
  slide. Up to 20 slides; add, remove and reorder them

**Edit**
- Move, pinch to resize, twist to rotate. Handles to stretch, resize or rotate with one finger
- Snapping to cuts, slide centres, margins and other photos, with guide lines. Rotation snaps to straight
- Double-tap a photo to move and zoom it inside its frame (the rest shows faintly around it)
- Photo adjustments: 13 filters with strength, exposure, contrast, saturation, warmth, tint, fade, vignette
- Frames: rounded corners, border and colour, shadow, opacity, flip, replace the photo, fit to its shape
- Text: 10 fonts, size, colour, alignment, bold, a label behind it, letter spacing, line height, outline,
  shadow
- **1,500+ stickers in 19 packs**, all drawn in code and tinted by the theme: travel and marker doodles,
  die-cut stickers, title and subtitle styles, washi tape, stamps and postmarks, boarding passes and tickets,
  labels, paper clips, patches, bead and ransom-note words, foil badges, flowers, 60 flags, speech bubbles.
  Search by word; edit a sticker's words and colour, flip it; plus the plain shapes
- **95 frames in 7 shelves**: instant (Polaroid, Instax, square, wide in 8 colours), film strip, negative,
  slide mount, stamp, deckle, postcard, photo corners, notebook, taped, torn, phones, cameras, browser,
  music player, message, pop-up, circle, arch, oval, heart, blob, window, ticket, lace, scribble.
  Put one on a photo or add an empty framed slot
- 14 background textures: paper grain, grid, dots, lined, stripes, gingham, linen, kraft, checks, waves,
  confetti, stars, halftone
- Background: a colour, a gradient, or colours picked from your own photos
- Unlimited undo and redo. Projects save themselves

**Templates & themes**
- **227 templates** in 20 categories: 16 hand-made ones plus 22 style kits × 12 layouts. Browse one calm feed
  (featured carousels, then every template two by two, previewed with real photos), search by meaning ("friends", "bday", "ootd" or a typo all find the right looks, offline; gibberish finds nothing), save favourites, swipe through every slide before using one, remix it in any of 12
  colour themes and any of 4 formats. Each one stretches to any number of photos, or starts with
  empty slots you tap to fill. Title, dates and place come from the photos themselves
- Everything a template makes stays editable. 12 themes: switch one and every colour and font that follows
  the theme changes with it, or change a single colour or the fonts yourself

**Routes**
- A route through your photos in one tap, running across the slides so the swipe follows the trip
- Or a route on one slide, or draw freehand with your finger. Smooth or straight, solid/dashed/dotted, start
  and end markers, stops, and a plane, car, train, boat, walker or bike riding along it. Drag a point to bend it

**Motion & video**
- Every item can arrive: fade, rise, drop, pop, slide in, spin, draw itself (routes), type out (text), float.
  Or animate everything in one tap
- Play the video right in Preview
- Export a Reel (9:16 scrolling video) where the camera glides from slide to slide and the route draws
  itself ahead of it, or an animated carousel: one clip per moving slide, and each slide's route carries on
  from where the last slide's left off

**Seams, checked**
- Dashed lines show exactly where Instagram cuts, with slide numbers
- A face (Play build, found on the phone with ML Kit) or text sitting on a cut is outlined in red, with a
  one-tap fix that moves it the shortest way off the cut
- Photos too small for their frame are flagged before they look soft on Instagram

**Text**
- Fill any text with a photo: the picture shows through the letters (Text → Colour → Fill with a photo)

**Out**
- Preview: swipe it like a feed post, with the profile-grid tile
- Export page shows the post the way Instagram will (swipe it), then one tap sends it
- Sizes: 1× (exactly 1080 wide, what Instagram uses), 2× or 3× for printing and other apps, with the file
  size shown. JPEG quality 90/95/100, in order, in Pictures/Unroll. No watermark. Optionally one long image
- Post to Instagram straight into its carousel composer, TikTok (when installed), save, or share anywhere

**Support (optional, never a gate)**
- Play build: a tip jar and a "watch an ad" button after an export and in Settings. That's the only place an
  ad ever appears, and the ad SDK isn't even started until it's tapped. Ads are non-personalised
- F-Droid build (`-Pfoss`): no ads, no tips, no ML Kit and no internet permission

## Building

Kotlin + Jetpack Compose, Android 10+.

```
source scripts/env.sh
./gradlew assemblePlayDebug          # APK in app/build/outputs/apk/play/debug
./gradlew testPlayDebugUnitTest      # model, seams, layout, colour, undo, JSON tests
./gradlew -Pfoss assembleFossDebug   # the F-Droid flavour
```

Or open the folder in Android Studio and run the `playDebug` variant on your phone.

Before release, replace Google's test AdMob IDs in `app/build.gradle.kts` with real ones, and create the
tip products (`tip_coffee`, `tip_lunch`, `tip_dinner`, consumable) in the Play Console.

## Licence

GPL-3.0. Bundled fonts are under the SIL Open Font License.
