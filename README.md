# Glory Kill — DOOM-style executions for Minecraft 1.20.1 (Forge + GeckoLib)

Weaken a **hostile mob** to 20 % health and it **staggers**. Walk up, press **G**, and you and the mob are locked into a
server-controlled execution: a GeckoLib first-person animation of *your own arms*, a matching GeckoLib animation on the
victim, camera shake, blood, sound, lethal damage on an exact tick, and a heal reward. Everyone nearby sees the same
execution from the outside.

* Minecraft **1.20.1**, Forge **47.x**, Java **17**, **GeckoLib 4 required** (mandatory in `mods.toml` *and* checked in the
  mod constructor — no GeckoLib-free fallback).
* **Every hostile mob** can be executed, vanilla or modded. **Players never can** (no player-vs-player executions).
* **Six moves** (choreographies), picked per execution; **modded mobs get one universal move** by default.

### What's new in 1.1
| | |
|---|---|
| All hostile mobs | any `Enemy` (zombie, skeleton, creeper, spider, blaze, piglin brute, wither, warden, modded hostiles …) |
| 6 moves | `zombie` (own GeckoLib model), `stab`, `slam`, `neck_snap`, `uppercut`, `rip` — random per execution, never the same twice in a row |
| Whole-body victim animation | the mob keeps its **own model** (armour, held items, modded model) while a GeckoLib rig animates its whole body |
| Modded mobs | get the single universal move (`stab`); `moddedMobsUseAllMoves = true` gives them everything |
| No PvP | players are rejected by one central rule used by the stagger system, the server check and the client prompt |
| Size-aware | camera frames the victim's head whatever its height; the player's stand-off distance grows with the mob's width; mobs wider than 2.2 / taller than 4 blocks (Ghast, Ender Dragon) are skipped |
| Adaptive blood | red for flesh, bone dust for skeletons, green goo for slimes, embers for fire mobs, violet for the End … |
| Test tools | `/glorykill moves`, `/glorykill force <move>`, `tools/check_assets.py` validator |

---

## 1. Verification status — read this first

**Version 1.0 compiled and ran for you** (confirmed), so the GeckoLib / Forge API assumptions it used are proven. Version
1.1 was written the same way — without being able to run Gradle or Minecraft here — and checked as far as possible offline:

