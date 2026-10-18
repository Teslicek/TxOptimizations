# TxOptimizations

Client-side Fabric mod for **Minecraft 26.3** (Java 25) that removes repeated per-frame work from the render thread without changing settings or what you see.

## Optimizations

### Rain fog

Every frame, `AtmosphericFogEnvironment.updateRainFogState` looks up the biome and the sky light at the camera position to fade in rain fog. A spark profile showed the sky light lookup alone taking about 2.8% of the render thread. TxOptimizations looks both up once per client tick and reuses them for the other frames of that tick, as long as the camera stays in the same block of the same level. The values only feed a slowly fading fog multiplier, so the result is visually identical.

### Particle light

Every frame, each visible particle looks up the light level at its position again (`Particle.getLightCoords`). Particles only move once per tick, so at high frame rates the same lookup repeats many times per tick. TxOptimizations caches the result per particle for the current client tick and position. A light change reaches particles at most one tick (50 ms) later. Particle types that compute their own light without calling the base method are unaffected.

When a particle does look its light up again, the block position is now written into one reused mutable position instead of a new `BlockPos` (3.7 MB/s with 13,000 particles). The light lookup only reads the position, so it sees the same block.

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

At the end of every frame, the vertex buffer pools close every spare buffer that was not used in that frame, so a buffer that is needed again a frame later has to be created again. TxOptimizations keeps a spare buffer for up to 3 idle frames before closing it, and only clears its idle-frame counts when there are any to clear. Spares that get reused never reach that limit, and buffers the GPU is still using are never touched. Gnetum's HUD flushing still skips the cleanup the same way it does in vanilla.

### Translation templates

Every time a translated text is resolved, `TranslatableContents.decomposeTemplate` runs a regular expression over the translation string to find `%s`, `%1$s` and `%%`. TxOptimizations parses each distinct translation string once, remembers the literal parts and argument positions, and replays them in the same order with the same argument lookups. Invalid translation strings are left to vanilla so they fail and fall back exactly as before. Up to 4096 strings are kept, and the cache is cleared when it fills up.

### F3 entry checks

`DebugScreenEntryList.isCurrentlyEnabled` searches the list of enabled F3 entries one by one, and it is called for many entries every frame. TxOptimizations answers from a hash set built from that list, rebuilt whenever vanilla's own version counter for the list changes. `getCurrentlyEnabled` also made a fresh immutable copy of the list on every call, several times per frame; the copy is now kept and handed out again until that same version counter changes.

### Particle layers

Every frame, each particle quad is added to `QuadParticleRenderState`, which looks up the storage for the particle's layer in a hash map. The layer is a record, so every lookup hashes its fields again. Particles only ever use six constant layer objects and the map never loses an entry, so TxOptimizations remembers each layer object it has seen together with its storage and returns that storage directly. A layer it has not seen yet still goes through the map.

### Buffer builder elements

Every new `BufferBuilder` looks up up to eight vertex elements (position, color, UVs, normal, line width) by name in its vertex format, and the format's element map compares the names one by one. Vertex formats never change, so TxOptimizations resolves those eight elements once per vertex format with the vanilla lookup and reuses the result for every later `BufferBuilder` of that format, including the constructor's check that the format has a position element. The constructor passes the name straight from its name table, so the index is found by comparing that exact string object against the eight table entries, and the element is taken from the cached array at that index instead of being searched for by name. Reading the constructor's loop index directly made MixinExtras allocate an int holder for every new `BufferBuilder` (13 MB/s on minemen).

### Player skin model lookup

Every frame, `EntityRenderDispatcher.getAvatarRenderer` reads each player's skin through the player list and skin manager to choose the wide or slim player model, once for culling and once for drawing. TxOptimizations keeps the skin it read for that player during the current frame and reuses it for the second call. A skin that finishes loading is picked up on the next frame.

### Translucent sorting

Translucent geometry such as entity translucency, text, see-through name tags and glints is sorted back to front every frame. Vanilla reads the center of every quad with six bounds-checked `ByteBuffer.getFloat` calls and writes the six sorted indices per quad through an `IntConsumer` lambda. TxOptimizations reads the same floats straight from native memory when the buffer is direct and in native byte order (one bounds check for the whole range up front), computes the centers with the same arithmetic, and writes the same index values straight to the buffer's memory when it is direct and in native byte order, with one bounds check for the whole range, and through a short or int view of the buffer otherwise. The bytes written and the final buffer position are identical.

### Glow check spectator state

Entity extraction asks `Minecraft.shouldEntityAppearGlowing` for every entity every frame, and for every entity that is not glowing vanilla checks whether the local player is a spectator. TxOptimizations computes that once per frame and reuses it for the other entities.

### Environment attribute values

Sky color, fog color and other environment attributes are read through `EnvironmentAttributeProbe$ValueProbe.get`, which blends the previous and current tick values for the frame's partial tick every time it is called, several times per frame. The blend only depends on those two values and the partial tick, so TxOptimizations remembers the last result and returns it while both values and the partial tick are unchanged.

### GPU crash checkpoints

When the GPU supports `VK_AMD_buffer_marker` or `VK_NV_device_diagnostic_checkpoints`, renderpearl writes a marker into the command buffer at every render pass so a driver crash report can name the last pass that ran. That took about 0.4% of the render thread. TxOptimizations makes renderpearl use its built-in no-op checkpoint storage, the same one it uses on GPUs without those extensions. Rendering is unchanged, but a GPU driver crash report no longer lists the last render passes.

### Particle rotation

Every camera-facing particle allocated a new rotation quaternion each frame. The quaternion is now reused, reset to identity first as the new one was.

Each particle then rolls that quaternion by its roll angle, which computed a sine and cosine for every particle every frame even though almost every particle has a roll of zero. With 13,000 particles on screen, `SingleQuadParticle.extract` took 21% of the render thread. For a roll of exactly zero, JOML's sine of half the angle is that same zero and its cosine is exactly 1, so the rotation is now computed with those two values directly, the same multiplications and additions in the same order, giving identical bits including signed zeros. Any other roll, and JOML's fast math mode, still go through `rotateZ`.

### Main target clear

Every frame cleared the main color and depth target before rendering, and the world render cleared the same target again with the fog color before drawing anything. When a world is rendered, the first clear is skipped. Screens without a world still clear as before.

### Particle corners

For every particle, vanilla rotated the four corners of the quad by the particle rotation, four quaternion rotations per particle per frame. Camera-facing particles all carry the camera rotation, so the rotated corners are the same for each of them. The rotated corners are kept while the rotation stays bit-for-bit the same, and only the per-particle scale and offset are applied, in the same order as before, so every vertex is identical.

### Debug lines

Hitboxes and other debug lines are built with a pose that is always the identity, yet vanilla transformed every vertex position and normal through it, allocating a vector each time, and multiplied both ends of every line by the full view matrix only to read the view depth for near-plane clipping. With hitboxes shown this cost several percent of the render thread. The values are now written directly, only the depth row of the view matrix is evaluated with the same arithmetic, and an error is thrown if the pose is ever anything but the identity. When the line buffer duplicates vertices for line quads and has position, color, normal and line width, each line is written straight into the buffer in its final form (start, its copy, end) with the same encoding the buffer builder uses, leaving the end vertex for the builder to copy exactly as it would after its own calls.

### Render thread memory stack

Every LWJGL call that needs scratch memory (descriptor pushes, buffer copies, mapping) fetches the thread's `MemoryStack` from a `ThreadLocal`. The render thread's ThreadLocal table is crowded, so this lookup missed its first slot and probed on almost every call, 1.7% of the render thread in the BedWars prelobby. The render thread's stack is now captured once when Minecraft starts, and `MemoryStack.stackGet` returns it directly when called from that thread. It is the same object the ThreadLocal returns, and every other thread still uses the ThreadLocal.

### Empty render phase checks

Draining the frame's submit collections asked every feature render phase whether it was empty, and each check walked all of the phase's per-feature lists and batch maps, 0.3% of the render thread in the BedWars prelobby. Each phase now remembers whether anything was submitted since it was last cleared, which is the only way its lists can fill or empty.

### Camera fluid lookups

Every frame the camera checks whether it is in water, lava or powder snow: one fluid lookup at its block, then a fluid lookup and usually a block lookup at five points of the near plane. The near plane is a few hundredths of a block across, so these points almost always fall in the camera's own block. A point in the same block as the previous one now reuses that lookup. The points, their order and every comparison are unchanged.

### Per-frame allocations

Garbage collection pauses of 6 to 8 ms were the worst frames, and their frequency follows how fast the game allocates. `/txprofile`'s allocation sampling showed several sources in TxOptimizations' own code. Wrapped calls with many arguments boxed every float and int into an array on each call: the glyph writer and the baked block entity model swap in chunk meshing. They now use injectors that pass arguments without boxing. Each render pass gets a number, and a pipeline records the number of the last pass that checked its color targets, so the check still runs once per pipeline per pass without a set that was filled and cleared in every pass. Each pipeline now keeps its uniform names and binding slots in two small arrays, built once in the map's own order from its fixed uniform map, so binding a pipeline walks the arrays instead of every slot of the hash map (1.9% of the render thread in the BedWars prelobby). Binding a vertex buffer writes the buffer and offset straight to stack memory instead of wrapping each in a new `LongBuffer`. The driver receives the same values in every case.

### Packed gizmo lines

With hitboxes shown, every entity's box became 8 corner `Vec3`s and 12 `Line` records each frame, plus the list storage for them, about 35 MB/s in the BedWars prelobby. Gizmo groups now keep their lines as plain numbers (start and end coordinates, color and width), box edges are written from the box coordinates directly, and the line renderer reads those numbers. The coordinates are the same doubles the `Vec3`s held, in the same order. Anything that still reads the list as `Line` objects gets them built on access. The two gizmo collections that `LevelRenderer` rebuilt every frame are now cleared and refilled instead, since the previous frame's collections are dropped at that point and were already drawn, so the line arrays keep their capacity and stop being reallocated. Each line written straight into a line buffer reserved space twice, once for the copy of the previous line's end vertex that line buffers add and once for the new line. When the buffer already has room for both, it is now one reservation with the same copy and the same bytes, vertex count and last-vertex pointer; otherwise it goes through the two separate steps as before, so the buffer grows exactly as it did.

