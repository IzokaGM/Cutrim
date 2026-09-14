# Cutrim

Cutrim is an Android-native video editor prototype built with Kotlin and Jetpack Media3. The long-term product goal is feature parity with modern mobile editors such as CapCut while keeping Cutrim's own branding, assets, and implementation.

## Current build: 0.8.0

### Working editor features
- Multi-video import through Android's document picker
- Create, autosave, reopen, and delete local projects
- Recent projects on the Home screen
- Add more media to an existing project
- Trim start / trim end
- Split, delete, reorder, and duplicate clips
- Undo / redo for clips, text, and audio state
- Duration-aware timeline cards that account for clip speed
- Constant speed presets from 0.5x to 2x
- Basic color filters and simple visual effects
- Per-clip emoji stickers
- Per-clip text overlays with size and color controls
- Music / audio import with volume, fade-in, and fade-out
- MP4 export through Media3 Transformer
- 720p / 1080p and 30 / 60 FPS export controls
- Export progress and MediaStore publishing

### Important limitations
- Transition choices are stored in the project model but are **not rendered into the exported video yet**.
- Preview is still based on Android `VideoView`, so it previews the selected clip rather than the entire composition accurately.
- Text/sticker transforms, keyframes, masks, blend modes, chroma key, curves, and multi-layer video are not implemented yet.
- Audio has no waveform timeline, start offset, beat markers, ducking, EQ, or voice recording yet.
- Photos, GIFs, templates, captions, tracking, background removal, and AI tools are not implemented yet.
- Export has no bitrate/codec selector, 4K workflow, HDR controls, or background render queue yet.

## Architecture direction

The current `MainActivity` is intentionally kept compatible with the existing prototype, but the next milestone should migrate playback from `VideoView` to Media3 `CompositionPlayer`, introduce a track-based timeline model, and move project storage/rendering out of the Activity.

For true transition parity, do not rely on the current transition name field alone. Media3 Composition still documents video/audio crossfading as unsupported, so Cutrim needs a dedicated frame compositor/render path for overlap transitions and advanced layer compositing.

See `CAPCUT_PARITY_ROADMAP.md` for the implementation order.

## Build

Open the project in Android Studio with a JDK compatible with Android Gradle Plugin 9.4.0. The project currently targets SDK 37 and uses Media3 1.11.0.

The automated sandbox used during this patch could not run a full Gradle build because the Gradle distribution could not be downloaded from `services.gradle.org` in that environment. Kotlin parser checks were run against `MainActivity.kt`; a full Android Studio/Gradle build on a networked development machine is still required.