| Verified offline | How |
|---|---|
| All 47 Java files type-check (generics, lambdas, overloads, flow) against stubs of the Minecraft / Forge / GeckoLib API | `javac`; mutation-tested to confirm the check catches wrong types, missing returns, wrong arity |
| Geometry (yaw/forward/right axes, stand-off scaling, airborne placement), phase boundaries, weighted move picker (exact boundaries + statistics) | 48 numeric tests |
| 23 JSON files valid; every Java definition ↔ asset file pair consistent: animation names, bones per rig, phase lengths vs timeline, `impact;/react;/kill;` keyframes on the right ticks, no pops between phases, body animations end neutral | `tools/check_assets.py` (0 errors, 0 warnings) |
| First-person fists stay in view at the key moments of every move (screen-space projection through GeckoLib's bone transform order) | forward-kinematics preview, used to tune the keyframes |

| **Not verified** — needs your run | |
|---|---|
| How the new animations *look* | they are placeholders; refine in Blockbench (§6). `[calibration]` toggles fix flipped axes (§8) |
| The new whole-body path on real mobs | test plan in §10 |
| The few new vanilla API calls | `RenderLivingEvent.Post`, `Mth.rotLerp`, `Creeper#setSwellDir`, `StringArgumentType` (standard 1.20.1; see §9) |

---

## 2. Build & run
1. JDK 17 (Gradle also auto-downloads one through the foojay plugin).
2. **Gradle wrapper:** the wrapper *jar* is binary and not included — open the folder in IntelliJ IDEA / Eclipse, or run
   `gradle wrapper --gradle-version 8.8` once, or copy `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` from
   any Forge 1.20.1 MDK.
3. `./gradlew runClient` / `runServer` / `build` (`build/libs/glorykill-1.1.0.jar`).
4. Players need **GeckoLib 4.7+ for 1.20.1 Forge** (`gradle.properties` builds against 4.8.2).

---

## 3. Playing

| | |
|---|---|
| Hit a hostile mob until it is at ≤ 20 % health | it **staggers** for 5 s: frozen, glowing, can't attack, can't die from normal damage |
| Walk within 2.5 blocks, look at it | `[G] GLORY KILL` appears under the crosshair |
| Press **G** (rebindable in *Controls*) | the server picks a move and starts the execution |
| `/glorykill moves` | lists every move (op) |
| `/glorykill stagger` | weakens + staggers the mob you look at (op) |
| `/glorykill force [move]` | executes the mob you look at, ignoring health/look checks; `force slam` forces a move (op) |

During an execution: movement, jumping, attacking, item use and block interaction are disabled; the player is invulnerable
(configurable); the mob only takes the scripted lethal hit.

### The moves
| move | you | the mob | weight | modded mobs |
|---|---|---|---|---|
| `zombie` | thrust and rip | **replaced by its own GeckoLib model**, rips and collapses | 3 | no (zombie only) |
| `stab` | thrust and rip | lurches forward, recoils back | 1 | **yes (universal)** |
| `slam` | both fists overhead, hammered down | squashed flat, crushed a second time | 2 | no |
| `neck_snap` | both hands on the head, wrung round | lifted, twisted, goes limp | 2 | no |
| `uppercut` | crouch, launching punch | thrown up spinning, comes down | 2 | no |
| `rip` | grab both sides, pull apart | stretched thin, then bursts | 2 | no |

Every generic move ends with the mob standing neutral and dying on `lethalTick`; the vanilla death animation (the mob
keels over the way *it* normally does) takes over, and the corpse disappears at the same moment the execution ends.

### Which mobs?
`GloryKillRegistry.check(entity)` is the single rule set (server, stagger system and client prompt all use it):

1. **never a player** → `PLAYER`
2. must be hostile (`net.minecraft.world.entity.monster.Enemy`) → `NOT_HOSTILE`
3. type not in `disabledEntityTypes` → `DISABLED_TYPE`
4. width ≤ `maxVictimWidth` (2.2) and height ≤ `maxVictimHeight` (4.0) → `TOO_LARGE`
5. at least one enabled move for that type → `NO_MOVES`

Mounted mobs (spider jockeys) are refused (`isPassenger/isVehicle`). Flying mobs work: the player keeps their own height.

### Configuration
`serverconfig/glorykill-server.toml` (per world, synced to clients; copy to `defaultconfigs/` for new worlds):

| key | default | |
|---|---|---|
| `enabled` | true | master switch |
| `healthThreshold` | 0.20 | executable at ≤ this fraction of max health |
| `maxDistance` | 2.5 | blocks, eye → hitbox (server allows +0.75 latency tolerance) |
| `maxLookAngle` | 35 | degrees (server check) |
| `staggerDuration` | 5.0 | seconds · `staggerCooldown` 5.0 s · `protectStaggered` true · `glowWhenStaggered` true |
| `gloryKillHeal` | 2.0 | **hearts** healed on success |
| `bloodParticles` / `gloryKillSounds` | true / true | |
| `invulnerableDuringExecution`, `requireLineOfSight`, `cancelDriftBlocks` | true, true, 3.0 | |
| **`genericMoves`** | true | all hostile mobs can use the generic moves (false = only mobs with their own definition: the zombie) |
| **`moddedMobsUseAllMoves`** | false | modded mobs normally get only the universal move |
| **`maxVictimWidth` / `maxVictimHeight`** | 2.2 / 4.0 | larger mobs are skipped |
| **`disabledMoves`** | [] | e.g. `["glorykill:slam"]` |
| `disabledEntityTypes` | [] | e.g. `["minecraft:warden"]` |

(`requireHostile` was removed: hostile-only is now a hard rule.) `config/glorykill-client.toml`: `cameraShake`,
`cameraShakeIntensity`, `fovPunch`, `cameraAnimation`, `cameraPositionAnimation`, `firstPersonArms`, `forceFirstPerson`,
`showPrompt`, `useBundledVictimTexture`, `blendInTicks/OutTicks`, `debugLogging`, `[calibration]` toggles (§8).

---

## 4. Architecture

```
 CLIENT                                          SERVER (authoritative)
 ──────                                          ──────────────────────
 [G] → GloryKillTargeting (hint only)
     → RequestGloryKillPacket(entityId) ───────▶ GloryKillManager.tryStart  ◀── validates EVERYTHING
                                                   GloryKillRegistry.check: not a player · hostile · allowed · size · moves
                                                   health ≤ threshold · distance · look cone · line of sight · room to stand
                                                   nobody else executing · not mounted
                                                 GloryKillRegistry.pick: weighted move, not the player's previous one
                                                 ActiveGloryKill.begin: lock victim, place the player (width-scaled spot)
 ClientGloryKillManager ◀── StartGloryKillPacket (move id + positions; to everyone tracking; late joiners too)
   ClientGloryKillSession  (clock, phase, positions)
     FirstPersonArmsAnimatable ─ GeckoLib ─ RenderHandEvent       (you)
     ExecutorAnimatable        ─ GeckoLib ─ RenderPlayerEvent.Pre  (everybody else / F5)
     VictimAnimatable          ─ GeckoLib ─ RenderLivingEvent.Pre  MODEL: model replaced  |  BODY: own model + animated root bone
   GloryKillCamera (auto-aim at the head, camera bone, shake, FOV punch, blend in/out)
                          ◀── SyncGloryKillPacket every 10 ticks (clock correction)
                                                 tick 0…N: timed effects · impact hook · tick lethalTick: VICTIM DIES (server)
 end ◀──────────────────── EndGloryKillPacket
```

### Victim modes (`GloryKillDefinition.VictimMode`)
* **`MODEL`** — the mob's renderer is cancelled and a GeckoLib model of the mob is drawn (the zombie's `.geo.json`).
  Needs a rig that matches the mob.