### Repeated binds

Binding a pipeline on a render pass validated its attachments, recorded a pipeline bind, forgot every uniform and then set all of them again, even when that same pipeline was already bound on the pass. Vertex and index buffers were also bound again through a native call before every draw that used the buffer already bound. Binding the pipeline, vertex buffer slice or index buffer and type that is already bound on the pass is now skipped, unless the buffer has been closed so vanilla's error still fires. Setting a uniform to a value equal to the one the pass already holds for that name is skipped too, and for textures the check runs before the texture and sampler pair is allocated (5 to 6 MB/s of allocation at a CubeCraft spawn). Vanilla sets the projection, fog, globals and lighting uniforms again before every draw, and each set marked the descriptors dirty, so the next draw pushed all of them to the driver again (`setUniform` and `pushDescriptors` together took 4% of the render thread at a BedWars spawn). The backend slot already holds that value, because each pipeline change sets every stored uniform on the new pipeline's slots, and no other code writes those slots or pushes descriptors. Vulkan keeps the bound pipeline, its pushed descriptors, push constants and buffer bindings, so every draw sees the same state as before. Setting a uniform stores it with one map write and compares the value the map held before, instead of looking it up first and then writing, and the binding slot is read from the pipeline's own slot map instead of through its read-only wrapper. These checks are written into the render pass's own uniform, vertex buffer and index buffer setters, with vanilla's checks and order unchanged, instead of cancellable hooks in front of them: a cancellable hook allocates a callback object on every call, which was 13.6 MB/s of the render thread's allocation on minemen and part of what filled the young generation every 3.5 seconds (each young collection paused for about 10 ms).

### Clears on load

Every frame cleared the world target, the depth before the held item and the depth before the GUI with separate clear commands, each followed by a barrier that drains the whole GPU, and then started a render pass on the same target that loaded the cleared contents. A clear is now held back and folded into the next render pass on that texture as a clear on load when the pass covers the whole texture, which needs neither the separate command nor the barrier. Buffer uploads and timestamps may pass a held clear because they never touch the image. Any other command, or a pass that does not cover the texture, records the held clear first exactly as before.

### Upload barriers

The Vulkan backend follows every buffer upload with a barrier that makes the whole GPU finish all earlier work before anything else may start. Several uniform uploads happen back to back each frame (global settings, projection matrices, lighting), so the GPU drained once per upload. Consecutive uploads and buffer-to-buffer copies to separate buffer ranges now share a single barrier (a copy also waits when its source or destination overlaps a range still waiting), which is recorded before the next command of any other kind, before an upload that overlaps one still waiting for its barrier, and before the command buffer ends. Timestamp queries do not trigger it.

### World text layout

Name tags, holograms and other text in the world were laid out again every frame: each character looked up its glyph, built a positioned glyph instance and updated the text bounds. Components cache their visual-order sequence, so text the server has not changed arrives as the same sequence object every frame. The laid-out text is now kept per sequence object, position, colors, shadow and outline, and drawn again from the cache. Text with obfuscated characters or with player head and atlas sprite glyphs is never cached, unused layouts are dropped after 200 frames, and everything is dropped when fonts reload or font options change. Drawing a laid-out text only reads it, so the cached copy draws exactly like a fresh one.

### Direct vertex writes

Vanilla writes every glyph, particle, entity model cube and item quad one vertex at a time through the buffer builder: each vertex checks the previous one, reserves its own space and then sets its position, color, UVs, light and normal one element at a time, and glyph, particle and item positions each go through a new vector. Without Sodium, `putBakedQuad` alone took 5.6% of the render thread in an entity-heavy benchmark scene and glyphs 2.3% in the busiest one. When the vertex buffer uses exactly the format being written, TxOptimizations reserves the space for the whole glyph, text background or underline, cube, item quad or particle layer at once and writes the same bytes straight into it. The reservation makes the same checks the first vertex would (the builder is still building, the previous vertex is complete, the buffer is not a line buffer that duplicates vertices, the vertex limit is not reached), updates the vertex count and last vertex pointer the way the builder does, and is only taken when the vertices fit into the memory the builder already has, so the buffer grows at exactly the same points as before; otherwise the vanilla path runs. Item and block model quads are written in vanilla's entity and block formats on any thread, world text in vanilla's lit text format, and see-through text (name tags and text displays seen through walls) and GUI text in vanilla's position, UV and color format, where the light is dropped as vanilla drops it for that format. Positions go through the same JOML matrix call vanilla makes. A comparison against vanilla's builder over random matrices, quads, glyphs, particle layers and buffers too small to hold them matched every byte, vertex count, last vertex pointer and buffer capacity (NaN positions compare as NaN, since the JIT may produce either NaN sign in vanilla too). Buffers with any other format, subclasses of the buffer builder, and cubes drawn off the render thread keep vanilla's per-vertex path.
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

### Resource pack zips

Zip resource packs, including the ones servers send, are read through an index of the zip's entry names built once per pack and kept sorted, so listing a folder (all textures, all models) is a binary search for its range instead of a scan of every entry in the zip, and looking up a single file asks the zip for that entry directly. Overlay folders are searched in the same order as vanilla, and a pack whose zip cannot be opened is logged and treated as empty, as vanilla does.

### Server switches

While a server reconfigures the connection (the screen shown when a network moves you between servers), frames are not rendered for up to 5 seconds, so the game does not spend the GPU and CPU on that screen. Rendering resumes as soon as the world loads, an overlay such as a resource reload appears, or the 5 seconds run out.

### Loading screens

The resource reload overlay is not drawn: while a reload runs you keep seeing the screen behind it (the title screen or the current menu, or the world in game), and as soon as the reload finishes it is completed and the overlay removed, without the fade-out. The world loading screen is not drawn once a world exists. On the first world load after starting the game, the window keeps showing the last frame while that screen is up instead of new ones: frames are still rendered, so the chunks around you keep building and the screen closes when vanilla's would, but they are not copied to the window or presented. Without that, the first join showed black frames, because the world had nothing to draw before your position and chunks arrived. Once a world frame has been shown, later joins and dimension changes present every frame as before. The title screen skips its fade-in. These change what is shown while loading, not how long the loading itself takes.

### Frame limiter

With a frame limit set, the limiter waits until the next frame is due by parking the thread until 0.5 ms before it and then spinning, so frames are paced to the exact interval instead of whatever the OS sleep granularity allows. Frames that fall more than one interval behind restart the schedule instead of catching up. At the unlimited setting the limiter is not used.

### FPS counter

TxOptimizations draws its own FPS counter in the top left corner (`123 FPS`, white, no shadow, at 2,2), hidden while the F3 screen is open or the HUD is hidden. `K` toggles it (rebindable under Options > Controls > TxOptimizations), and the choice is saved in `config/txoptimizations.json`. The number is the average frame rate over the last 0.5 seconds and updates every 0.5 seconds. It only counts frames, and the text is rebuilt only when the number changes, so drawing it skips translating and laying out the text again every frame.

### Baked block entities

Chests, ender chests, shulker boxes, bells, skulls, banner poles, decorated pots, copper golem statues and cushions are baked into the chunk mesh like normal blocks instead of being drawn again every frame. They switch back to per-frame rendering only while they move: a chest or shulker box while its lid is open, a bell while it swings, a decorated pot while it wobbles, an animated dragon or piglin head. Banner flags keep waving because only the pole and bar are baked. Player heads and decorated pots with sherds stay per-frame, and a named cushion only draws its name tag per frame. In a server lobby profile this cut block entity rendering from about 4.1% to 0.35% of the render thread.

Block entities with nothing to draw are skipped: signs without text, beacons without a beam, campfires and shelves without items and lecterns without a book. The answer is remembered on each block entity for the current client tick, so a sign that gains text shows it at most one tick later. Sign text facing away from the camera is not drawn.

### Atlases and batching

- Font atlas: font glyph textures are 1024x1024 instead of 256x256, so text spans far fewer textures and needs fewer texture switches and draw calls.
- Map atlas: map images are packed into shared 2048x2048 textures, so walls of maps in item frames draw in a few calls instead of one per map.
- Glyph vertex builder: consecutive glyphs of the same render type reuse the same vertex builder instead of looking it up for every glyph.
- Empty enchantment iteration: running an enchantment effect over an item with no enchantments wrapped the empty enchantment map in an unmodifiable set and two iterators before finding nothing to visit. Humanoid renderers work out a crossbow charge time this way for every humanoid and armor stand every frame, even with nothing in hand (0.8% of the render thread with 334 armor stands). An item with no enchantments now loops over a shared empty set, which visits the same nothing without allocating.
- Gizmo collector: every gizmo looked up the thread's gizmo collector in a `ThreadLocal`, which kept missing its first slot (up to 0.8% of the render thread with hitboxes shown). The collector set by the most recent `withCollector` or restore is kept together with the thread that set it, and a lookup from that thread returns it directly; any other thread reads the `ThreadLocal` as before.
- Leash holder: drawing an entity that can be leashed and has leash data looked up `level.getEntity` with the delayed holder id on every visibility check and render state, even when that id was 0 and the result was never used (up to 0.8% of the render thread with many leashable mobs). The render side now skips that lookup when there is no delayed holder and otherwise does the same lookup and hand-over as vanilla. Nothing else overrides `getLeashHolder`.
- Item vertex builder: within one item, consecutive quads of the same render type reuse the vertex builder the previous quad got, when that render type merges consecutive geometry, which is exactly the builder vanilla's lookup returns again (the item quad loop was 3% of the render thread with 334 armor stands at a CubeCraft lobby). Whether the render type merges consecutive geometry is read once when its builder is looked up instead of for every quad (0.2% of the render thread without Sodium); it only depends on the pipeline's primitive topology, which never changes.
- Animated item atlas: animated item icons in the GUI share one atlas that is cleared once per frame instead of slot by slot.
- Scissor state equality: vanilla merges draws of the same prepared render type inside groups it marks as reorderable, but `RenderType.prepare` copies the scissor state every time and `ScissorState` has no value equality, so two prepared render types never compare equal and the merge almost never happens. Comparing scissor states by value lets that vanilla merge work, which cuts draw calls. Strictly ordered groups are untouched.

