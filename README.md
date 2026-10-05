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

Every frame, every block entity (chests, signs, heads, banners) asks `BlockEntityRenderDispatcher.getRenderer` for its renderer, which is a hash map lookup by block entity type. On a server lobby with many block entities this took about 1.7% of the render thread. TxOptimizations remembers the renderer on each block entity together with the renderer map it came from. A resource reload replaces that map, so the next lookup after a reload refreshes automatically, and a block entity's type never changes. The renderer returned is always the same one vanilla would return.

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

`DebugScreenEntryList.isCurrentlyEnabled` searches the list of enabled F3 entries one by one, and it is called for many entries every frame. TxOptimizations answers from a hash set built from that list, rebuilt whenever vanilla's own version counter for the list changes. `getCurrentlyEnabled` also made a fresh immutable copy of the list on every call, several times per frame; the copy is now kept and handed out again until that same version counter changes.

### Particle layers

Every frame, each particle quad is added to `QuadParticleRenderState`, which looks up the storage for the particle's layer in a hash map. The layer is a record, so every lookup hashes its fields again. Particles only ever use six constant layer objects and the map never loses an entry, so TxOptimizations remembers each layer object it has seen together with its storage and returns that storage directly. A layer it has not seen yet still goes through the map.

### Buffer builder elements

Every new `BufferBuilder` looks up up to eight vertex elements (position, color, UVs, normal, line width) by name in its vertex format, and the format's element map compares the names one by one. Vertex formats never change, so TxOptimizations resolves those eight elements once per vertex format with the vanilla lookup and reuses the result for every later `BufferBuilder` of that format, including the constructor's check that the format has a position element.

### Player skin model lookup

Every frame, `EntityRenderDispatcher.getAvatarRenderer` reads each player's skin through the player list and skin manager to choose the wide or slim player model, once for culling and once for drawing. TxOptimizations keeps the skin it read for that player during the current frame and reuses it for the second call. A skin that finishes loading is picked up on the next frame.

### Translucent sorting

Translucent geometry such as entity translucency, text, see-through name tags and glints is sorted back to front every frame. Vanilla reads the center of every quad with six bounds-checked `ByteBuffer.getFloat` calls and writes the six sorted indices per quad through an `IntConsumer` lambda. TxOptimizations reads the same floats straight from native memory when the buffer is direct and in native byte order (one bounds check for the whole range up front), computes the centers with the same arithmetic, and writes the same index values straight to the buffer's memory when it is direct and in native byte order, with one bounds check for the whole range, and through a short or int view of the buffer otherwise. Sodium's sort of up to 80 quads ran fastutil's indirect quicksort with a comparator per comparison and then a pass that orders equal keys by index; it now packs each key and index into one long and sorts those, which gives exactly the same order (by key, then by index). Unstable sorts are left as they were. The bytes written and the final buffer position are identical. Sodium's closest-point entity sort option, when enabled, replaces the center decoding with its own and is unaffected.

### Glow check spectator state

Sodium asks `Minecraft.shouldEntityAppearGlowing` for every entity every frame, and for every entity that is not glowing vanilla checks whether the local player is a spectator. TxOptimizations computes that once per frame and reuses it for the other entities.

### Environment attribute values

Sky color, fog color and other environment attributes are read through `EnvironmentAttributeProbe$ValueProbe.get`, which blends the previous and current tick values for the frame's partial tick every time it is called, several times per frame. The blend only depends on those two values and the partial tick, so TxOptimizations remembers the last result and returns it while both values and the partial tick are unchanged.

### GPU crash checkpoints

When the GPU supports `VK_AMD_buffer_marker` or `VK_NV_device_diagnostic_checkpoints`, renderpearl writes a marker into the command buffer at every render pass so a driver crash report can name the last pass that ran. That took about 0.4% of the render thread. TxOptimizations makes renderpearl use its built-in no-op checkpoint storage, the same one it uses on GPUs without those extensions. Rendering is unchanged, but a GPU driver crash report no longer lists the last render passes.

### Particle rotation

Every camera-facing particle allocated a new rotation quaternion each frame. The quaternion is now reused, reset to identity first as the new one was.

### Main target clear

Every frame cleared the main color and depth target before rendering, and the world render cleared the same target again with the fog color before drawing anything. When a world is rendered, the first clear is skipped. Screens without a world still clear as before.

### Particle corners

For every particle, Sodium rotated the four corners of the quad by the particle rotation, four quaternion rotations per particle per frame. Camera-facing particles all carry the camera rotation, so the rotated corners are the same for each of them. The rotated corners are kept while the rotation stays bit-for-bit the same, and only the per-particle scale and offset are applied, in the same order as before, so every vertex is identical.

### Debug lines

Hitboxes and other debug lines are built with a pose that is always the identity, yet vanilla transformed every vertex position and normal through it, allocating a vector each time, and multiplied both ends of every line by the full view matrix only to read the view depth for near-plane clipping. With hitboxes shown this cost several percent of the render thread. The values are now written directly, only the depth row of the view matrix is evaluated with the same arithmetic, and an error is thrown if the pose is ever anything but the identity. When the line buffer duplicates vertices for line quads and has position, color, normal and line width, each line is written straight into the buffer in its final form (start, its copy, end) with the same encoding the buffer builder uses, leaving the end vertex for the builder to copy exactly as it would after its own calls.

### Item quad normals

Sodium writes item model quads by transforming and renormalizing the normal of every vertex, although the four vertices of a quad nearly always share one normal. The transformed normal is now reused while consecutive vertices carry the same normal, so most quads do this work once instead of four times.

### Indirect terrain draws

On GPUs with the multi-draw extension Sodium hands every section's draw commands to the driver through `vkCmdDrawMultiIndexedEXT`, and the driver walks all of them on the render thread each frame. Sodium's indirect path writes the same commands into a GPU-visible buffer and issues one indirect draw per region, so the GPU reads the list instead. It is now used whenever the device supports indirect multi-draw.