* **`BODY`** — works for *every* mob. The mob's own renderer draws it as usual; in `RenderLivingEvent.Pre` the animated
  **`root` bone** of `geo/glory_kill_generic.geo.json` is applied to the PoseStack (same translate / pivot / rotate / scale
  sequence GeckoLib uses for a bone), and `RenderLivingEvent.Post` pops it. Armour, held items and modded models survive.
  The GeckoLib rig only provides the animation; nothing of it is drawn.

### Phase-driven animation
Each animatable has one `AnimationController`; every frame it asks the session which phase we are in
(START / EXECUTE / FINISH, from the **server-synchronised tick**) and plays the matching animation with
`RawAnimation.begin().thenPlayAndHold(name)`. Player and victim therefore switch phase on the same tick, and the server's
`lethalTick` always matches. Keyframe instructions in the animation JSON reach the game (`handleInstructions`).

### Timeline of a move (example: `stab`, same numbers as the zombie)
```
tick  0  START    grab sound
tick 10  EXECUTE  impact       crit + blood            (camera_shake / fov_punch keyframe)
tick 15           victim reacts
tick 20           LETHAL DAMAGE (server) · heal · big blood burst  → vanilla death animation starts
tick 30  FINISH   player_glory_kill_finish / victim *_death
tick 40           end — controls return (the corpse vanishes ~now: endTick = lethalTick + 20)
```
All numbers live in the move's `GloryKillTimeline(executeStart, impact, victimReact, lethal, finishPhase, end)`.

---