When a loaded resource pack overrides the `core/text` shader, the font atlas stays at 256x256 unless the pack lists `font_atlas_resizing` under `compatible_features` in an `immediatelyfast` metadata section, and the map atlas is switched off when the pack lists `map_atlas_generation` under `incompatible_features`. Server resource packs with custom text shaders therefore look the same as in vanilla.

### HUD cache

The HUD is drawn into an offscreen framebuffer at up to 60 frames per second (20 while a screen is open), and every frame only blends that cached image over the world. Each HUD frame is spread over 3 render frames: HUD layers are split into 3 groups of roughly equal measured cost, and each frame redraws one group, so no single frame pays for the whole HUD. Animations inside cached layers advance by the summed frame time of the cycle. Layers whose blending reads the color underneath them, like the crosshair, switch to being drawn every frame the first time they appear, and so does anything drawn outside a registered HUD layer, including the FPS counter. Resizing the window, opening or closing a screen or hiding the HUD redraws the HUD directly for two cached frames so nothing shows stale. The cached image is blended over the world only inside up to 8 non-overlapping rectangles covering the bounds of everything drawn into it, padded by 2 GUI pixels, instead of over the whole screen, so the GPU blends the areas around the hotbar, chat, scoreboard and other HUD parts instead of every pixel. Vanilla drops GUI elements without bounds and orders overlapping elements by them, so every drawn element reports them.

A HUD layer that adds a picture-in-picture element, such as Xaero's Minimap, is not cached: the element renders into its own texture while it is prepared, which the cached passes spread over several frames could not do every frame, so the minimap flickered or was not drawn. The whole layer is drawn every frame in the normal GUI pass, in its original order, as without the cache. Xaero's Minimap also hooks the start and end of every GUI render to do its per-frame work against the main framebuffer; while the HUD cache renders its own offscreen frame those hooks are skipped, so they run once per frame in the normal GUI pass as they do without the cache.

### Rain culling

Rain and snow are built column by column around the camera every frame. Columns whose vertical span lies outside the camera's view are skipped. Each column draws with its own position-seeded randomness, so the visible rain looks exactly the same.

For every column that is drawn, vanilla also asked the level for the precipitation type at that block, which looks up the biome through the biome zoom noise and the biome temperature. In rain a spark profile showed this at 20% of the render thread. The answer only depends on the chunk being loaded and its biome data, so it is cached per block position and the cache is dropped whenever a chunk is loaded, unloaded or gets new biomes, and when the level changes. The column bounds come from the values vanilla already computed instead of a second heightmap lookup.

### Entity culling

Entities and block entities hidden behind solid blocks are not extracted or drawn. Every 5 ticks the client tick records the culling box and position of every loaded entity, plus every block entity within 64 blocks of the camera whose renderer isn't drawn off screen. A background thread then traces rays from the camera to sample points on the camera-facing faces of each box, through blocks whose state is a solid render cube, and marks it culled when no ray gets through. This runs again every tick the camera moves or a new capture arrives. Ray results per block are cached for the pass, and only the touched cache entries are cleared before the next one.

Entities farther than 128 blocks, glowing entities, all players, entities whose renderer ignores culling and boxes larger than 50 blocks are never culled, and anything found visible stays visible for at least one second. Players are never culled because the capture only runs every 5 ticks: a player stepping out from behind a wall would otherwise stay hidden until the next capture and then appear at once. Culled entities that show their name still draw the name tag and the below-name score through walls, at the same distances vanilla uses. Block entities outside the camera's view are skipped as well, without allocating a box.

Entities that are culled, or were not extracted in any frame since the previous tick, only run a basic tick on the client: vanilla's common tick (old position and rotation, invulnerability timer, position interpolation, tick count), `aiStep`, hurt time and the warden heartbeat. A tick with no frame since the previous one, below 20 FPS, never counts entities as out of camera. Players, the camera entity, vehicles, boats, minecarts, display entities and firework rockets always tick fully.

### Persistent buffer mapping

Every write into a CPU-visible GPU buffer (dynamic transforms, uniforms, fog, clouds, vertex staging) mapped the buffer's memory with `vmaMapMemory` and unmapped it again when the view closed, two native calls per write. A spark profile showed these calls at 2.8% of the render thread. Each buffer is now mapped the first time it is written and stays mapped until it is destroyed. Every mappable buffer is allocated host-visible and host-coherent, so the GPU sees the same bytes at the same addresses without any flush, and vanilla's check that a buffer is not closed while a view is open still works.

### Texel buffer views

Pushing descriptors that use a texel buffer (the cloud buffer) created a Vulkan buffer view on every push and queued it for destruction two frames later. The view only depends on the buffer, offset, size and format, so it is now kept on the buffer (up to 8 per buffer) and destroyed together with the buffer, after the GPU is done with it. Whether the view came from the buffer is kept on the render pass between creating the view and queueing it for destruction, instead of in a holder object allocated on every descriptor push.

### Pipeline switches

Switching pipelines inside a render pass re-applied every uniform ever set in that pass through a hash map pass, and checked the pipeline's color formats against the pass attachments again. Only the new pipeline's own uniforms are now applied, which gives the backend the same values, and the format check runs once per pipeline per render pass.

### Present thread

The frame's queue submit and its present are native driver calls that took a third of the render thread in the prelobby. Both now run in the same order on one dedicated thread, while the render thread already moves on to the next frame. With vsync off, the next frame acquires its screen image just before it copies to the screen instead of at the start, after the present thread has finished, so the render thread builds the whole frame while the previous one is being presented. Every other queue or swapchain call waits for the present thread first, and the same image is presented in the same order. With vsync on, the image is still acquired at the start of the frame, so input latency is unchanged. With vsync off the swapchain also gets one more image than vanilla asks for, when the driver allows it. In immediate mode a finished image is shown as soon as it is presented, so the extra image adds no latency; it only means the late acquire does not have to wait for the presentation engine to release an image while the GPU is the bottleneck.


The present thread only wins while the CPU is the bottleneck. With the GPU as the bottleneck, NVIDIA's driver blocks in the late acquire and the GPU loses work, so the frame then runs vanilla's order on the render thread instead: acquire at the start of the frame, then submit and present right after the frame is recorded. Both orders draw the same image. Which one is used is decided from what each order measures about itself over every 128 frames, without ever running the other order on trial: in vanilla's order, a render thread that waits for the GPU less than 10% of the frame time is CPU-bound, so the present thread takes over; with the present thread, an acquire that blocks more than 8% of the frame time means the GPU is the bottleneck, so vanilla's order takes over. In the measured scenes these values are 1.7% against 40% and 1.0% against 19.5%, far from both limits. `/txprofile` reports the share of frames that used the present thread.
### Submit collections

The hand and screen effect passes drained their submit collections twice per frame and threw away the empty ones, so the next frame built them again with all their phases. Empty collections are now kept and reused; an empty phase draws nothing.

### GUI item atlas sweep

At the end of every frame the GUI item atlas walked all its slots looking for slots marked to be dropped after the frame. The sweep now only runs when a slot was marked since the last sweep.

### Viewport, scissor and pipeline binds

Every render pass set the viewport and scissor and bound its first pipeline again, even when the command buffer already had exactly that state from the previous pass. The bound pipeline and both dynamic states stay valid across render passes in the same command buffer, so a viewport, scissor or pipeline that is already set is no longer recorded again. `vkCmdBindPipeline` alone was 2.8% of the render thread in the BedWars prelobby. Pushed descriptors also stay valid across render passes, so the check that skips pushing an identical descriptor set with the same pipeline layout now spans the whole command buffer instead of a single pass. The remembered state is dropped whenever a new command buffer starts recording.

### Particle vertices

Particles were turned into vertices one at a time: a callback and a new rotation per particle, a new vector for every corner, and a separate vertex reservation for each vertex. Each particle layer is now written in one pass straight from the stored particle arrays into one reservation, with the same corner math, colors, UVs and light, so the vertex data is identical. Each vertex position is computed in local floats, the same multiply then add JOML's `mul` and `add` do (Java never fuses them), instead of going through a shared vector object's fields (3.4 to 3.8% of the render thread with 750 particles at a CubeCraft spawn, inside particle preparation). A layer is written this way only when the buffer can reserve it exactly as described under direct vertex writes; buffers that cannot, including one that would have to grow for the layer, still go through the vanilla per-particle path.

### Model part rotation

Every rotated model part (entity limbs, heads, armor) built a new quaternion from its three angles and rotated both its pose matrix and its normal matrix by it, and each of the two rotations worked out the same nine rotation terms from the quaternion again. The quaternion is now computed in local values with JOML's own formula, its rotation terms are worked out once, and both matrices are rotated by them with JOML's own formulas, including its separate paths for identity, translation-only, affine and general matrices and the matrix property flags it sets. Most rotated parts turn around only one or two axes, so a half angle of exactly zero now skips its sine and cosine: JOML returns that same zero and exactly 1 for it, so the matrices are identical. JOML's fast math mode still computes them. A test of 3,000,000 random poses and angles, including signed zeros, infinities and NaN, against vanilla's rotation with JOML 1.10.9 matched every value and property flag.

### Item model bounds