### Render thread memory stack

Every LWJGL call that needs scratch memory (descriptor pushes, buffer copies, mapping, glyph drawing, terrain push constants) fetches the thread's `MemoryStack` from a `ThreadLocal`. The render thread's ThreadLocal table is crowded, so this lookup missed its first slot and probed on almost every call, 1.7% of the render thread in the BedWars prelobby. The render thread's stack is now captured once when Minecraft starts, and `MemoryStack.stackGet` returns it directly when called from that thread. It is the same object the ThreadLocal returns, and every other thread still uses the ThreadLocal.

### Empty render phase checks

Draining the frame's submit collections asked every feature render phase whether it was empty, and each check walked all of the phase's per-feature lists and batch maps, 0.3% of the render thread in the BedWars prelobby. Each phase now remembers whether anything was submitted since it was last cleared, which is the only way its lists can fill or empty.

### Camera fluid lookups

Every frame the camera checks whether it is in water, lava or powder snow: one fluid lookup at its block, then a fluid lookup and usually a block lookup at five points of the near plane. The near plane is a few hundredths of a block across, so these points almost always fall in the camera's own block. A point in the same block as the previous one now reuses that lookup. The points, their order and every comparison are unchanged.

### Per-frame allocations

Garbage collection pauses of 6 to 8 ms were the worst frames, and their frequency follows how fast the game allocates. `/txprofile`'s allocation sampling showed several sources in TxOptimizations' own code. Wrapped calls with many arguments boxed every float and int into an array on each call: the glyph writer, when it fell back to vanilla for GUI text, and the baked block entity model swap in Sodium's chunk meshing. They now use injectors that pass arguments without boxing. The per-render-pass set of validated pipelines is one shared set cleared at each new pass, since only one pass can be open at a time. Pipeline uniforms are iterated through the open hash map behind the unmodifiable wrapper, so iteration reuses one entry object. Binding a vertex buffer writes the buffer and offset straight to stack memory instead of wrapping each in a new `LongBuffer`. The driver receives the same values in every case.

### Packed gizmo lines

With hitboxes shown, every entity's box became 8 corner `Vec3`s and 12 `Line` records each frame, plus the list storage for them, about 35 MB/s in the BedWars prelobby. Gizmo groups now keep their lines as plain numbers (start and end coordinates, color and width), box edges are written from the box coordinates directly, and the line renderer reads those numbers. The coordinates are the same doubles the `Vec3`s held, in the same order. Anything that still reads the list as `Line` objects gets them built on access. The two gizmo collections that `LevelRenderer` rebuilt every frame are now cleared and refilled instead, since the previous frame's collections are dropped at that point and were already drawn, so the line arrays keep their capacity and stop being reallocated.

### Region pass lookups

Each region kept its per-pass section data and cached draw batches in hash maps keyed by render pass, looked up for every region and pass each frame. With only three passes they are now also held in arrays indexed by pass, and the lookups read the arrays.

### Repeated binds

Binding a pipeline on a render pass validated its attachments, recorded a pipeline bind, forgot every uniform and then set all of them again, even when that same pipeline was already bound on the pass. Vertex and index buffers were also bound again through a native call before every draw that used the buffer already bound. Binding the pipeline, vertex buffer slice or index buffer and type that is already bound on the pass is now skipped, unless the buffer has been closed so vanilla's error still fires. Vulkan keeps the bound pipeline, its pushed descriptors, push constants and buffer bindings, and every uniform set afterwards still goes through the normal path, so every draw sees the same state as before.

### Clears on load

Every frame cleared the world target, the depth before the held item and the depth before the GUI with separate clear commands, each followed by a barrier that drains the whole GPU, and then started a render pass on the same target that loaded the cleared contents. A clear is now held back and folded into the next render pass on that texture as a clear on load when the pass covers the whole texture, which needs neither the separate command nor the barrier. Buffer uploads and timestamps may pass a held clear because they never touch the image. Any other command, or a pass that does not cover the texture, records the held clear first exactly as before.

### Upload barriers

The Vulkan backend follows every buffer upload with a barrier that makes the whole GPU finish all earlier work before anything else may start. Several uniform uploads happen back to back each frame (global settings, projection matrices, lighting), so the GPU drained once per upload. Consecutive uploads and buffer-to-buffer copies to separate buffer ranges now share a single barrier (a copy also waits when its source or destination overlaps a range still waiting), which is recorded before the next command of any other kind, before an upload that overlaps one still waiting for its barrier, and before the command buffer ends. Timestamp queries do not trigger it.

### World text layout

Name tags, holograms and other text in the world were laid out again every frame: each character looked up its glyph, built a positioned glyph instance and updated the text bounds. Components cache their visual-order sequence, so text the server has not changed arrives as the same sequence object every frame. The laid-out text is now kept per sequence object, position, colors, shadow and outline, and drawn again from the cache. Text with obfuscated characters or with player head and atlas sprite glyphs is never cached, unused layouts are dropped after 200 frames, and everything is dropped when fonts reload or font options change. Drawing a laid-out text only reads it, so the cached copy draws exactly like a fresh one.

### Direct vertex writes

Sodium writes every glyph, particle, entity cuboid and item quad into a small stack buffer and then copies it into the frame's vertex buffer, one native memory copy per glyph or cuboid. At a busy server spawn these copies took about 10% of the render thread. When the vertex buffer uses exactly the format being written, TxOptimizations reserves the space in the buffer first and writes the same bytes straight into it, updating the vertex count and last vertex pointer the same way Sodium's push does. Buffers with another format keep Sodium's converting push.

### Attribute lookups