## 5. Project layout
```
src/main/java/com/example/glorykill/
  GloryKillMod.java                 entry point; GeckoLib presence check
  common/   GloryKillRegistry (moves, eligibility, weighted pick) · GloryKillDefinition (+Builder, VictimMode)
            definitions/ZombieGloryKill · definitions/GenericGloryKills (stab, slam, neck_snap, uppercut, rip)
            GloryKillManager (validation) · ActiveGloryKill (server session) · StaggerManager · BloodStyle
            ExecutionPosition · GloryKillTimeline · GloryKillState/Phase · AnimationSet · GloryKillEffect
            Reward · GloryKillCameraSettings · GloryKillAssets · GloryKillMath
            GloryKillServerEvents · GloryKillCommands
  network/  NetworkHandler · Request/Start/Sync/EndGloryKillPacket
  config/   GloryKillConfig (SERVER + CLIENT specs)
  client/   ClientEvents · ClientModEvents · ClientGloryKillManager/Session · GloryKillCamera
            FirstPersonGloryKillRenderer · ThirdPersonGloryKillRenderer · GloryKillTargeting/Hud/KeyMappings
    animation/  SessionAnimatable · FirstPerson/Executor/VictimAnimatable · GloryKillGeoModel · GeoRenderBridge · HeldItemRenderer
src/main/resources/assets/glorykill/
  geo/         glory_kill_player(.slim) · glory_kill_player_first_person(.slim) · glory_kill_zombie · glory_kill_generic
  animations/  zombie_{player,player_first_person,victim} · stab_victim ·
               {slam,neck_snap,uppercut,rip}_{player,player_first_person,victim}
tools/         generate_assets.py (regenerates every placeholder) · check_assets.py (validator)
```

---

## 6. Authoring in Blockbench (GeckoLib plugin)

Install Blockbench → *File → Plugins* → **GeckoLib Animation Utils**; use the **GeckoLib Animated Model** format, 64×64.
The placeholders are plain JSON; open any `.geo.json` with *File → Open Model*.
**After exporting, run `python3 tools/check_assets.py`** — it tells you about wrong names, missing bones, retimed phases and
keyframes that no longer match the server timeline.

### Coordinate conventions (same as every GeckoLib entity model)
`forward = −Z (north)` · `up = +Y` · the model's **right side is −X** · rotation **X negative swings a limb forward/up,
positive back/down**. Keyframes are **offsets from the rig's rest pose** (first-person arms rest at X = −80°).
**Sideways swings use Y, not Z** (Y is applied after X): for the right arm `+Y` = outward, `−Y` = inward; left arm mirrored.
`Z` only twists an arm that points forward.

### Files, bones and animation names
| Role | Model | Animation file | Bones |
|---|---|---|---|
| First person (your arms) | `geo/glory_kill_player_first_person(.slim).geo.json` | `{move}_player_first_person.animation.json` | `right_arm`, `left_arm`, `camera`, `item_anchor` (child of `right_arm`) |
| Third person (executor) | `geo/glory_kill_player(.slim).geo.json` | `{move}_player.animation.json` | `root`, `torso`, `head`, `right_arm`, `left_arm`, `right_leg`, `left_leg` |
| Victim — zombie model (`MODEL`) | `geo/glory_kill_zombie.geo.json` | `zombie_victim.animation.json` | same bones as third person |
| Victim — any mob (`BODY`) | `geo/glory_kill_generic.geo.json` | `{move}_victim.animation.json` | **`root`** only |