Items on shelves and dropped items asked for their model's bounding box every frame, and because their render state is cleared or recreated every frame, vanilla rebuilt the box from every vertex of the model each time (about 1% of the render thread at a server spawn full of shelves). The box only depends on the model's extents, its item transform, the hand and its local transform, so single-layer items now keep it in a cache keyed by exactly those values. Items with Fabric mesh quads, which add their own vertices to the box, and multi-layer items still compute it every time.

### Raycast air

Every block a raycast passes through (the per-frame crosshair pick, the third-person camera collision) asked the block for its shape. Air never has a shape in any raycast mode, so air blocks now return the empty shape directly.

### Particle collision

Every particle with physics checked its movement against the blocks around it each tick by collecting every block collision shape in the swept box into a new list (up to 0.8% of the render thread at a server spawn full of dust particles). Particles have no entity colliders and no world border, so when every block in that box is air, vanilla returns the movement unchanged. The box is now first checked directly in the chunk sections, skipping all-air sections whole, and only boxes that contain any non-air block go through vanilla's collision.

### Ambient block ticks

Every client tick, 1334 random blocks around the player get their ambient effects (campfire smoke, dripping water, ambient particles), and each one was looked up twice, once for the block and once for its fluid. The fluid is part of the block state, so it is now read from the block state already looked up. The debug world, where block lookups do not come from the chunk sections, keeps the second lookup.

### Translucent sort reuse

Translucent entity, item, text and particle quads are sorted back to front before every upload. Vanilla works out a distance for every quad and sorts the quad indices with fastutil's merge sort, which calls a comparator lambda for every comparison and keeps equal distances in index order; that comparator call site is shared by every merge sort in the game, so it is not inlined, and without Sodium the sort took 3.1% of the render thread in the busiest benchmark scene and 3.8% in the particle scene. Each sort's result is now remembered by its position among the frame's sorts, and the next frame first checks in one pass whether the remembered order is still sorted under the new distances (ties ordered by quad index). If it is, it is exactly the order the stable sort would produce, so it is used without sorting. Otherwise every distance becomes an integer key that orders exactly like `Float.compare` (negative zero below zero, every NaN equal and above infinity), each key is packed with its quad index into one long, and up to 80 quads are sorted with an in-place quicksort that switches to heapsort if it recurses too deep and finishes small ranges with insertion sort, and larger counts with a radix sort that skips passes where every key lands in the same bucket. The index breaks every tie, so both give vanilla's order. None of this allocates in steady state: the quad centers are decoded into an array kept per quad count and zeroed before each use, the sort keys go into one reused array, and each remembered order is rewritten in place. The index writer reads the remembered order directly, and only callers outside the upload path get their own copy. Translucent terrain, which is sorted on the section build threads, packs the same keys straight into one array and sorts it with the same in-place quicksort, and fewer than 16 quads are ordered by a stable insertion sort on the keys, so those sorts never allocate more than vanilla's did. A test of 141,548 sorts against vanilla's own sorters over 3,000 simulated frames, with repeated and moved quads, ties, signed zeros, infinities, NaN with different payloads and up to 4,000 quads, matched vanilla's order every time through both the remembered and the fresh path.
### Transient command buffer

Every frame started a command buffer for copies into transient GPU memory and submitted it first, even though nothing in the game records such copies in a normal frame. That buffer is now only started when the first copy is recorded, and it is then placed first in the frame's submission, exactly where it would have been. Frames without such copies submit one command buffer fewer.

### Continued render passes

Ending a render pass recorded the end of the rendering instance and a full barrier right away, and the next pass began a new instance, even when it drew into exactly the same color and depth textures. The end of a pass is now held back. If the next pass uses the same attachments and render area, clears nothing and nothing else was recorded in between, it keeps drawing in the same rendering instance, as happens from the sky pass into the main world pass in clear weather. Draws inside one rendering instance keep their order for color, depth and blending, and a pass cannot sample its own attachments, so the image is the same. Anything else that records a command, ends the command buffer, or begins a different pass ends the held pass first, exactly as before. The GPU pass profiler disables this while it records.

### Long selection lists

Filling a scrolling list (server list, world list, resource packs) placed each new row by summing the heights of every row already in the list, so filling n rows took n² steps: about 7.3 million for a 3,817-entry server list, an 88 ms freeze when the multiplayer screen opened. While a list is being refilled, each row's position is now carried over from the previous row by adding that row's height, the same value vanilla's sum gives.

### Server icons

Every server list ping decoded the server's icon after removing newlines from its base64 text with `String.replaceAll("
", "")`, which compiles and runs a regular expression over text that is often tens of thousands of characters long. Pinging the 4,109 servers of a large list spent 12.5% of all CPU time there. The newlines are now removed with a plain character replacement, and text without a newline is used as it is, which gives the same string.

### Frustum box test

Every entity, block entity and rain column visibility check asked JOML for a full box/frustum classification (outside, inside or intersecting), which tests each plane twice, and then only used whether the box was outside. The check now uses JOML's plain box test, which runs exactly the first of those two tests per plane in the same order, so the answer is identical with half the plane math.

### Dynamic transform equality

Each draw's model-view, color, offset and texture matrix are compared with the previously written ones to reuse the uniform slice. That record's generated `equals` went through method-handle dispatch on every comparison (0.2 to 0.4% of the render thread). It is now a plain comparison of the same four components with `Objects.equals`, the cheap vectors first, which returns the same answer.

### Identity model parts

Every visible model part pushed a copy of the pose stack, applied its offset, rotation and scale, drew, and popped the copy again. For parts with no offset, no rotation and unit scale (roots, hats, jackets, sleeves and other overlay layers) that only copied two matrices and translated the copy by zero. Such parts now draw and recurse into their children on the current pose directly; parts with any transform still push, transform and pop. Translating a finite matrix by zero leaves every value as it was, apart from turning a negative zero translation into a positive zero, which does not change any pixel.

### Model pose reset

Every model reset its parts before animating by walking an immutable list through its iterator. The parts are now copied into an array when the model is built and reset in a plain loop, with the same parts in the same order.

### Bare armor stands

Before drawing its layers, every armor stand set up its full pose and then asked the armor, held item, wings and head layers to draw, even when it wore and held nothing, which is what servers use for floating text. When the renderer has exactly the vanilla layers and the stand has no equipment, no held items and no head, that pose setup and the four empty layer calls are skipped. The stand's own model still animates when it is drawn.

### Cuboid normals

Every entity model cube transformed, normalized and packed the normal of each of its faces every time it was drawn, and every item quad did the same for its face. The render thread now keeps the packed normal of each of the six face directions for the current normal matrix, and drops them as soon as any of the matrix's nine values or its trusted-normals flag differs bit for bit, so every cube face and item quad gets exactly the bytes vanilla's transform would produce. Opposite faces use exactly the negated direction, and vanilla's normal packing is symmetric around zero, so a direction whose opposite is already known takes its bytes negated instead of being transformed again, which gives the same bytes (checked on 9 million random direction pairs and matrices including NaN, infinities and signed zeros). Faces whose normal is not one of the six direction vectors are transformed as before, and so is anything that goes through vanilla's per-vertex path.

### Collision cursor