Every attribute read, such as an entity's scale or name tag distance each frame, looked the attribute up with `computeIfAbsent` and a freshly allocated lookup function. Attribute instances are never removed from an entity's attribute map, so the lookup now reads the map first and only falls back to vanilla's `computeIfAbsent` with the same creation function when the attribute is not there yet.

### Descriptor pushes

Every draw with a changed uniform pushed the pass's whole descriptor set to the driver, and uniforms are set again for most draws even when they hold the same buffers and textures as before. Each render pass now remembers the values and pipeline layout of its last push and skips a push whose values and layout are identical. Vulkan keeps pushed descriptors for the same pipeline layout, so every draw reads the same descriptors as before. A new render pass always pushes.

### Entity shadow blocks

Every entity with a shadow checked the blocks under it every frame: the block below, the light level, whether the block below has a full collision shape and its shape. These answers are now kept per block position for the current client tick and shared by all entities, then dropped at the next tick or level change. The shadow's strength and position are still computed every frame from the entity and the camera; a block or light change under a shadow shows up to one tick (50 ms) later.

### Map colors

When a map's contents change, every one of its 16384 pixels was converted from its packed color byte to a color by looking up the map color and scaling it by the brightness. Map colors and brightness levels are fixed, so the 256 possible results are computed once with the same vanilla function and looked up per pixel. Servers that update maps in item frames often, like leaderboards, pay this on every update.

### Hitbox cuboids

Each hitbox or debug cuboid created 24 corner vectors every frame for its 12 edges and 24 more when filled, although a box has 8 corners. The edges and faces now share the 8 corner vectors, which are immutable, so the same lines and quads are emitted with fewer allocations.

### Feature renderer list

Preparing each frame walked the list of feature renderers through a freshly built Guava filter that skips empty slots, three times per frame preparation. The filtered list only changes when a renderer is registered, so it is built once at registration and reused.

### Entity visibility reuse

With entity hitboxes shown (F3+B), the hitbox renderer runs the same visibility check for every entity that entity extraction already ran earlier in the frame. Each entity remembers its result for the frame, and a second check with the same frustum, camera position, partial tick and fade setting reuses it. Any call with different inputs computes normally.

### Block outline edges

The outline around the targeted block walks the block shape's voxel grid every frame to find its edges, which took about 2% of the render thread on complex shapes. The edges of the last 8 shapes are kept and replayed in the same order, so the outline is identical. Block shapes are immutable and shared, so a new shape object is simply recomputed.

### FPS counter

TxOptimizations draws its own FPS counter in the top left corner (`123 FPS`, white, no shadow, at 2,2), hidden while the F3 screen is open or the HUD is hidden. The number is the average frame rate over the last 0.5 seconds and updates every 0.5 seconds, the same as Sodium Extra's FPS overlay. It only counts frames, and the text is rebuilt only when the number changes, so drawing it skips the per-frame translation and text-direction work that Sodium Extra's overlay goes through.

Sodium Extra records every frame and sorts the last 5 seconds of frame times twice every 0.5 seconds for its 1% and 0.1% low values, even when its overlay is off, which took about 0.8% of the render thread at high frame rates. When Sodium Extra is installed, TxOptimizations skips that recording, so Sodium Extra's own FPS overlay (its Show FPS option, `show_fps` in `config/sodium-extra-options.json`) should be switched off.

### Baked block entities

Chests, ender chests, shulker boxes, bells, skulls, banner poles, decorated pots, copper golem statues and cushions are baked into the chunk mesh like normal blocks instead of being drawn again every frame. They switch back to per-frame rendering only while they move: a chest or shulker box while its lid is open, a bell while it swings, a decorated pot while it wobbles, an animated dragon or piglin head. Banner flags keep waving because only the pole and bar are baked. Player heads and decorated pots with sherds stay per-frame, and a named cushion only draws its name tag per frame. In a server lobby profile this cut block entity rendering from about 4.1% to 0.35% of the render thread.

Block entities with nothing to draw are skipped: signs without text, beacons without a beam, campfires and shelves without items and lecterns without a book. The answer is remembered on each block entity for the current client tick, so a sign that gains text shows it at most one tick later. Sign text facing away from the camera is not drawn.

### Atlases and batching

- Font atlas: font glyph textures are 1024x1024 instead of 256x256, so text spans far fewer textures and needs fewer texture switches and draw calls.
- Map atlas: map images are packed into shared 2048x2048 textures, so walls of maps in item frames draw in a few calls instead of one per map.
- Glyph vertex builder: consecutive glyphs of the same render type reuse the same vertex builder instead of looking it up for every glyph.
- Animated item atlas: animated item icons in the GUI share one atlas that is cleared once per frame instead of slot by slot.
- Scissor state equality: vanilla merges draws of the same prepared render type inside groups it marks as reorderable, but `RenderType.prepare` copies the scissor state every time and `ScissorState` has no value equality, so two prepared render types never compare equal and the merge almost never happens. Comparing scissor states by value lets that vanilla merge work, which cuts draw calls. Strictly ordered groups are untouched.

When a loaded resource pack overrides the `core/text` shader, the font atlas stays at 256x256 unless the pack lists `font_atlas_resizing` under `compatible_features` in an `immediatelyfast` metadata section, and the map atlas is switched off when the pack lists `map_atlas_generation` under `incompatible_features`. Server resource packs with custom text shaders therefore look the same as in vanilla.

### HUD cache

The HUD is drawn into an offscreen framebuffer at up to 60 frames per second (20 while a screen is open), and every frame only blends that cached image over the world. Each HUD frame is spread over 3 render frames: HUD layers are split into 3 groups of roughly equal measured cost, and each frame redraws one group, so no single frame pays for the whole HUD. Animations inside cached layers advance by the summed frame time of the cycle. Layers whose blending reads the color underneath them, like the crosshair, switch to being drawn every frame the first time they appear, and so does anything drawn outside a registered HUD layer, including the FPS counter. Resizing the window, opening or closing a screen or hiding the HUD redraws the HUD directly for two cached frames so nothing shows stale. The cached image is blended over the world only inside up to 8 non-overlapping rectangles covering the bounds of everything drawn into it, padded by 2 GUI pixels, instead of over the whole screen, so the GPU blends the areas around the hotbar, chat, scoreboard and other HUD parts instead of every pixel. Vanilla drops GUI elements without bounds and orders overlapping elements by them, so every drawn element reports them.