Animation names (set each animation's loop mode to **Hold on last frame**):
```
zombie  player: player_glory_kill_start | _execute | _finish        victim: zombie_glory_kill_start | _execute | _death
stab    player: (re-uses the zombie player animations)              victim: victim_glory_kill_stab_start | _execute | _death
slam / neck_snap / uppercut / rip
        player: player_glory_kill_{move}_start | _execute | _finish
        victim: victim_glory_kill_{move}_start | _execute | _death
```
Replace any placeholder with your export **under the same file name** — no Java change needed. Only if you change a
*duration* do you edit the move's timeline numbers (the server owns timing). Player rigs are textured with the player's
skin automatically (vanilla 64×64 UV layout; `_slim` files for Alex skins).

### The `root` bone of the generic rig (BODY victims)
`root` pivots at the mob's **feet**; its animated position (px, 16 = 1 block), rotation and **scale** are applied to the mob's
whole body, in the mob's own frame (forward = −Z, so position −Z moves it toward the player).
Rules that make every mob look right:
* **End the `*_death` animation in the neutral pose** (rotation 0, position 0, scale 1). From the lethal tick the vanilla
  death animation makes the mob keel over; if your animation also fell over, the two would add up. (`check_assets.py` enforces it.)
* Use scale for squash & stretch (squash = Y < 1, X/Z > 1), rotation Y for spins, position Y for lifts.
* Keep lethal at `endTick − 20` so the corpse disappears as the execution ends.
* The pivot is at the feet, so rotating X/Z swings the head through an arc proportional to the mob's height — keep leans small
  (≤ 25°) or the head clips the ground on small mobs.

### The `camera` bone (first-person rig)
Animate it like a camera sitting at the origin (defaults — flip with the `[calibration]` toggles if your preview disagrees):

| Blockbench | Effect |
|---|---|
| rotation X + / − | look down / up |
| rotation Y + / − | turn right / left |
| rotation Z + / − | tilt left / right |
| position −Z / +Z | camera forward / back (lunge) |
| position +Y / −Y | up / down |
| position −X / +X | right / left |

Position needs `cameraPositionAnimation = true` and moves the camera by `value / 16` blocks. The base view is aimed
automatically at the victim's head (`atan2` of eye vs. 80 % of its height); a definition's `viewPitch` is an extra bias.

### Animation event keyframes (*Animate Effects → Timeline*)
Instructions separated by `;`, arguments by `:`.
`impact;` `react;` `kill;` `finish;` `start;` (markers, visible with `debugLogging`) ·
`camera_shake:<intensity>:<ticks>;` · `fov_punch:<degrees>;`
They are cosmetic — **gameplay never depends on them**; the server timeline decides damage.

### Regenerating the placeholders
`python3 tools/generate_assets.py` rewrites every model/animation/texture from the tables in the script.

---

## 7. Adding moves and mobs

**A new generic move** (works on every hostile mob) — copy one method in `GenericGloryKills`:
```java
static GloryKillDefinition headbutt() {
    return GloryKillDefinition.genericBuilder(id("headbutt"))
        .weight(2)                                   // .universal(true) would also allow it on modded mobs
        .position(ExecutionPosition.facingEachOther(1.0D, 40))
        .timeline(new GloryKillTimeline(9, 9, 12, 20, 24, 40))
        .playerAnimations(playerSet("headbutt"))     // player_glory_kill_headbutt_start|_execute|_finish
        .victimAnimations(victimSet("headbutt"))     // victim_glory_kill_headbutt_start|_execute|_death
        .assets(GloryKillAssets.generic("headbutt", "headbutt"))
        .effect(20, GloryKillEffect.sound(SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.6f))
        .effect(20, GloryKillEffect.bloodChunks(40, 0.4D, 0.2D, 0.7D))   // coloured to suit the victim
        .build();
}
```
then `GloryKillRegistry.registerGeneric(headbutt());` in `registerAll()` and add the three animation files
(`headbutt_player_first_person`, `headbutt_player`, `headbutt_victim`).

**A mob with its own GeckoLib model** (like the zombie, `VictimMode.MODEL`): copy `ZombieGloryKill`, build with
`GloryKillDefinition.builder(id, EntityType.X)`, `GloryKillAssets.standard("x", texture)`, and register with
`GloryKillRegistry.register(EntityType.X, new XGloryKill())`. A type may have several specific moves; they are mixed into the
same weighted pool as the generic ones. **A move for a specific modded mob** is the same call with that mob's
`EntityType` (e.g. from `FMLCommonSetupEvent#enqueueWork`).

`ExecutionPosition` offsets: **x = right, y = up, z = forward** (toward the victim), relative to the victim's start spot;
the player offset is widened automatically for wide mobs (`forVictim`). Override `onStart/onImpact/onLethal/onEnd` for
custom server behaviour.

---

## 8. First-run calibration (camera & item)
If the camera tilts/turns the wrong way compared to your Blockbench preview, flip the toggle in
`glorykill-client.toml → [client.calibration]` (`invertCameraPitch/Yaw/Roll`, `invertCameraOffsetX/Y/Z`). If the arms look
mirrored, the rig needs its right arm at −X (§6). If the held item is misplaced, move `item_anchor`'s pivot/rotation.
`debugLogging = true` prints session lifecycle and every keyframe.

---

## 9. API surface to double-check if the build fails
Version 1.0's GeckoLib usage is proven by your successful build (classes `GeoRenderBridge`, `GloryKillGeoModel`,
`SessionAnimatable`, `HeldItemRenderer`, `FirstPersonGloryKillRenderer`). New in 1.1, all standard 1.20.1 API and none of it
GeckoLib-specific beyond what 1.0 already used (`RenderUtils` bone helpers, now also in `ThirdPersonGloryKillRenderer`):

