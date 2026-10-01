# TxOptimizations

Client-side Fabric mod for **Minecraft 26.3** (Java 25) that removes repeated per-frame work from the render thread without changing settings or what you see.

## Optimizations

### Rain fog

Every frame, `AtmosphericFogEnvironment.updateRainFogState` looks up the biome and the sky light at the camera position to fade in rain fog. A spark profile showed the sky light lookup alone taking about 2.8% of the render thread. TxOptimizations looks both up once per client tick and reuses them for the other frames of that tick, as long as the camera stays in the same block of the same level. The values only feed a slowly fading fog multiplier, so the result is visually identical.

### Particle light

Every frame, each visible particle looks up the light level at its position again (`Particle.getLightCoords`). Particles only move once per tick, so at high frame rates the same lookup repeats many times per tick. TxOptimizations caches the result per particle for the current client tick and position. A light change reaches particles at most one tick (50 ms) later. Particle types that compute their own light without calling the base method are unaffected.

### Vertex buffer recycling

At the end of every frame, `StagedVertexBuffer$GpuBufferPool.tryRecycleBuffers` checks every pending batch of vertex buffers to see whether the GPU is done with it. Each check is a native Vulkan call (`vkWaitSemaphores` with a zero timeout), and a spark profile showed these checks taking about 2.8% of the render thread. The batches are added in submission order and the GPU finishes submissions in that order, so TxOptimizations checks them from the oldest and stops at the first one that is still in use. Several pools (staging, vertex and index buffers for the world and the GUI) run this check every frame, and the oldest batch is usually still in use, so each pool would still make one native call per frame. While this recycle pass runs, TxOptimizations also remembers the lowest submit index the GPU reported as unfinished in the current frame and answers "not yet" for that index and every later one without asking the GPU again. It never answers "done" from memory, and vanilla's own fast path for already completed submits still runs first.

When the GPU does have to be asked, any zero-timeout completion check reads the submit semaphore's counter with `vkGetSemaphoreCounterValue` instead of calling `vkWaitSemaphores` with a zero timeout. The Vulkan spec defines both as the same question (has the counter reached this submit), so the answer is identical. The counter value is also stored as the encoder's completed submit index, so every older submit is answered by vanilla's fast path without any native call. Waits with a timeout are not touched. The result is the same set of recycled buffers with far fewer native calls. If a batch finishes in the middle of a frame or out of order, it is only reused one frame later.

### Block entity renderer lookup

Every frame, every block entity (chests, signs, heads, banners) asks `BlockEntityRenderDispatcher.getRenderer` for its renderer, mostly through EntityCulling's visibility check, which is a hash map lookup by block entity type. On a server lobby with many block entities this took about 1.7% of the render thread. TxOptimizations remembers the renderer on each block entity together with the renderer map it came from. A resource reload replaces that map, so the next lookup after a reload refreshes automatically, and a block entity's type never changes. The renderer returned is always the same one vanilla would return.

### Entity renderer lookup

Every frame, every entity asks `EntityRenderDispatcher.getRenderer` for its renderer, which runs a type switch to separate players and mannequins from other entities and then looks the renderer up by entity type in a hash map. TxOptimizations remembers the renderer on each entity together with the renderer map it came from, the same way it does for block entities. A resource reload replaces that map, so the next lookup after a reload refreshes automatically. Players and mannequins are never cached here because their renderer depends on the skin model.

### Sound listener updates

Every frame, `SoundEngine.updateSource` sends the camera's position and direction to the sound thread, and every send wakes that thread from sleep. At thousands of frames per second this took about 1% of the render thread. TxOptimizations skips the update when the camera has not moved or turned since the last one that was sent, and otherwise sends at most one update every 5 ms. Each frame compares against the last update actually sent, so the final position after you stop moving is always sent within 5 ms. OpenAL applies listener changes once per mixing block, which is normally longer than 5 ms, so the limit is not audible. When the sound system restarts (`loadLibrary`), the last sent update is forgotten so the listener is set again on the next frame.

### Left-to-right text

Every piece of text drawn on screen goes through `FormattedBidiReorder.reorder`, which runs ICU's bidirectional algorithm and Arabic shaping on it, even for plain English. When the language is left-to-right and the text has no right-to-left letters, Arabic numbers or explicit direction marks, the algorithm always produces a single left-to-right run covering the whole text, and shaping changes nothing. TxOptimizations detects that case with ICU's own character direction data and builds the same single run directly. Any text with Hebrew, Arabic or direction control characters, and every right-to-left language, still goes through vanilla.

### Camera fluid

`Camera.getFluidInCamera` checks the blocks at the camera and at the four near-plane corners for water, lava and powder snow, and it is called several times per frame. TxOptimizations computes it once per frame and reuses the answer while the camera's level, position and rotation are the same.

### Spare vertex buffers

At the end of every frame, the vertex buffer pools close every spare buffer that was not used in that frame, so a buffer that is needed again a frame later has to be created again. TxOptimizations keeps a spare buffer for up to 3 idle frames before closing it. Spares that get reused never reach that limit, and buffers the GPU is still using are never touched. Gnetum's HUD flushing still skips the cleanup the same way it does in vanilla.