### Rain culling

Rain and snow are built column by column around the camera every frame. Columns whose vertical span lies outside the camera's view are skipped. Each column draws with its own position-seeded randomness, so the visible rain looks exactly the same.

For every column that is drawn, vanilla also asked the level for the precipitation type at that block, which looks up the biome through the biome zoom noise and the biome temperature. In rain a spark profile showed this at 20% of the render thread. The answer only depends on the chunk being loaded and its biome data, so it is cached per block position and the cache is dropped whenever a chunk is loaded, unloaded or gets new biomes, and when the level changes. The column bounds come from the values vanilla already computed instead of a second heightmap lookup.

### Entity culling

Entities and block entities hidden behind solid blocks are not extracted or drawn. Every 5 ticks the client tick records the culling box and position of every loaded entity, plus every block entity within 64 blocks of the camera whose renderer isn't drawn off screen. A background thread then traces rays from the camera to sample points on the camera-facing faces of each box, through blocks whose state is a solid render cube, and marks it culled when no ray gets through. This runs again every tick the camera moves or a new capture arrives. Ray results per block are cached for the pass, and only the touched cache entries are cleared before the next one.

Entities farther than 128 blocks, glowing entities, all players, entities whose renderer ignores culling and boxes larger than 50 blocks are never culled, and anything found visible stays visible for at least one second. Players are never culled because the capture only runs every 5 ticks: a player stepping out from behind a wall would otherwise stay hidden until the next capture and then appear at once. Culled entities that show their name still draw the name tag and the below-name score through walls, at the same distances vanilla uses. Block entities outside the camera's view are skipped as well, without allocating a box.

Entities that are culled, or were not extracted in any frame since the previous tick, only run a basic tick on the client: vanilla's common tick (old position and rotation, invulnerability timer, position interpolation, tick count), `aiStep`, hurt time and the warden heartbeat. A tick with no frame since the previous one, below 20 FPS, never counts entities as out of camera. Players, the camera entity, vehicles, boats, minecarts, display entities and firework rockets always tick fully.

### Persistent buffer mapping