`RenderLivingEvent.Post` · `Mth.rotLerp` · `LivingEntity.yBodyRot/yBodyRotO` · `Creeper#setSwellDir(int)` ·
`Entity#getBbWidth/isVehicle/getRandom`, `RandomSource#nextInt(int)` · `AABB#move(double,double,double)` ·
`StringArgumentType.word()/getString` · `Component.literal` · `ResourceLocation.tryParse` ·
sounds `PLAYER_ATTACK_SWEEP, ANVIL_LAND, GENERIC_EXPLODE, TURTLE_EGG_CRACK, ZOMBIE_BREAK_WOODEN_DOOR` ·
`ParticleTypes.POOF` · blocks `BONE_BLOCK, SLIME_BLOCK, MAGMA_BLOCK, PURPUR_BLOCK, MOSS_BLOCK`.

---

## 10. Test plan
1. `/glorykill moves` — six moves listed. `/glorykill stagger` on a **zombie**, press **G** a few times: you should see
   different moves, never the same one twice in a row.
2. Try each deterministically on a **skeleton in armour**: `/glorykill force slam`, `force neck_snap`, `force uppercut`,
   `force rip`, `force stab` — the skeleton keeps its own model and armour, bone-white particles, and keels over after the
   lethal tick. Repeat on a **creeper** (it must not explode), a **spider**, a **blaze** (floating — you must not be lifted)
   and a **slime** (splits when it dies).
3. A **baby zombie**, a **Wither Skeleton**, an **Enderman** (violet blood).
4. Make sure it *refuses*: another **player** (`/glorykill force` and the G key do nothing), a cow, a Ghast ("too big"),
   a spider jockey ("not enough room").
5. A **modded hostile mob**: only `stab` should ever play unless `moddedMobsUseAllMoves = true`.
6. Multiplayer: two clients — the second sees the first execute (body, victim animation, effects), both pressing **G** on
   the same mob in one tick gives exactly one execution, a late joiner sees an ongoing execution, killing the executor's
   connection releases the mob.
7. Camera: the view returns to exactly where you were looking; try a tall mob (Wither: the camera looks up) and a small one.

## 11. Known limitations
* **BODY animation needs the mob to render through vanilla's `LivingEntityRenderer`** (that is what fires
  `RenderLivingEvent`). Some modded mobs with custom renderers (e.g. certain GeckoLib mobs) will not get the whole-body
  animation: they stay frozen at the execution spot while the player animation, effects and the kill still work.
* The zombie's `MODEL` move does not draw armour / held items (the GeckoLib model replaces the mob); the generic moves do.
* Held item is drawn on the **first-person** arms only. Left-handed players still get the item on the right arm.
* Client and server must run the same mod version (the move id is sent over the wire).
* The vanilla death animation (`deathTime`) cannot be altered from outside; that is why generic animations end neutral.

Licence: MIT. GeckoLib is © its authors (MIT).