Block collision scans (particles moving near blocks, the player's suffocation check) walked their cells by splitting a running index with two divisions and two remainders per cell. The cursor now steps x, then y, then z with counters, which visits the same cells in the same order (checked against the division form on every box up to 12 by 12 by 12).

### Render type equality

Batching draws compares prepared render types with their generated record equality, which went through method-handle dispatch and compared the name string before anything else. It now compares the same six components with `Objects.equals`, cheapest first (pipeline, transform slice, scissor, OIT set, name, textures), which returns the same answer. Their hash code, which hashed the whole texture list on every map lookup, is worked out once per prepared render type and kept: every component is immutable or hashes by identity, so it never changes.

### Vertex upload copy

Every frame's entity, text, item, GUI and debug geometry used to be written into a CPU-side staging buffer, then copied by the GPU into the real vertex and index buffers, with a barrier around each copy. That was 5 copies per frame and 0.018 ms of GPU time in a GPU-bound world. The vertex and index buffers are now allocated mappable, and the vertices and sorted indices are written straight into them, so both copies and the staging buffer are gone. Buffers only return to the pool after the GPU has finished with them, so nothing in use is overwritten. Each slice is copied straight from the vertex builder's memory with the same validity and overflow checks, and the GPU receives identical bytes.

### Uniform writes

Every entity, text or item draw with its own transform wrote that transform into a ring buffer through a fresh mapped view of the buffer. Each write allocated a pointer buffer, a byte buffer, a buffer slice, a mapped view and its close callback (over 16 MB/s of garbage in a busy server lobby). Under Vulkan each ring buffer now keeps one mapped view, opened the first time it is written and closed with the ring buffer, and every transform is written through it. Each transform is written at the same offset with the same limit, so the GPU reads identical bytes. The memory is host-coherent, as it was before, so no flush is involved. Other backends keep mapping per write.


When a uniform write equals the previous one, the previous block is reused. That reuse now also returns the slice object made for that block instead of a new one, as long as it still points at the same block of the same buffer, which is what a new slice would describe (3.8 MB/s in the BedWars prelobby).
### Model cube writes

Every model part drew its children by iterating its child map, which created an iterator object per model part per frame (5.2 MB/s in a world with many slimes). Child maps are fastutil array maps, so the children are now read in the same order straight from the map's value array. Any other kind of map keeps the iterator. A part's cubes are walked by index in the same order instead of through the cube list's iterator.

### Entity culling boxes

Every entity checked against the view frustum got a new bounding box, grown by half a block, every frame (about 11 MB/s in singleplayer). The check now works out the six coordinates of the grown box from the culling box with the same arithmetic vanilla's box constructor uses, runs vanilla's invalid and zero size tests on them and passes them straight to the frustum test, so neither a still nor a moving entity creates a grown box, and no box is kept per entity. A box is only created where vanilla needs one beyond that test: the four block box around an entity whose culling box is invalid, and the box spanning a leashed entity and its holder when neither is visible on its own.

### Particle draw maps

Every particle group built two new identity maps per frame to collect its draws and textures per layer. The maps are now cleared, when they hold anything, and reused once the frame's particles are drawn. A cleared map keeps its table size, so it lists its layers in the same order a new one would; a map that ever held enough layers to grow is dropped instead of reused.

### GUI glyph matrix

Every GUI glyph converted its 2D pose into a new 4x4 matrix before writing its vertices. One matrix is now reset to identity and reused for each glyph, and the glyph writes its vertices before the next one starts.

### Buffer slices

Asking a GPU buffer for a slice covering all of it created a new slice object each time, and vanilla does that for every entity, item, text, GUI and particle draw each frame. The slice is now created once per buffer, since a buffer never changes size.

### Feature phase loop

Each frame, the translucent pass walked every feature phase of every submit collection, and each phase with nothing to draw built an iterator over an empty list (about 8 MB/s in a BedWars game). Phases with no groups now return straight away, and the rest are walked by index in the same order.

### Projection uploads

The GUI, the item atlas and picture-in-picture renderers upload their projection matrix to the GPU with a buffer copy every frame, because their projection's version number changes each time it is set up, even when the matrix stays the same. Each projection buffer now remembers the matrix it last uploaded and skips the copy when the new matrix is bit-for-bit the same, so the buffer already holds exactly those bytes.

### Suspicious sand and gravel

Every suspicious sand and gravel block in view built a full render state each frame, including an item render state with its own matrices (about 45 MB/s of garbage on a BedWars map), although it only draws its buried item while it is being brushed. Blocks that have not been brushed at all now skip the render state, since vanilla's renderer draws nothing for them.

### Block entity distance

Every block entity in view allocated a vector for its center to check whether it is within the renderer's view distance (about 6 MB/s on a BedWars map). Renderers that use the default distance check now compute the same differences and squares directly from the block position, in the same order, so the result is identical. Renderers with their own check, such as beacons, still run it.

### Render type textures

Every time an entity, item or text render type was prepared for a frame, it built a new list of its textures (overlay, lightmap and each bound texture with its sampler), allocating the list and one record per texture. Each render type now keeps the list it built last and returns it again when every entry would be the same: same name, same texture view object and same sampler object, looked up the same way and in the same order as before. Any change, such as a reloaded texture or a different lightmap, builds a new list. Checking whether the list is still current walked the render type's texture map, a hash table of 16 slots for one or two textures (0.84% of the render thread at a CubeCraft spawn). A render setup's texture map is complete once the setup is built, since vanilla only adds textures through its builder before building, so the names and bindings are read into two arrays once, in the map's own order, and the check walks those. The texture manager counts every change to its texture map (registering, releasing or closing textures), and each render setup keeps the textures it looked up until that count changes, so the check no longer looks every texture up by name on each draw; each texture's current view and sampler are still read every time. The list keeps its hash code, the same value a list of those textures always has, so hashing a prepared render type no longer walks the textures (1.24% of the render thread in slow frames). Drawing a render type binds its textures with an indexed loop instead of a new iterator per draw (2.3 MB/s of allocation).

### Sound channel wait

Every sound the game plays asks the sound thread for an audio channel and waits for the answer on the render thread. The wait parked the render thread, so on top of the sound thread's own wake-up it also had to be woken by the operating system again, 0.3 to 0.5 ms per sound in `/txprofile`. The render thread now spins until the channel arrives and then takes it the same way, so the same channel is used at the same point, without the second wake-up.

### Light snapshots

The light engine keeps one map of light sections it updates and publishes a read-only snapshot of it for every other thread. Every time lighting changed, vanilla published a full copy of the whole map. At render distance 32 that map holds around 100,000 sections, so each copy allocated arrays of several MB on the render thread and on every server worker thread. While moving, `/txprofile` showed most garbage collections forced by these huge arrays, each pausing the game for 8 to 10 ms.

Snapshots are now stored as a tree that each new snapshot shares with the one before it. A chunk column keeps its sections in an array indexed by height, columns are grouped into regions of 16 by 16 chunks, and a small hash map holds the regions. A new snapshot copies that small map, then copies and updates only the regions and columns that contain a section the light engine recorded as changed since the previous snapshot, with that section's current data. Everything else is shared, and nothing in a published snapshot is ever modified, so a thread still reading an older snapshot keeps seeing it unchanged. Every change to the map the light engine updates is recorded that way, so each snapshot holds exactly what a full copy would. There are no full copies anymore, not even periodic ones.

Sky light snapshots also keep each column's top section in the tree. Vanilla copied that whole map of top sections, one entry per loaded chunk column, into every sky light snapshot; it changes only together with a section of the same column, so the tree updates it for the same columns. A lookup now finds the region in the small map and reads the column and the section from arrays, instead of searching the full section map. Vanilla never stores a light section outside the world's height plus one section above and below, and storing one now throws an error instead of being kept silently.

### Sky light lookups

Reading the sky light at a block looked up the column's top section in the snapshot's map of top sections, then the block's own section in the section map, and one more section for every step when it had to walk upward past sections without light data. Chunk meshing reads it for every block it lights, together with the block light. All of these now come from one region of the light snapshot tree: a single lookup in the small region map, then the column's top section and its sections are read from arrays, including the walk upward. The checks run in vanilla's order and return the same light level. Light storages also start with an empty tree snapshot instead of an empty vanilla copy, so every published snapshot is a tree.

### Light writes

Every light level the light engine writes while spreading light (the blocks around a placed torch, or the sky and block light of a newly generated chunk in singleplayer) did two hash set insertions: one marking its light section as changed since the last snapshot, and one marking the section for a mesh rebuild, plus the neighboring sections when the block sits on the section's edge. Light spreads to a block's six neighbors one after another, so consecutive writes are often in the same section. Both insertions are now skipped when the section is the one the previous write already inserted. A block on a section's edge still inserts all of its neighboring sections every time. A section only leaves these sets when the light engine publishes a snapshot and empties them, which also forgets the remembered section, so the sets end up with the same contents in the same order. The neighboring sections are also inserted directly instead of through a new callback object per write.

### Queued light data

Light data from the server (or from disk in singleplayer) waits in a queue until its section is part of the light engine. Every light update that follows new light data or a loaded or unloaded section walked the whole queue to find the sections that became ready. The queue is a synchronized wrapper around a hash map, so the walk went through the wrapper's general iterator, which creates a new entry object for every queued section. The walk now uses the wrapped map's own fast iterator, which reuses one entry object. Both iterators visit the same entries in the same order and remove them the same way, and vanilla's walk did not hold the wrapper's lock either.

### Entity hitboxes

With hitboxes shown (F3+B), every visible entity built its boxes and its view arrow from temporary vectors every frame: an offset vector, the moved bounding box computed twice for living entities, a new stroke style per box, and a scaled view vector. Each arrow then built a rotation, five vectors and an array to draw its four tips. The hitbox boxes now share one immutable stroke style per color, the moved bounding box is computed once from the offset's components, and the arrow end is computed from the view vector's components, all with the same arithmetic as before. Arrows rotate their tips with one reused rotation and write the lines straight into the packed line list. The rotation's terms and its one division are worked out once per arrow instead of once per tip, and each tip is rotated with the same fused multiply-adds JOML uses, so the GPU receives identical lines.

### Unused region cache

Every frame, vanilla's level extraction created a new region cache with its own hash map for the visible sections that need rebuilding, although most frames have none. The cache is now created when the first section of the frame asks it for a region, and that one cache serves every later section of the same frame, so every section gets the same region as before.

### GUI texture setups

Every GUI glyph described its textures with a new texture setup record: its glyph texture and sampler plus the lightmap and its sampler. Text in the HUD and menus uses the same few textures over and over, so the last setup is now kept and returned again when the glyph texture, its sampler, the lightmap and the lightmap sampler are all the same objects. The record is immutable, so callers get an equal value either way.

### Default texture matrix

Every entity, item and text render type prepared for a frame asked its texture transform for a texture matrix, and the default transform built a new identity matrix each time (about 12 MB/s of garbage in a BedWars game). The matrix is only read, when the transform is written into the uniform buffer, and vanilla already shares one identity texture matrix for its other transform writes. Render types with the default transform now pass one shared identity matrix, so the same bytes reach the GPU. Glint and offset transforms still build theirs.

### Item frame offset

Every item frame built a new render offset vector twice per frame, once when its entity was placed and once when the frame was drawn (about 5 MB/s of garbage at a spawn full of map frames). The offset only depends on the direction the frame faces, so the six offsets are now built once with the same arithmetic and shared. A vector cannot be changed, so every caller reads the same values.

### Breaking overlay rotation

The block breaking crack overlay computes its texture coordinates per vertex and called `Direction.getRotation()` for every vertex, which allocates a new quaternion and evaluates its sines and cosines (1.3% of the render thread in a CubeCraft lobby). The six rotations are now built once by that same method and reused. The overlay only reads them, so every vertex gets the same values.

### Entity render types

Every model asked its render type function for the render type of its texture once per entity per frame, which looks it up in a concurrent memo map keyed by a `Pair` (0.7% of the render thread with many mobs on screen). Each model now remembers the last texture and the render type it got for it, in one immutable pair, and returns that render type again while the texture is the same object. Vanilla's model render type functions are memoized, so the same texture always gives the same render type.

### Draw group lookup

When an entity asks for a vertex builder, its prepared render type is searched in the current draw group with `List.indexOf`, comparing it with every draw so far (1.5% of the render thread with many different mob textures). Each group now keeps a hash index from prepared render type to its first position, filled when vanilla adds it, so the lookup finds the same draw directly. Groups with up to 8 draws keep vanilla's `indexOf`, which is a few cheap comparisons; a group that grows past that gets a primitive int map built from its list in order, so the first position still wins. The calls are redirected without a wrapper, so adding a draw no longer allocates a boxed position, a map node, an argument array or, for the many small groups, any index at all (an index made for every group was 8.4 MB/s at the LifestealSMP spawn).

### Sky behind terrain

Vanilla draws the sky disc, sunrise glow, sun, moon and stars over the whole screen before any terrain, and the opaque terrain then paints over most of it. In a server lobby these sky passes took 0.019 ms of a 0.131 ms GPU frame. In the overworld with the default transparency, the sky is now drawn right after opaque and cutout terrain and before everything else, with copies of the vanilla sky pipelines whose vertices are placed at the far plane and depth tested against the terrain, so only pixels without terrain are shaded. Terrain does not blend and always writes depth, entities, particles, text, outlines and translucent terrain are still drawn after the sky, and the sky layers keep their order and blending, so the image is the same. The vanilla sky shaders get the far plane line only under a define that just these copies set; if a resource pack replaces one of them and the expected line is missing, the sky is drawn the vanilla way. With Iris installed the sky is also drawn the vanilla way, because Iris closes and replaces the main render pass at the point where the deferred sky continues it.

### Shader cache

Every resource reload, including the one when a server sends its resource pack and the one when you leave that server, compiled the shaders of all 385 render pipelines from GLSL to SPIR-V again (the shader loader took 1.7 s of worker time in a CubeCraft reload, including creating the Vulkan pipelines). Each compiled shader is now saved to `txoptimizations/spirv` under a SHA-256 of everything the compiler sees: the shader name, stage, source, defines, every shader include, the compiler flags for the GPU, and the Minecraft and LWJGL versions. The next reload with the same inputs reads the file instead of compiling, so the result is byte for byte what the compiler would produce, and any change to a shader or include compiles it again. Nothing is kept in memory.

### Font provider selection

When a reload finished, the render thread went through every codepoint of every font (more than 60,000 for the unifont alone) in every glyph provider to pick the providers each font uses and to list the glyphs of each width for obfuscated text. That took 72 ms of the 143 ms the render thread was blocked in a CubeCraft resource pack reload. The same work now runs on a worker thread while the reload is still preparing, with the codepoints in the same order, and the render thread only takes the finished result. Instead of asking every provider in turn for every codepoint, it first goes through each provider's list of supported codepoints once and notes the first provider that has each codepoint; bitmap, Unifont, space and TrueType providers return a glyph exactly for the codepoints they list, so that is the provider the old loop stopped at. Fonts with any other kind of provider are worked out the old way.

Before that, vanilla font loading asked every provider of every font for the glyph of every supported codepoint and threw the answers away, so that TrueType fonts load and keep their glyph metrics. Bitmap, Unifont, space and the missing-glyph fallback providers only look the glyph up, so for a font made only of those the loop has no effect, and it took 180 ms of worker time in a CubeCraft reload; it is now skipped for such fonts and runs as before for any font with another kind of provider.

When a resource pack turns font atlas resizing on or off, as CubeCraft's pack does on every join, all fonts are rebuilt so their glyph textures start over, and each font worked out its providers again with the same providers and options, a 138 ms render thread freeze on joining CubeCraft. Each font now keeps the providers and glyph widths it selected last, and reuses them when it is asked again for the same provider list and the same options; anything else is selected as before. It is used only when the font options and every provider are still the same when the reload is applied, otherwise the render thread works it out as before.

### Audio device on reload

Every resource reload closed the OpenAL audio device and opened it again, which took 52 ms of the 101 ms the render thread was blocked in a CubeCraft resource pack reload. The reload still stops every sound, frees every sound buffer and channel and loads the sounds again, but the device and its context stay open when the reopen would pick the same device: the sound device option, the directional audio option and the list of system audio devices are unchanged and the device is still connected. If any of them changed, the device is closed and opened as before.

### Shaders compiled on first use

A reload compiles every pipeline Minecraft registers while it prepares, but pipelines that mods create themselves, like TxOptimizations' own HUD and sky pipelines, were only compiled the first time they were drawn after the reload, and the render thread waited for them. A reload now also compiles, while it prepares, every pipeline that was compiled on first use since the last reload, with the same shaders, and adds it to the new pipeline cache. A pipeline that fails to compile is left out and compiled on first use as before.

### Language files on reload

The language reload read and parsed every language file on the render thread, about 9 ms in a CubeCraft reload. The files are now read and parsed while the reload prepares, and the render thread uses the result when the language and its fallback are still the same; if the language was changed during the reload, it reads the files as before.

### Uniform slots on pipeline changes

When a render pass switches pipeline, the Vulkan backend empties its uniform slot list and then sizes it for the new pipeline, writing every old slot to empty and then every new slot to empty again. It now empties the slots both pipelines share once and lets the resize empty the rest, which leaves the same list of empty slots.

### Atlas upload views

To put each texture into its atlas, a reload uploads it to its own small GPU texture and draws that into the atlas once for every mipmap level. It created a new view of the whole small texture for every level, five identical views per texture with 4 mipmap levels, and creating views was the largest part of the 92 ms reload frame on CubeCraft. Each small texture now gets one view that is used for every level, which is the same view the draws sampled before.

### Shader module cleanup

After a pipeline is built, each of its compiled shader modules frees its SPIR-V and destroys the SPIRV-Cross context that read its inputs and bindings. Destroying those contexts frees thousands of small native allocations, and with every reload worker doing it at once it took 424 ms of the 2,275 ms of worker time in a CubeCraft reload, more than any loading step. Modules are now closed in the same way on one background thread, so the reload workers go straight on to the next task; nothing uses a module after it is closed.

### Unifont reuse

Every reload unzipped and parsed the three Unifont files, more than 60,000 glyph lines read byte by byte, and font loading finished last in a CubeCraft reload because of it. The parsed font is never changed after loading and closing it does nothing, so a reload now reuses the one from the previous reload when the file's bytes have the same SHA-256 and the size overrides are equal; any other file is parsed as before. Only the fonts loaded by the latest reload are kept, and they are the ones the game is already using.

### Duplicate sprite warnings

To draw chests, shulker boxes, banners, decorated pots and bells in the chunk mesh, the mod adds their textures to the blocks atlas, and vanilla keeps them in their own atlases too, so every reload logged about 110 "Duplicate sprite" warnings on the render thread. The warning is no longer written for textures in the folders the mod's `assets/minecraft/atlases/blocks.json` adds when one of the two atlases is the blocks atlas; any other duplicate is logged as before.

### Missing texture warnings

Model loading is the last part of a reload to finish, and its final step wrote a warning for every model with a missing texture, one after another on one thread, to the console and to latest.log. CubeCraft's resource pack has about 1,360 such models, so that step held up the end of every reload with its pack. The same warnings are now written by a background thread after the models are done, in the same order and with the same text.

### GPU memory freed in the background

Vulkan resources that are closed are destroyed two frames later, once the GPU is done with them, and that destruction ran on the render thread. Destroying many at once stalls the frame: after a resource reload with Sodium, which closed the buffers of every loaded chunk region, it took 17 ms of a 33 ms frame on CubeCraft. Buffers, textures and texture views are now destroyed on a single background thread in the same order, at the same point after the GPU finished with them; the GPU memory allocator is internally synchronized, so this is safe. Other resources are destroyed on the render thread as before, and the game waits for the background thread before it shuts the GPU device down.

### First inventory open

The first time the inventory opened in a session, the game loaded about 30 classes it had not needed yet (the recipe book, the creative screen it checks for, the player preview and their Fabric hooks), and every mod's mixins were applied to each one on the render thread right as E was pressed. In survival that made the first open take 12.9 to 14.4 ms against 0.5 ms afterwards. Once the game finishes loading, a background thread now loads those classes without running their setup code, so the first open took 4.4 ms in the same test; what is left is the setup code and lambdas, which can only be made when they are first used.

### Data fixers and crash report preload at startup

Before registering blocks, the game builds its data fixers (the rules that upgrade old worlds and settings) and fills a throwaway crash report so the crash report code is loaded in advance, which includes reading the PC's hardware details. Both ran on the main thread and held up everything after them. They now run on two background threads while the blocks register; the game still waits for the data fixers at the same point as before, just before it opens the window, and the memory kept in reserve for out-of-memory crashes is still set aside on the main thread first. Measured over 5 startups each, with the same startup cache as the launcher, time from process start to the title screen went from a median of 4,528 ms to 4,137 ms.

### Block state cache at startup

Every launch, vanilla builds the shape and lighting cache of all 35,723 block states one after another on the main thread, which took 840 ms in a test run. The states are still registered in the same order, but their caches are now built on all cores right after the block classes finish loading, before anything reads them; in the same test it took 517 ms. Building them inside the block class setup deadlocks, because copper and flower pot blocks read other blocks while their cache is built, so the build waits until that setup is done. A check that rebuilt all 35,723 caches one at a time afterwards found no difference in any cached value.

### Section buffer lookups

Every frame, vanilla asks the vertex and index uber buffers of each terrain layer where every visible section's mesh is stored, and each buffer answers from a hash map keyed by the mesh. Averaged over 8 benchmark scenes, these lookups took 23% of the render thread. Each compiled mesh now keeps its six allocations (vertex and index for each layer) in a small array, and the uber buffers' map calls read and write that array instead: when an upload stores an allocation, when one is looked up or freed, and when a buffer closes. Each buffer also keeps the set of meshes that hold one of its allocations, so closing it clears exactly the entries the map clear dropped. Vanilla only allocates buffer space for compiled meshes, so the placeholder meshes of empty and unbuilt sections still have none. Every lookup returns the same allocation object the map returned, so the draws use the same buffers and offsets. Memory stays about the same, since each mesh gains a six-slot array and each allocation loses its hash map entry.

### Section draw extraction

`LevelRenderer.extractSectionDrawGroups` builds the frame's terrain draw list. For every visible section and every layer it asked the mesh's enum map for the layer's draw, looked up the section's buffers even for layers without geometry and for the placeholder meshes of empty and unbuilt sections, wrapped them in a new slice record, read the vertex size through the pipeline's vertex format list and divided both buffer offsets with 64-bit division. Averaged over 8 benchmark scenes this was 12.9% of the render thread in the method itself, 3.7% in the enum map and 0.7% in the vertex format lookups. Placeholder meshes are now skipped at once, since only compiled meshes ever hold buffer space, and a layer's vertex allocation is checked before its draw, so a layer without geometry costs one array read. The vertex size of each layer is read once per frame from the same pipeline, the allocations are read straight from the mesh without a slice record, and the offsets are divided with 32-bit division, which gives the same quotient because an uber buffer heap's size is an int, so every offset fits in one. The section infos, the draw groups and their order, the hash that merges draws into groups and every draw command are built exactly as before, apart from the split of solid and cutout draws by face direction described under Terrain face directions.

### Section draw commands

Every frame the terrain's indirect draw commands (five ints per draw) and section infos (a position and a fade value per section) are written into GPU-visible memory. Each value went through `ByteBuffer.putInt`, which checks the position against the limit and passes through the JDK's scoped memory access with a reachability fence, and each section info also went through a new `Std140Builder` that aligned every value. Averaged over 8 benchmark scenes, writing the draw commands took 6.2% of the render thread. Both records now check once that the buffer is a writable direct buffer in native byte order with room for the whole record, which every buffer vanilla passes is, store their values with `MemoryUtil` at the same offsets and move the position past them as before. The bytes are identical: the buffer's byte order is the native one the stores use, the section info keeps the std140 layout the builder produced (three ints with the float right after them), and floats are stored by their raw bits like `putFloat`.
### Frozen tick passengers

While ticking is frozen (`/tick freeze`), every frame and every tick asks each entity how many players ride it, to decide whether it still moves smoothly, and the answer always came from a new stream over all of its passengers and their passengers. An entity without passengers now answers 0 straight away, which is the count the empty stream returned. An entity that carries passengers counts them through the same stream as before.

### Section update lookups

Every frame, level extraction looks up the update state of each visible chunk section to find the sections that need a rebuild, and each lookup worked out the section's slot in the update grid with two modulo operations before reading it. The grid of render sections is laid out the same way, so the slot number every render section already carries is now tried first. When the update state in that slot belongs to the same section and the section lies inside the grid, it is exactly the state the full lookup returns; in any other case the full lookup runs as before.

### Section update scan

Every frame, level extraction walked all visible chunk sections and looked up each one's update state to find the few that need a rebuild. The answer for a section only changes when a block or light change marks a clean section dirty, when the update grid or the grid of render sections moves with the camera or is created, or when the list of visible sections is cleared or refilled. Each scan now remembers, in visible order, the sections that were still dirty when it checked them. While none of those changes has happened since, the next scan checks only the remembered sections, with the same checks in the same order; every other visible section was clean or outside the grid at the last scan and still is. Changes are counted when they start and when they end, so a change made by another thread while a scan runs makes the following scan a full one, and any change makes the next scan walk all visible sections as before.

### Block entity sections

Every frame, block entity extraction walked all visible chunk sections and fetched each one's mesh to see whether it holds any block entities, although most sections hold none. A section only gains block entities when a mesh that has some is installed on it after it compiles. Each frame now remembers, in visible order, the sections whose mesh had block entities when it checked them. While no mesh with block entities has been installed and the list of visible sections has not been cleared or refilled since, the next frame checks only those remembered sections, with the same fade test and extraction in the same order; every other visible section had no block entities at the last check and still has none. Meshes are also installed from the section compile threads, so each install is counted when it starts and when it ends, and a frame that starts while one is still running is followed by a full walk. Any install of a mesh with block entities and any change to the visible list makes the next frame walk all visible sections as before.

### Display entity view range

Every frame, each block, item and text display read its view range from its synced entity data to decide whether it is close enough to draw, which goes through the data table, the data entry and a boxed float. The view range only changes through the synced data, and every change to synced data calls the entity's update hook right after the new value is stored, so each display now keeps its view range in a plain field that is read when the display is created and again whenever that hook reports the view range. The distance check uses that field with the same arithmetic, so it gives the same answer.

### Dragon head culling boxes

Every frame, each mob, armor stand and player close enough to be drawn reads the item on its head during its visibility check, because a worn dragon head sticks out of the body and vanilla grows the culling box by half a block for it. That read went through the equipment map, the head stack and the stack's item holder of every one of them, and took 2.2% of the render thread in a profile. The equipment of each entity now also keeps its head stack when that stack holds a dragon head, and the empty stack otherwise. It is refreshed whenever the head slot is set, all equipment is replaced or cleared, and when the equipment is created, which are the only ways its slots change. The culling check runs vanilla's dragon head test on the kept stack. A stack never changes its item, so a head that was not a dragon head cannot become one later, and the test still reads the stack's count, so a dragon head stack that drops to zero stops counting just as before. This keeps one reference per entity's equipment.

### Entity flags

Each entity keeps seven on and off states in one byte of its synced entity data: on fire, sneaking, sprinting, swimming, invisible, glowing and gliding. Every read of one of them went through the data table, the data entry and a boxed byte, and entity extraction reads several for every entity it extracts each frame, such as the glow check, the invisibility check and the sneaking check. The glow check of text displays alone took 0.57% of the render thread in a profile. The byte only changes when synced data stores a new value, either set on the entity or received from the server, so each entity now also keeps a plain copy of it, written right after vanilla stores the new value and before any update hook runs. Every read tests the same bit of that copy. The copy starts at 0, the value vanilla defines for the byte, and adds one byte field per entity.

### Item layers in render states

Every item render state was created with one item layer, and every layer holds its own transform matrix. Entity render states are created anew for every extracted entity every frame: the state of each living entity has an item state for its head, mobs that hold items add one per hand and players one more, so a player's state alone created four layers and four matrices whether it held anything or not. Creating the hand item states of player states took 0.29% of the render thread in a profile. An item state now creates its first layer when an item model first asks for a layer, which is when vanilla hands out that layer. Vanilla only reads the layers in use, except for the block light flag, which it reads from the first layer even when no layer is in use. For a state that has not created a layer yet, that read returns false, which is what the untouched layer vanilla would have read holds.

### Biome blending

Grass, leaves, water and other tinted blocks get their color from the average over the biome blend area around them, 5 by 5 blocks at the default blend radius of 2. Every block that needed a tint looked up the biome of every block in its area again, and each lookup runs biome zooming and a chunk lookup in the client chunk map. In a profile of the chunk build threads this took 36% of their time, 28% in the chunk lookup alone. Neighboring blocks share almost all of their area, so while a section is meshed, each biome looked up for it is kept in a per-thread grid that covers the section and the blend radius around it, and the averages of all blocks in the section read their biomes from there. The color resolver still runs for every sampled position with the same biome and coordinates, the colors are added in the same order and divided the same way, so every tint is identical. Tints whose blend area reaches outside the grid, tints asked for outside chunk meshing and a blend radius of 0 look their biomes up as before. The grid is emptied after every section, and also in the middle of one whenever a chunk loads, a chunk's biomes change or the tint caches are cleared, which are the moments vanilla throws away its finished tints. A tint stored after that is therefore never averaged from a biome looked up before it, exactly as in vanilla. The grid holds at most 6,400 biome references per meshing thread at radius 2 (14,400 at the maximum radius of 7).

### Tint cache layers

Every tint lookup goes through vanilla's tint cache, which keeps the finished tints of each chunk column in one array per height. Finding that array took the column's read lock and searched the column's heights one by one on every lookup, so meshing threads working in the same column kept writing to the same lock. Each thread now also remembers the arrays it got for up to 16 heights of the column it is in, and only asks the cache again for a height it has not seen there, after it moves to another column, or once vanilla replaced the column's cache. A column's cache never swaps the array of a height it already holds, so the remembered array is the same one the lookup returns, and tints are still computed, stored and read exactly as before.

### Smooth lighting cache

Smooth lighting reads the light level and the shade of the blocks around every face it lights. Vanilla keeps the last 100 of each in two small hash maps per meshing thread, but a section is meshed along x first, then y, then z, so the neighbors one row and one layer away were looked up 16 and 256 blocks earlier and have long been pushed out of those maps when they are needed again. Every miss reads the light engine or the block's shade again and inserts it into the map. In a profile of the chunk build threads, the shade map's lookups and inserts took 3.3% of their time and the light lookup through its map another 6%. While a section is meshed, the light and shade of every position in the section and the one block border around it are now kept in two flat arrays indexed by position (18 x 18 x 18 entries, 23 KB each per meshing thread), filled the first time a position is asked for. Each entry holds exactly what vanilla's maps hold, the sky and block light from the light engine and the shade of that position's block, and the light emission of the block being lit is still applied on every lookup the same way. Positions outside that box and lookups outside chunk meshing still go through vanilla's maps.

### Block states while meshing

Meshing a section reads the same block states over and over: face culling reads all six neighbors of every block, smooth lighting reads up to ten positions for every lit face, and fluids read their neighbors and the blocks above them. Every read goes through the section copy's palette, unpacking a bit-packed index and looking it up in the palette. While a section is meshed, the block states of the section and its one block border are now kept in a per-thread array of 18 x 18 x 18 entries, filled on the first read of each position, and later reads of that position return the stored state. Section copies are snapshots that never change while a section is meshed, so every read returns the same state as before. The meshing loop's own read of each block still goes straight to the section copy, since it reads every position only once, reads from other threads and outside the border do the same, and after the section only the entries that were filled are reset.

### Enclosed fluids

Every block with a fluid in it is handed to the fluid renderer during meshing, including water deep inside an ocean or lake, which has the same fluid on all six sides and draws nothing. Fabric's fluid rendering hooks run each of those calls through their handler chain first: a cancellable callback, the default handler, a scoped value bound around a lambda that calls the fluid renderer a second time, and a second callback, before vanilla reads the six neighbors and returns without drawing a face. Fluid rendering took 22% of the chunk build threads in a profile, every enclosed block included. When no mod has registered its own handler for the fluid, meshing now reads the six neighbors first and skips the call when all of them hold the same fluid, which is exactly when vanilla draws no face. Fluids with any other neighbor, and fluids with a registered handler, go through Fabric's chain as before.

### Terrain face directions

Vanilla draws the solid and the cutout geometry of each section as one range. The terrain pipelines cull back faces, so the GPU drops every face that points away from the camera, but only after it has run the vertex shader on its four vertices and assembled its two triangles. Sodium keeps each section's faces grouped by direction and only draws the groups that can face the camera; vanilla terrain now does the same.

When a section is built, its solid and cutout vertex data is reordered in place right after the mesh is created from the finished buffers, so every quad is included no matter who wrote it, Fabric's renderer and baked block entities included. A quad joins the up, down, north, south, east or west group only when its four positions lie exactly on one axis-aligned plane and both triangles the GPU draws from it (vertices 0, 1, 2 and 2, 3, 0 through the shared quad index buffer) wind towards that direction. The visible side of a vanilla face is the one its (v1 - v0) x (v2 - v0) normal points to, as the vertex order of every block face in `FaceInfo` shows. Every other quad, sloped, crossed, degenerate, twisted or with a non-finite position, goes into an unassigned group that is always drawn. So do narrow faces: for each triangle the build measures its thickness, twice its area divided by its two shorter edges, which is 1/2 for a full block face, 1/3 for a slab side and 1/4 for the half-block faces of stairs. Faces thinner than 1/4, such as fence posts, panes, torches, buttons and the edges of doors and carpets, stay unassigned, because the thinner a face is the farther behind it the camera has to be before it can be skipped, and one of them would hold back its whole group. Across the 23,701 axis-aligned faces of the vanilla block models, every face of the 910 models made only of full faces (the plain cubes of natural terrain) and the slab and stair faces stay in their direction groups. The quads keep their order inside each group and swap places through two scratch quads on the memory stack, so no second copy of the vertex data is made. Each group remembers the plane nearest to the side it faces (the lowest plane of the up faces, the highest of the down faces, and so on for the sides) and its thinnest face, and the mesh remembers one box around the direction faces of its solid and cutout layers together, all taken from the actual positions, so model faces outside the block still count. A solid or cutout layer keeps its group ends, planes, face thickness and a mask of its occupied groups in one array of 21 ints (104 bytes), and each compiled mesh holds those arrays in a three-entry array (32 bytes) next to the box (six doubles, 64 bytes). Translucent geometry is left exactly as vanilla builds and sorts it.

Each frame the skip test reads what the GPU will actually use: the projection the level is uploaded with (view bobbing and the nausea warp included), the terrain's view rotation, the camera block position and offset rounded to a float exactly as vanilla's globals uniform does, the size of the main render target, and the subpixel precision the Vulkan device reports, read from its limits while the device is created. The eye is the center of that projection, computed in double precision with a bound on its own error. If any of this is missing or degenerate, such as a field of view of 0 during startup, an orthographic projection or a graphics backend that does not report its subpixel precision, every group is drawn, which is vanilla's draw list. A direction group is skipped only when two things hold for the section's box. First, the box lies strictly inside the clip volume even after the GPU's own rounding, so no triangle of it is clipped and the GPU decides each triangle's facing from its own three vertices. Second, the eye is behind every plane of the group by more than what snapping and rounding can undo. For a triangle with the eye at distance h behind its plane, its screen area is exactly half the viewport width times half its height times the determinant of the projection's x, y and w rows times its world area times h, divided by the three clip depths. Moving each vertex by at most one subpixel step plus the GPU's single-precision error changes twice that area by at most twice the step times the summed lengths of two edges plus eight times the step squared, and both are bounded from the box, the face thickness and the projection, so the group is skipped only when the area is larger than any such change. Shader arithmetic is allowed up to 21 single-precision rounding units of the camera-relative magnitudes, which covers any order of the matrix products, fused or not. An offline check placed random faces at grazing angles from 1 to 1000 blocks away, with real fields of view, aspect ratios, 4 and 8 bit subpixel grids, view bobbing and camera coordinates up to 30 million, found the smallest distance behind the plane at which each one is skipped, and then simulated the GPU there in single precision with three evaluation orders and floor, nearest and ceiling snapping. In 59,454 triangle checks no triangle ever turned front-facing, while at 0.9 times the skip distance some already did, so the test is close to the real limit. For full block faces 64 blocks away the camera has to be 0.56 blocks behind their plane, 4.5 blocks at 256, 16 at 512 and 60 at 1000, and 2.0 to 2.1 times that for slab and stair faces. In a synthetic flat world, 57 to 87% of the groups the old fixed 1/32 margin skipped are still skipped. Nearly all of the difference comes from sections that cross the edge of the view, which are never skipped now; at a subpixel grid fine enough to make snapping irrelevant, the share rose by only 0 to 3 points. Everything that depends only on the section, the clip volume check, the depth range and the factors of the threshold, is worked out once per section with fused multiply-adds and no division, by multiplying the inequality through by the fourth power of the nearest depth, and shared by its solid and cutout layers. Each direction group then costs a subtraction, a fused multiply-add, two multiplications and a comparison, without branches. In an offline run over 7,648 visible sections in shuffled order this took 7.7 ns per layer for sections with both layers and 11.6 ns for sections with one, against 23 to 28 ns per layer when every layer computed its own bound. For sections with one opaque layer the decisions are the same as before; for sections with both, the shared box is the union of the two layers' boxes, so a group is occasionally drawn that a layer's own smaller box would have let it skip (2% of their decisions in a randomized comparison, never the other way round). The groups are stored east, up, south, unassigned, north, down, west, so the visible ones next to each other become one draw, and a layer never needs more than three draws per section. The same sections are drawn with the same section infos and fade-in, and a section's draws share its instance data.

A skipped face therefore never had a pixel to draw. This rests on the Vulkan rules that shader additions and multiplications are correctly rounded and that facing is decided from the framebuffer positions at the reported subpixel precision, plus the assumption that the fixed-function divide and viewport transform are at least as precise as single precision, which the specification does not state. One difference from vanilla remains: opaque faces of different directions are now drawn in a different order, and the depth test lets the later of two fragments with exactly the same depth win. That can only happen along lines where two faces meet or cross, such as a slab or path block against a full block, a fence bar entering its post or the crossed planes of crops, and only where both depths round to the same float; Sodium orders its faces by direction too.

### Memory

Less live memory means shorter and rarer garbage collections.

- Block state neighbor table: instead of every block state holding its own table of neighbor states, all states of a block share one bit-packed table that `setValue` indexes into. `getValue` keeps reading vanilla's per-state value array.
- Block state cache deduplication: identical collision shapes and face sturdiness arrays in the per-state cache are shared instead of stored once per state.
- Empty item component patches: items with no changed components share one empty map instead of each allocating their own.
- Uniform light layers: a 2 KB light array where every value is the same non-zero level (sky sections at full light, sent by the server or loaded from disk) is stored as just that level, the form vanilla already uses for freshly lit sections. Reads return the same value and the array comes back filled on the first write. A heap dump of a 32 chunk singleplayer world had about 14,000 such arrays (29 MB).

## Commands

`/txprofile reload` makes the next resource reload, such as the one after a server sends its resource pack, run with vanilla's reload profiling, which logs to `latest.log` how long the reload took and how long each reload listener spent preparing and applying. `/txprofile` records 10 seconds and saves one report to the `txoptimizations` folder. The frame in which the profile starts is not counted, since starting the Flight Recorder can stall it for 100 to 300 ms. The report starts with the frame rate from every frame's time: average, 1% low and 0.1% low (the frame rate at the 99th and 99.9th percentile frame time) and the worst frame, followed by GPU command counts per frame (render passes, pipeline sets, draws, multi-draw calls, indirect and batched draws), the scene (server or world, dimension, position, loaded entities and players, particles), whether hitboxes, chunk borders and the F3 overlay are on, the video options, the GPU and driver, the JVM with its collector, heap and launch flags, and the swapchain's format, image count and present mode next to the format of the main framebuffer that is copied into it. Then come frame time percentiles (p50 to p99.9), how many frames took longer than 1, 2, 4, 8 and 16 ms, and the frame count, average frame rate and worst frame of every second. The GPU part lists the time of every render pass and pipeline per frame. It also lists the 10 frames that took the GPU longest, each with its 4 most expensive passes, timed when the frame's results were read. The CPU part samples the render thread's call stack every millisecond and lists every sampled method by its own time (with its top callers), the 30 hottest call stacks up to 12 frames deep, and every method by its time including callees, plus the CPU time of every thread. A separate section covers only the slowest 1% of frames: what the render thread was doing inside them, their 15 hottest call stacks, the 10 worst frames with the samples taken during each, and every garbage collection with its time, length, the old generation's size after it and the used and committed size of all memory pools, marked on any slow frame it overlaps. It also lists how much memory each thread allocated per second and how much the render thread allocates per frame, which is what fills the young generation and sets how often those collections happen. The JDK's built-in Flight Recorder adds JVM events: every safepoint, every VM operation by name (garbage collection, deoptimization, handshakes and others) with the longest ones, every JIT compilation with the longest ones, deoptimizations by method and reason, and every time the render thread parked or waited for a lock for 0.2 ms or longer, by call site. Every garbage collection pause is broken down into G1's phases with their times, the slowest worker in each parallel phase (object copy, root scanning, code roots and others), the eden, survivor and old generation sizes before and after it, and how many regions it collected and MB it copied, which shows whether a long young pause comes from surviving objects or from scanning references. It also samples the allocations, listed per thread by class and by allocation site six frames deep. About 10000 samples make one sample roughly 0.01%, so shares below that are not measurable. Sampling another thread's stack briefly pauses it, so the render thread runs a little slower while the profile records. Copy labels are worked out once per buffer and pipeline labels once per pipeline, and the profiler's timestamp queries are reset in one call per frame after reading them instead of one call per query, to keep the profile's own cost low.

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