Every write into a CPU-visible GPU buffer (dynamic transforms, uniforms, fog, clouds, vertex staging, Sodium's indirect command and uniform buffers) mapped the buffer's memory with `vmaMapMemory` and unmapped it again when the view closed, two native calls per write. A spark profile showed these calls at 2.8% of the render thread. Each buffer is now mapped the first time it is written and stays mapped until it is destroyed. Every mappable buffer is allocated host-visible and host-coherent, so the GPU sees the same bytes at the same addresses without any flush, and vanilla's check that a buffer is not closed while a view is open still works.

### Texel buffer views

Pushing descriptors that use a texel buffer (Sodium's section time buffer for every terrain layer, the cloud buffer) created a Vulkan buffer view on every push and queued it for destruction two frames later, 1.4% of the render thread. The view only depends on the buffer, offset, size and format, so it is now kept on the buffer (up to 8 per buffer) and destroyed together with the buffer, after the GPU is done with it.

### Pipeline switches

Switching pipelines inside a render pass re-applied every uniform ever set in that pass through a hash map pass, and checked the pipeline's color formats against the pass attachments again. Only the new pipeline's own uniforms are now applied, which gives the backend the same values, and the format check runs once per pipeline per render pass.

### Present thread

The frame's queue submit and its present are native driver calls that took a third of the render thread in the prelobby. Both now run in the same order on one dedicated thread, while the render thread already moves on to the next frame. With vsync off, the next frame acquires its screen image just before it copies to the screen instead of at the start, after the present thread has finished, so the render thread builds the whole frame while the previous one is being presented. Every other queue or swapchain call waits for the present thread first, and the same image is presented in the same order. With vsync on, the image is still acquired at the start of the frame, so input latency is unchanged. With vsync off the swapchain also gets one more image than vanilla asks for, when the driver allows it. In immediate mode a finished image is shown as soon as it is presented, so the extra image adds no latency; it only means the late acquire does not have to wait for the presentation engine to release an image while the GPU is the bottleneck.


The present thread only wins while the CPU is the bottleneck. With the GPU as the bottleneck, NVIDIA's driver blocks in the late acquire and the GPU loses work, so the frame then runs vanilla's order on the render thread instead: acquire at the start of the frame, then submit and present right after the frame is recorded. Both orders draw the same image. Which one is used is decided from what each order measures about itself over every 128 frames, without ever running the other order on trial: in vanilla's order, a render thread that waits for the GPU less than 10% of the frame time is CPU-bound, so the present thread takes over; with the present thread, an acquire that blocks more than 8% of the frame time means the GPU is the bottleneck, so vanilla's order takes over. In the measured scenes these values are 1.7% against 40% and 1.0% against 19.5%, far from both limits. `/txprofile` reports the share of frames that used the present thread.
### Submit collections

The hand and screen effect passes drained their submit collections twice per frame and threw away the empty ones, so the next frame built them again with all their phases. Empty collections are now kept and reused; an empty phase draws nothing.

### GUI item atlas sweep

At the end of every frame the GUI item atlas walked all its slots looking for slots marked to be dropped after the frame. The sweep now only runs when a slot was marked since the last sweep.

### Sodium per-frame checks

Sodium read the operating system name to pick the sub-texel precision every frame and updated its chunk build time estimators every frame. The precision is computed once, and the estimators only update after new build results arrived; without new data the update leaves them unchanged.

### Viewport, scissor and pipeline binds

Every render pass set the viewport and scissor and bound its first pipeline again, even when the command buffer already had exactly that state from the previous pass. The bound pipeline and both dynamic states stay valid across render passes in the same command buffer, so a viewport, scissor or pipeline that is already set is no longer recorded again. `vkCmdBindPipeline` alone was 2.8% of the render thread in the BedWars prelobby. Pushed descriptors also stay valid across render passes, so the check that skips pushing an identical descriptor set with the same pipeline layout now spans the whole command buffer instead of a single pass. The remembered state is dropped whenever a new command buffer starts recording.

### Particle vertices

Particles were turned into vertices one at a time: a callback per particle, Sodium's per-particle hook with its own buffer lookup, and a separate vertex reservation for each quad. Each particle layer is now written in one pass straight from the stored particle arrays into one reservation, with the same corner math, colors, UVs and light, so the vertex data is identical. Buffers that cannot be written directly still go through the vanilla per-particle path.

### Model part rotation

Every rotated model part (entity limbs, heads, armor) rotated both its pose matrix and its normal matrix with JOML's `rotateZYX`, and each call computed the same three sines and cosines again. The angles are now turned into sines and cosines once and both matrices are rotated with JOML's own formulas, including its separate paths for identity, translation-only and affine matrices and the matrix property flags it sets. A test of 2,000,000 random matrices and angles against JOML 1.10.9 matched every bit.

### Item model bounds

Items on shelves and dropped items asked for their model's bounding box every frame, and because their render state is cleared or recreated every frame, vanilla rebuilt the box from every vertex of the model each time (about 1% of the render thread at a server spawn full of shelves). The box only depends on the model's extents, its item transform, the hand and its local transform, so single-layer items now keep it in a cache keyed by exactly those values. Items with Fabric mesh quads, which add their own vertices to the box, and multi-layer items still compute it every time.

### Region bookkeeping

Sodium walked its whole hash table of terrain regions every frame to let each region free GPU memory it no longer needs, and most of that time went into stepping over empty table slots (0.87% of the render thread in a singleplayer profile). Regions are now also kept in a plain list that is updated when a region is created or removed, and the per-frame pass walks that list. Every region is still visited once per frame; only the order differs, which only changes which free region id is handed out next.

### Raycast air

Every block a raycast passes through (the per-frame crosshair pick, the third-person camera collision) asked the block for its shape. Air never has a shape in any raycast mode, so air blocks now return the empty shape directly.

### Particle collision

Every particle with physics checked its movement against the blocks around it each tick by collecting every block collision shape in the swept box into a new list (up to 0.8% of the render thread at a server spawn full of dust particles). Particles have no entity colliders and no world border, so when every block in that box is air, vanilla returns the movement unchanged. The box is now first checked directly in the chunk sections, skipping all-air sections whole, and only boxes that contain any non-air block go through vanilla's collision.

### Ambient block ticks

Every client tick, 1334 random blocks around the player get their ambient effects (campfire smoke, dripping water, ambient particles), and each one was looked up twice, once for the block and once for its fluid. The fluid is part of the block state, so it is now read from the block state already looked up. The debug world, where block lookups do not come from the chunk sections, keeps the second lookup.

### Animated sprite marks

Sodium only animates textures that are on screen, so every frame it marked every animated sprite in every visible section as active (0.85% of the render thread in a singleplayer profile). The mark is only cleared when the texture atlases advance their animations once per client tick, so marking the same sprites again in later frames of the same tick changes nothing. The marking pass now runs again only after an atlas animation step, a new set of render lists, or a change to any section's animated sprites.

### Translucent sort reuse

Translucent entity, item and text quads are sorted back to front before every upload, 3.4 to 4.7% of the render thread at busy server spawns. Each sort's result is now remembered by its position among the frame's sorts, and the next frame first checks in one pass whether the remembered order is still sorted under the new distances (ties ordered by quad index). If it is, it is exactly the order a stable sort would produce, so it is used without sorting. Otherwise the quads are sorted with the same orderings Sodium uses (signed keys up to 80 quads, unsigned above), using packed key and index values, a reused buffer, and skipping radix passes where every key lands in the same bucket. A test of 26,037 random sorts against Sodium's own sort matched every result. None of this allocates in steady state: the quad centers are decoded into an array kept per quad count and zeroed before each use, the sort keys go into one reused array, and each remembered order is rewritten in place. The index writer reads the remembered order directly, and only callers outside the upload path get their own copy. Together these were about 66 MB/s of garbage in a BedWars game.

### Transient command buffer

Every frame started a command buffer for copies into transient GPU memory and submitted it first, even though nothing in the game records such copies in a normal frame. That buffer is now only started when the first copy is recorded, and it is then placed first in the frame's submission, exactly where it would have been. Frames without such copies submit one command buffer fewer.

### Continued render passes

Ending a render pass recorded the end of the rendering instance and a full barrier right away, and the next pass began a new instance, even when it drew into exactly the same color and depth textures. The end of a pass is now held back. If the next pass uses the same attachments and render area, clears nothing and nothing else was recorded in between, it keeps drawing in the same rendering instance, as happens from the sky pass into the main world pass in clear weather. Draws inside one rendering instance keep their order for color, depth and blending, and a pass cannot sample its own attachments, so the image is the same. Anything else that records a command, ends the command buffer, or begins a different pass ends the held pass first, exactly as before. The GPU pass profiler disables this while it records.

### Frustum box test

Every entity, block entity and rain column visibility check asked JOML for a full box/frustum classification (outside, inside or intersecting), which tests each plane twice, and then only used whether the box was outside. The check now uses JOML's plain box test, which runs exactly the first of those two tests per plane in the same order, so the answer is identical with half the plane math.

### Dynamic transform equality

Each draw's model-view, color, offset and texture matrix are compared with the previously written ones to reuse the uniform slice. That record's generated `equals` went through method-handle dispatch on every comparison (0.2 to 0.4% of the render thread). It is now a plain comparison of the same four components with `Objects.equals`, the cheap vectors first, which returns the same answer.

### Identity model parts

Every visible model part pushed a copy of the pose stack, applied its offset, rotation and scale, drew, and popped the copy again. Sodium already skips the transform when a part has no offset, no rotation and unit scale, so for those parts (roots, hats, jackets, sleeves and other overlay layers) the push and pop only copied two matrices. Such parts now draw and recurse into their children on the current pose directly; parts with any transform still push, transform and pop.

### Rotation-only culling

Sodium's cull thread rebuilds three section trees every frame the camera changes, which on servers kept it busy for 21 to 67% of a core while just looking around. Only the narrow tree is frustum tested; the regular and wide trees depend on the camera position, the search distances, the occlusion setting and which sections are waiting to rebuild. While none of those change and no section is pending, a rotation-only frame now keeps the previous regular and wide trees, which the renderer already frustum tests per frame during movement. The sections right next to the camera are added to those two trees even outside the frustum, so they no longer depend on the view direction. When the camera stops, a full cull runs once and the narrow tree comes back as before.

### Entity check order

Sodium wraps every entity renderer's visibility check and runs its own section test first, which reads the entity's glowing flag, builds its culling box and walks the section tree, and only then lets vanilla reject the entity by distance and frustum. On servers that cost about 2% of the render thread, mostly spent on entities behind the camera. Both checks are side-effect free, so the entity pass now asks vanilla first and runs Sodium's test only for entities vanilla would draw. Renderers that override the check (shulkers, guardians, end crystals and any modded override) keep the original order.

### Model pose reset

Every model reset its parts before animating by walking an immutable list through its iterator. The parts are now copied into an array when the model is built and reset in a plain loop, with the same parts in the same order.

### Bare armor stands

Before drawing its layers, every armor stand set up its full pose and then asked the armor, held item, wings and head layers to draw, even when it wore and held nothing, which is what servers use for floating text. When the renderer has exactly the vanilla layers and the stand has no equipment, no held items and no head, that pose setup and the four empty layer calls are skipped. The stand's own model still animates when it is drawn.

### Cuboid normals

Sodium transformed all six face normals of an entity cuboid every time the normal matrix changed (1% of the render thread in singleplayer). Opposite faces use exactly the negated matrix column, so their packed normals are the byte-wise negation of each other. Only the up, south and east normals are transformed now, and down, north and west are flipped from them, which gives the same bytes (checked on 40 million matrices including NaN, infinities and signed zeros).

### Collision cursor

Block collision scans (particles moving near blocks, the player's suffocation check) walked their cells by splitting a running index with two divisions and two remainders per cell. The cursor now steps x, then y, then z with counters, which visits the same cells in the same order (checked against the division form on every box up to 12 by 12 by 12).

### Render type equality

Batching draws compares prepared render types with their generated record equality, which went through method-handle dispatch and compared the name string before anything else. It now compares the same six components with `Objects.equals`, cheapest first (pipeline, transform slice, scissor, OIT set, name, textures), which returns the same answer.

### Vertex upload copy

Every frame's entity, text, item, GUI and debug geometry used to be written into a CPU-side staging buffer, then copied by the GPU into the real vertex and index buffers, with a barrier around each copy. That was 5 copies per frame and 0.018 ms of GPU time in a GPU-bound world. The vertex and index buffers are now allocated mappable, and the vertices and sorted indices are written straight into them, so both copies and the staging buffer are gone. Buffers only return to the pool after the GPU has finished with them, so nothing in use is overwritten. Each slice is copied straight from the vertex builder's memory with the same validity and overflow checks, and the GPU receives identical bytes.

### Uniform writes

Every entity, text or item draw with its own transform wrote that transform into a ring buffer through a fresh mapped view of the buffer. Each write allocated a pointer buffer, a byte buffer, a buffer slice, a mapped view and its close callback (over 16 MB/s of garbage in a busy server lobby). Under Vulkan each ring buffer now keeps one mapped view, opened the first time it is written and closed with the ring buffer, and every transform is written through it. Each transform is written at the same offset with the same limit, so the GPU reads identical bytes. The memory is host-coherent, as it was before, so no flush is involved. Other backends keep mapping per write.

### Model cube writes

Sodium writes entity model cubes through a cancellable hook on every cube, which allocated a callback object per cube per frame (about 5 MB/s in a world with mobs). Model parts now hand their cubes straight to Sodium's cuboid writer, with the same color conversion, light and overlay, so every vertex is identical. Vertex consumers Sodium cannot write to still go through the cube's own method.

### Entity culling boxes

Every entity checked against the view frustum got a new bounding box, grown by half a block, every frame (about 11 MB/s in singleplayer). An entity that has not moved keeps the same bounding box object, so the grown box is now kept per entity and reused while the box it came from is the same object. A moving entity gets a new box as before.

### Particle draw maps

Every particle group built two new identity maps per frame to collect its draws and textures per layer. The maps are now cleared and reused once the frame's particles are drawn. A cleared map keeps its table size, so it lists its layers in the same order a new one would; a map that ever held enough layers to grow is dropped instead of reused.

### GUI glyph matrix

Every GUI glyph converted its 2D pose into a new 4x4 matrix before writing its vertices. One matrix is now reset to identity and reused for each glyph, and the glyph writes its vertices before the next one starts.

### Buffer slices

Asking a GPU buffer for a slice covering all of it created a new slice object each time, and Sodium does that for every chunk region drawn each frame. The slice is now created once per buffer, since a buffer never changes size. Sodium's indirect terrain draws also created a slice of the command buffer per region per frame; each draw batch now remembers its last 8 slices, with their buffer, offset and length kept in flat arrays so a miss while moving costs only a few comparisons, and reuses one with the same buffer, offset and length.

### Feature phase loop

Each frame, the translucent pass walked every feature phase of every submit collection, and each phase with nothing to draw built an iterator over an empty list (about 8 MB/s in a BedWars game). Phases with no groups now return straight away, and the rest are walked by index in the same order.

### Projection uploads

The GUI, the item atlas and picture-in-picture renderers upload their projection matrix to the GPU with a buffer copy every frame, because their projection's version number changes each time it is set up, even when the matrix stays the same. Each projection buffer now remembers the matrix it last uploaded and skips the copy when the new matrix is bit-for-bit the same, so the buffer already holds exactly those bytes.

### Suspicious sand and gravel

Every suspicious sand and gravel block in view built a full render state each frame, including an item render state with its own matrices (about 45 MB/s of garbage on a BedWars map), although it only draws its buried item while it is being brushed. Blocks that have not been brushed at all now skip the render state, since vanilla's renderer draws nothing for them.

### Block entity distance

Every block entity in view allocated a vector for its center to check whether it is within the renderer's view distance (about 6 MB/s on a BedWars map). Renderers that use the default distance check now compute the same differences and squares directly from the block position, in the same order, so the result is identical. Renderers with their own check, such as beacons, still run it.

### Render type textures

Every time an entity, item or text render type was prepared for a frame, it built a new list of its textures (overlay, lightmap and each bound texture with its sampler), allocating the list and one record per texture. Each render type now keeps the list it built last and returns it again when every entry would be the same: same name, same texture view object and same sampler object, looked up the same way and in the same order as before. Any change, such as a reloaded texture or a different lightmap, builds a new list.

### Entity cull order check

Deciding whether an entity's culling can be deferred to Sodium looked up the renderer's class in a class-keyed table for every entity every frame (1.3% of the render thread in a world full of entities). Each renderer now remembers the answer itself after the first lookup, since a renderer's class never changes.

### Sound channel wait

Every sound the game plays asks the sound thread for an audio channel and waits for the answer on the render thread. The wait parked the render thread, so on top of the sound thread's own wake-up it also had to be woken by the operating system again, 0.3 to 0.5 ms per sound in `/txprofile`. The render thread now spins until the channel arrives and then takes it the same way, so the same channel is used at the same point, without the second wake-up.

### Terrain push constants

For every chunk region it draws, Sodium writes the region's camera offset, age and id into 20 bytes of stack memory and pushes them to the shader. Taking those bytes from the memory stack wrapped them in a new `ByteBuffer` each time (about 20 MB/s of garbage in a singleplayer world at render distance 32). The push constants are copied into the command buffer when they are pushed, so one buffer is now reused for every region and receives the same bytes.

### Light snapshots

The light engine keeps one map of light sections it updates and publishes a read-only snapshot of it for every other thread. Every time lighting changed, vanilla published a full copy of the whole map. At render distance 32 that map holds around 100,000 sections, so each copy allocated arrays of several MB on the render thread and on every server worker thread. While moving, `/txprofile` showed most garbage collections forced by these huge arrays, each pausing the game for 8 to 10 ms.

A snapshot is now the last full copy plus a small map of the sections that changed since it. Each new snapshot copies the previous small map and adds the sections the light engine recorded as changed since the previous snapshot, with their current data. Lookups check the small map first and then the full copy, so every lookup returns exactly what a full copy would. Nothing ever modifies a published full copy or a published change map. Once the changes since the last full copy would exceed a sixteenth of the sections (at least 512), the next snapshot is a full copy again, the same way vanilla makes it, and becomes the new base.

### Visible region lookup

Every time Sodium collects the visible chunk sections, it looks up each section's render region (8 by 4 by 8 sections) in a hash map, 2.6% of the render thread while flying in singleplayer. Sections are visited in tree order, so consecutive sections usually share a region. Each collection now remembers the last region it looked up and its coordinates, and reuses it while the next section is in the same region. A collection only reads the region map and runs on one thread from start to end, so the region is the same one the map would return.

### Vanilla section graph

With Sodium, vanilla's section occlusion graph still updated every frame, although Sodium gives it a view area that contains no sections, so every result it can produce (visible sections, chunks it waits for, the culling and octree debug views) stays empty. Each time the camera entered a new section it still copied the sets of loaded chunks and empty sections (about 10 MB/s of garbage while flying at render distance 32) and ran a full graph update on a background thread. The update is now skipped while Sodium's empty view area is in use. Sodium does its own terrain culling in the same frame as before, and the frustum flag the graph would set only gated a call that Sodium already replaces with nothing.

### Entity hitboxes

With hitboxes shown (F3+B), every visible entity built its boxes and its view arrow from temporary vectors every frame: an offset vector, the moved bounding box computed twice for living entities, a new stroke style per box, and a scaled view vector. Each arrow then built a rotation, five vectors and an array to draw its four tips. The hitbox boxes now share one immutable stroke style per color, the moved bounding box is computed once from the offset's components, and the arrow end is computed from the view vector's components, all with the same arithmetic as before. Arrows rotate their tips with one reused rotation and vector and write the lines straight into the packed line list, so the GPU receives identical lines.

### Unused region cache

Every frame, vanilla's level extraction created a new region cache for rebuilding the visible sections it tracks. With Sodium that list of sections is always empty, so the cache was never used, but each one still allocated its hash map (about 4 MB/s of garbage in a server lobby). While the list is empty, one shared cache that is never touched now stands in for it. If the list ever has sections, a new cache is created as before.

### GUI texture setups

Every GUI glyph described its textures with a new texture setup record: its glyph texture and sampler plus the lightmap and its sampler. Text in the HUD and menus uses the same few textures over and over, so the last setup is now kept and returned again when the glyph texture, its sampler, the lightmap and the lightmap sampler are all the same objects. The record is immutable, so callers get an equal value either way.

### Breaking overlay rotation

The block breaking crack overlay computes its texture coordinates per vertex and called `Direction.getRotation()` for every vertex, which allocates a new quaternion and evaluates its sines and cosines (1.3% of the render thread in a CubeCraft lobby). The six rotations are now built once by that same method and reused. The overlay only reads them, so every vertex gets the same values.

### Entity render types

Every model asked its render type function for the render type of its texture once per entity per frame, which looks it up in a concurrent memo map keyed by a `Pair` (0.7% of the render thread with many mobs on screen). Each model now remembers the last texture and the render type it got for it, in one immutable pair, and returns that render type again while the texture is the same object. Vanilla's model render type functions are memoized, so the same texture always gives the same render type.

### Draw group lookup

When an entity asks for a vertex builder, its prepared render type is searched in the current draw group with `List.indexOf`, comparing it with every draw so far (1.5% of the render thread with many different mob textures). Each group now keeps a hash index from prepared render type to its first position, filled when vanilla adds it, so the lookup finds the same draw directly.

### Sky behind terrain

Vanilla draws the sky disc, sunrise glow, sun, moon and stars over the whole screen before any terrain, and the opaque terrain then paints over most of it. In a server lobby these sky passes took 0.019 ms of a 0.131 ms GPU frame. In the overworld with the default transparency, the sky is now drawn right after opaque and cutout terrain and before everything else, with copies of the vanilla sky pipelines whose vertices are placed at the far plane and depth tested against the terrain, so only pixels without terrain are shaded. Terrain does not blend and always writes depth, entities, particles, text, outlines and translucent terrain are still drawn after the sky, and the sky layers keep their order and blending, so the image is the same. The vanilla sky shaders get the far plane line only under a define that just these copies set; if a resource pack replaces one of them and the expected line is missing, the sky is drawn the vanilla way.

### RGSS close-up terrain

With RGSS texture filtering, Sodium's terrain shader takes four texture samples around every pixel, plus one sharp sample, and blends them by how small the texture is on screen. In a server lobby terrain is limited by pixel shading (a fifth of the pixels made solid terrain three times faster), and most nearby terrain shows texels at least a pixel wide, where that blend weight is exactly 0 and the result is exactly the sharp sample. The shader now returns the sharp sample right away in that case and skips the four RGSS samples; every other pixel is computed as before. The line is added to Sodium's shader source as it loads, and a replaced shader without the expected line is left alone.

### Memory

Less live memory means shorter and rarer garbage collections.

- Block state neighbor table: instead of every block state holding its own table of neighbor states, all states of a block share one bit-packed table that `setValue` indexes into. `getValue` keeps reading vanilla's per-state value array.
- Block state cache deduplication: identical collision shapes and face sturdiness arrays in the per-state cache are shared instead of stored once per state.
- Empty item component patches: items with no changed components share one empty map instead of each allocating their own.
- Uniform light layers: a 2 KB light array where every value is the same non-zero level (sky sections at full light, sent by the server or loaded from disk) is stored as just that level, the form vanilla already uses for freshly lit sections. Reads return the same value and the array comes back filled on the first write. A heap dump of a 32 chunk singleplayer world had about 14,000 such arrays (29 MB).

## Commands

`/txprofile` records 10 seconds and saves one report to the `txoptimizations` folder. It starts with the frame rate from every frame's time: average, 1% low and 0.1% low (the frame rate at the 99th and 99.9th percentile frame time) and the worst frame, followed by GPU command counts per frame (render passes, pipeline sets, draws, multi-draw calls, indirect and batched draws), the scene (server or world, dimension, position, loaded entities and players, particles), whether hitboxes, chunk borders and the F3 overlay are on, the video options, the GPU and driver, and the JVM with its collector, heap and launch flags. Then come frame time percentiles (p50 to p99.9), how many frames took longer than 1, 2, 4, 8 and 16 ms, and the frame count, average frame rate and worst frame of every second. The GPU part lists the time of every render pass and pipeline per frame, plus terrain draws and triangles. It also lists the 10 frames that took the GPU longest, each with its 4 most expensive passes, timed when the frame's results were read. The CPU part samples the render thread's call stack every millisecond and lists every sampled method by its own time (with its top callers), the 30 hottest call stacks up to 12 frames deep, and every method by its time including callees, plus the CPU time of every thread. A separate section covers only the slowest 1% of frames: what the render thread was doing inside them, their 15 hottest call stacks, the 10 worst frames with the samples taken during each, and every garbage collection with its time, length, the old generation's size after it and the used and committed size of all memory pools, marked on any slow frame it overlaps. It also lists how much memory each thread allocated per second and how much the render thread allocates per frame, which is what fills the young generation and sets how often those collections happen. The JDK's built-in Flight Recorder adds JVM events: every safepoint, every VM operation by name (garbage collection, deoptimization, handshakes and others) with the longest ones, every JIT compilation with the longest ones, deoptimizations by method and reason, and every time the render thread parked or waited for a lock for 0.2 ms or longer, by call site. It also samples the allocations, listed per thread by class and by allocation site six frames deep. About 10000 samples make one sample roughly 0.01%, so shares below that are not measurable. Sampling another thread's stack briefly pauses it, so the render thread runs a little slower while the profile records. Copy labels are worked out once per buffer, and the profiler's timestamp queries are reset in one call per frame after reading them instead of one call per query, to keep the profile's own cost low.

`/txocclusion` measures how much drawn terrain in the next frame is hidden behind other terrain.

## Fixes

### Command suggestion crash

Typing a command in chat updates the usage hint once the suggestions arrive, on whichever thread finished them. Measuring that hint can bake a new font glyph, which records a texture upload into the render thread's command buffer from another thread and crashed the NVIDIA driver. The update now runs on the render thread, and is skipped if newer suggestions replaced it in the meantime.

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