### Translation templates

Every time a translated text is resolved, `TranslatableContents.decomposeTemplate` runs a regular expression over the translation string to find `%s`, `%1$s` and `%%`. TxOptimizations parses each distinct translation string once, remembers the literal parts and argument positions, and replays them in the same order with the same argument lookups. Invalid translation strings are left to vanilla so they fail and fall back exactly as before. Up to 4096 strings are kept, and the cache is cleared when it fills up.

### F3 entry checks

`DebugScreenEntryList.isCurrentlyEnabled` searches the list of enabled F3 entries one by one, and it is called for many entries every frame. TxOptimizations answers from a hash set built from that list, rebuilt whenever vanilla's own version counter for the list changes.

### Particle layers

Every frame, each particle quad is added to `QuadParticleRenderState`, which looks up the storage for the particle's layer in a hash map. The layer is a record, so every lookup hashes its fields again. Particles only ever use six constant layer objects and the map never loses an entry, so TxOptimizations remembers each layer object it has seen together with its storage and returns that storage directly. A layer it has not seen yet still goes through the map.

### Buffer builder elements

Every new `BufferBuilder` looks up up to eight vertex elements (position, color, UVs, normal, line width) by name in its vertex format, and the format's element map compares the names one by one. Vertex formats never change, so TxOptimizations resolves those eight elements once per vertex format with the vanilla lookup and reuses the result for every later `BufferBuilder` of that format, including the constructor's check that the format has a position element.

### Player skin model lookup

Every frame, `EntityRenderDispatcher.getAvatarRenderer` reads each player's skin through the player list and skin manager to choose the wide or slim player model, once for culling and once for drawing. TxOptimizations keeps the skin it read for that player during the current frame and reuses it for the second call. A skin that finishes loading is picked up on the next frame.

### Environment attribute values

Sky color, fog color and other environment attributes are read through `EnvironmentAttributeProbe$ValueProbe.get`, which blends the previous and current tick values for the frame's partial tick every time it is called, several times per frame. The blend only depends on those two values and the partial tick, so TxOptimizations remembers the last result and returns it while both values and the partial tick are unchanged.

### GPU crash checkpoints

When the GPU supports `VK_AMD_buffer_marker` or `VK_NV_device_diagnostic_checkpoints`, renderpearl writes a marker into the command buffer at every render pass so a driver crash report can name the last pass that ran. That took about 0.4% of the render thread. TxOptimizations makes renderpearl use its built-in no-op checkpoint storage, the same one it uses on GPUs without those extensions. Rendering is unchanged, but a GPU driver crash report no longer lists the last render passes.

### FPS counter

TxOptimizations draws its own FPS counter in the top left corner (`123 FPS`, white, no shadow, at 2,2), hidden while the F3 screen is open or the HUD is hidden. The number is the average frame rate over the last 0.5 seconds and updates every 0.5 seconds, the same as Sodium Extra's FPS overlay. It only counts frames, and the text is rebuilt only when the number changes, so drawing it skips the per-frame translation and text-direction work that Sodium Extra's overlay goes through.

Sodium Extra records every frame and sorts the last 5 seconds of frame times twice every 0.5 seconds for its 1% and 0.1% low values, even when its overlay is off, which took about 0.8% of the render thread at high frame rates. When Sodium Extra is installed, TxOptimizations skips that recording, so Sodium Extra's own FPS overlay (its Show FPS option, `show_fps` in `config/sodium-extra-options.json`) should be switched off.

### Optimized Block Entities (OBE)

Chests, ender chests, shulker boxes, signs, banners, beds, bells, skulls, decorated pots, lecterns, shelves, campfires, beacons, copper golem statues and cushions are baked into the chunk mesh like normal blocks instead of being rendered again every frame, and only switch back to per-frame rendering while they animate (for example while a chest opens). In a server lobby profile this cut block entity rendering from about 4.1% to 0.35% of the render thread.

This is a trimmed copy of [Optimized Block Entities](https://github.com/maDU59/OptimisedBlockEntities) 1.1.50 by maDU59_ (commit `f186eae`), kept in its original `fr.madu59.obe` packages. Removed from the original: the config screen, ModMenu and Sodium options pages, the public API, and the compatibility code for Iris, Lootr, Entity Model Features, BCLib and VulkanMod. OBE's settings still load from `config/obe.json` with OBE's defaults. The empty sign check reads the text filtering flag once per frame instead of once per sign. The OBE code is licensed by its author under LGPL-3.0-or-later, see `LICENSE-OBE`. It requires Fabric API and Sodium.

## Build

```
gradlew build
```

Produces `build/libs/txoptimizations-26.3-1.0.0.jar` and copies it to `dist/`.

## Install

Drop `dist/txoptimizations-26.3-1.0.0.jar` into your Minecraft `mods/` folder.

Requires:

- Fabric Loader `>= 0.19.5`
- Minecraft `26.3`
- Java `25`
