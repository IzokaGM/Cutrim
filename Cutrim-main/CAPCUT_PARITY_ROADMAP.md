# Cutrim → CapCut-class Feature Roadmap

The objective is functional parity, not copying CapCut trademarks, proprietary assets, templates, fonts, or copyrighted content.

## Phase 0 — Foundation (0.8.0, implemented in this patch)

- [x] Persistent project IDs and local JSON project files
- [x] Autosave and Recent Projects
- [x] Reopen/delete saved projects
- [x] Add media after project creation
- [x] Duplicate clips
- [x] Undo/redo captures clip + text + audio state
- [x] Stable clip IDs
- [x] Text overlays scoped to a clip instead of every clip
- [x] Timeline card width reflects effective clip duration/speed
- [x] Remove the obsolete duplicate `EditorActivity`

## Phase 1 — Real timeline playback

Priority: highest.

- Replace `VideoView` with Media3 `CompositionPlayer`
- Use one composition as the source of truth for preview and export
- Project-wide playhead instead of clip-local playback only
- Accurate scrubbing across clip boundaries
- Continuous playback across the full timeline
- Thumbnail strip generation and cache
- Audio waveform extraction and cache
- Zoomable timeline with a fixed center playhead
- Snap-to-edge and magnetic timeline behavior

Recommended model:

- `Project`
- `Track` (`VIDEO`, `AUDIO`, `TEXT`, `STICKER`, `OVERLAY`)
- `TimelineItem(id, trackId, sourceUri, startUs, durationUs, sourceInUs, sourceOutUs, zIndex)`
- `Transform(positionX, positionY, scaleX, scaleY, rotation, opacity)`
- `Keyframe(property, timeUs, value, easing)`

## Phase 2 — Core editing parity

- Multi-track video overlays / picture-in-picture
- Photos with configurable duration
- Crop / rotate / mirror
- Canvas ratio and background controls
- Position / scale / rotation gestures
- Keyframes for transform and opacity
- Freeze frame
- Reverse
- Speed curves / ramps
- Clip opacity
- Copy/paste attributes
- Replace media while preserving edits
- Compound clips / grouping

## Phase 3 — Text, stickers, captions

- Text as real timeline items with in/out times
- Drag / resize / rotate text directly on canvas
- Fonts, weight, alignment, spacing, stroke, shadow, background
- Text animation in / out / loop
- Sticker asset browser
- Animated stickers
- Auto captions
- Caption word timing editor
- Caption style presets

## Phase 4 — Audio parity

- Audio tracks with timeline offsets and trims
- Waveforms
- Voice recording
- Extract audio from video
- Beat detection and beat markers
- Fade handles on the timeline
- Noise reduction
- Voice enhancement
- Pitch and voice effects
- Ducking
- EQ / basic mixer

## Phase 5 — Effects and transitions

Media3 can concatenate and mix assets, but its Composition documentation currently lists crossfading audio/video as unsupported. Treat transitions as a renderer feature, not a UI-only property.

Build a compositor layer that can render overlapping clips frame-by-frame:

- Dissolve / crossfade
- Slide / push / wipe
- Zoom / camera transitions
- Blur / flash transitions
- Mask-based transitions
- Motion blur
- Blend modes
- Masks and feathering
- Chroma key
- LUTs
- Adjustment layers
- Curves / HSL / color wheels
- Sharpen / denoise / vignette / grain

For advanced parity, plan for a custom OpenGL/Vulkan compositor and a native media pipeline rather than expecting Media3 alone to cover every CapCut-style effect.

## Phase 6 — Export pipeline

- 480p / 720p / 1080p / 1440p / 4K options
- 24 / 25 / 30 / 50 / 60 FPS
- Bitrate presets and custom bitrate
- H.264 / H.265 where supported
- HDR/SDR policy
- Audio bitrate/sample-rate controls
- Background export service + notification
- Export cancellation and resume strategy
- Device capability validation before export
- Share sheet after export

## Phase 7 — Templates and AI

- Template project format
- Beat-synced templates
- Speech-to-text captions
- Text-to-speech
- Background removal / subject segmentation
- Object tracking
- Face tracking
- Auto reframe
- Silence removal
- Highlight detection
- Script-to-video / asset suggestion workflows

These features should be treated as separate ML/cloud subsystems and added only after the deterministic editor/render core is reliable.

## Engineering cleanup required before scaling

1. Move models out of `MainActivity.kt`.
2. Create `ProjectRepository` for serialization/autosave.
3. Create `EditorViewModel` or an equivalent state holder.
4. Create a renderer/composition builder shared by preview and export.
5. Move thumbnail/waveform work off the UI thread.
6. Add instrumentation tests using short deterministic media fixtures.
7. Add project schema migrations before changing persisted models again.
