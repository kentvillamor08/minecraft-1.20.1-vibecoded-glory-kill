#!/usr/bin/env python3
"""
Generates the placeholder GeckoLib assets for the Glory Kill mod:

  geo/glory_kill_player_first_person(.slim).geo.json   first-person arms rig (+ camera / item_anchor bones)
  geo/glory_kill_player(.slim).geo.json                third-person player rig (uses the player's skin)
  geo/glory_kill_zombie.geo.json                        zombie victim rig
  animations/zombie_player_first_person.animation.json  first-person animations
  animations/zombie_player.animation.json               third-person executor animations
  animations/zombie_victim.animation.json               zombie victim animations
  geo/glory_kill_generic.geo.json                       generic victim rig: ONE bone ('root') that moves a mob's whole body
  animations/<move>_player_first_person.animation.json  } for the generic moves
  animations/<move>_player.animation.json               } slam, neck_snap, uppercut, rip
  animations/<move>_victim.animation.json               } (+ stab_victim: stab re-uses the zombie player animations)
  textures/glory_kill/zombie.png                        optional bundled zombie texture

Re-run after editing:  python3 tools/generate_assets.py
You can also ignore this script completely and author the same files in Blockbench.

Coordinate conventions (Blockbench / GeckoLib, same as every GeckoLib entity model):
  forward = -Z (north), up = +Y, the model's RIGHT side is -X (right arm at negative X).
  rotation X: negative swings a limb forward/up, positive swings it back/down.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/glorykill"


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


# --------------------------------------------------------------------------- geometry
def cube(origin, size, uv, inflate=None, mirror=False):
    c = {"origin": origin, "size": size, "uv": uv}
    if inflate:
        c["inflate"] = inflate
    if mirror:
        c["mirror"] = True
    return c


def bone(name, pivot, parent=None, rotation=None, cubes=None, never_render=False):
    b = {"name": name, "pivot": pivot}
    if never_render:
        b["neverRender"] = True
    if parent:
        b["parent"] = parent
    if rotation:
        b["rotation"] = rotation
    if cubes:
        b["cubes"] = cubes
    return b


def geometry(identifier, bones):
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": identifier,
                "texture_width": 64,
                "texture_height": 64,
                "visible_bounds_width": 4,
                "visible_bounds_height": 4,
                "visible_bounds_offset": [0, 1, 0],
            },
            "bones": bones,
        }],
    }


def humanoid(identifier, slim=False, overlays=True, zombie=False):
    """Full humanoid rig. Skin UV layout identical to a vanilla 64x64 skin."""
    aw = 3 if slim else 4
    torso = [cube([-4, 12, -2], [8, 12, 4], [16, 16])]
    head = [cube([-4, 24, -4], [8, 8, 8], [0, 0])]
    r_arm = [cube([-4 - aw, 12, -2], [aw, 12, 4], [40, 16])]
    r_leg = [cube([-3.9, 0, -2], [4, 12, 4], [0, 16])]
    if zombie:
        # vanilla zombie.png re-uses the right limb UVs for the left limbs (mirrored)
        l_arm = [cube([4, 12, -2], [aw, 12, 4], [40, 16], mirror=True)]
        l_leg = [cube([-0.1, 0, -2], [4, 12, 4], [0, 16], mirror=True)]
    else:
        l_arm = [cube([4, 12, -2], [aw, 12, 4], [32, 48])]
        l_leg = [cube([-0.1, 0, -2], [4, 12, 4], [16, 48])]
    if overlays:
        torso.append(cube([-4, 12, -2], [8, 12, 4], [16, 32], inflate=0.25))
        head.append(cube([-4, 24, -4], [8, 8, 8], [32, 0], inflate=0.5))
        r_arm.append(cube([-4 - aw, 12, -2], [aw, 12, 4], [40, 32], inflate=0.25))
        l_arm.append(cube([4, 12, -2], [aw, 12, 4], [48, 48], inflate=0.25))
        r_leg.append(cube([-3.9, 0, -2], [4, 12, 4], [0, 32], inflate=0.25))
        l_leg.append(cube([-0.1, 0, -2], [4, 12, 4], [0, 48], inflate=0.25))
    bones = [
        bone("root", [0, 0, 0]),
        bone("torso", [0, 12, 0], "root", cubes=torso),
        bone("head", [0, 24, 0], "torso", cubes=head),
        bone("right_arm", [-5, 22, 0], "torso", cubes=r_arm),
        bone("left_arm", [5, 22, 0], "torso", cubes=l_arm),
        bone("right_leg", [-1.9, 12, 0], "root", cubes=r_leg),
        bone("left_leg", [1.9, 12, 0], "root", cubes=l_leg),
    ]
    return geometry(identifier, bones)


def first_person(identifier, slim=False):
    """
    First-person arms rig. The origin (0,0,0) IS the player's camera (view space).
    Arms start 2px in front of the camera, bent 80 degrees so they point forward.
    Bones:  camera       (no cubes) - animated camera offset, read by the mod
            item_anchor  (no cubes) - child of right_arm, the held item is drawn here
    """
    aw = 3 if slim else 4
    half = aw / 2.0
    bones = [
        bone("root", [0, 0, 0]),
        bone("camera", [0, 0, 0]),
        bone("right_arm", [-5, -3, -2], "root", rotation=[-80, 0, 0], cubes=[
            cube([-5 - half, -15, -4], [aw, 12, 4], [40, 16]),
            cube([-5 - half, -15, -4], [aw, 12, 4], [40, 32], inflate=0.25),
        ]),
        bone("left_arm", [5, -3, -2], "root", rotation=[-80, 0, 0], cubes=[
            cube([5 - half, -15, -4], [aw, 12, 4], [32, 48]),
            cube([5 - half, -15, -4], [aw, 12, 4], [48, 48], inflate=0.25),
        ]),
        # counter-rotation so that at rest the item frame is view-aligned, exactly like a vanilla held item
        bone("item_anchor", [-5, -13, -2], "right_arm", rotation=[80, 0, 0]),
    ]
    return geometry(identifier, bones)


def generic_rig(identifier):
    """
    Generic victim rig for the whole-body moves. The mob keeps its own model; the animated 'root' bone (pivot at the
    mob's feet) is applied to it as a transform: position in pixels (16 = 1 block), rotation, scale.
    The 1-pixel cube only exists so the model is never empty; it samples a transparent corner of the texture.
    """
    return geometry(identifier, [
        bone("root", [0, 0, 0], cubes=[cube([0, 0, 0], [1, 1, 1], [56, 60])], never_render=True),
    ])


# --------------------------------------------------------------------------- animations
def key(t):
    return str(float(t))


def track(points, easing=None):
    out = {}
    for t, v in points:
        out[key(t)] = {"vector": v, "easing": easing} if easing else v
    return out


def bones_block(spec):
    """spec: {bone: {"rotation": [(t,[x,y,z])...], "position": [...]}}"""
    out = {}
    for name, channels in spec.items():
        out[name] = {}
        for channel, points in channels.items():
            easing = "easeInOutSine"
            if isinstance(points, tuple):
                points, easing = points
            out[name][channel] = track(points, easing)
    return out


def animation(length, spec, timeline=None, loop="hold_on_last_frame"):
    a = {"loop": loop, "animation_length": length, "bones": bones_block(spec)}
    if timeline:
        a["timeline"] = {key(t): s for t, s in timeline.items()}
    return a


def animations(entries):
    return {"format_version": "1.8.0", "animations": entries}


Z = [0, 0, 0]

# ---- third person executor (player rig) - rotations are absolute (rig rest pose = 0)
TP_START = animation(0.5, {
    "torso": {"rotation": [(0.0, Z), (0.25, [14, 0, 0]), (0.5, [10, 0, 0])],
              "position": [(0.0, [0, 0, 4]), (0.3, Z), (0.5, Z)]},
    "head": {"rotation": [(0.0, Z), (0.5, [6, 0, 0])]},
    "right_arm": {"rotation": [(0.0, Z), (0.2, [-95, 0, -6]), (0.35, [-40, 0, -6]), (0.5, [-105, 0, -4])]},
    "left_arm": {"rotation": [(0.0, Z), (0.2, [-92, 0, 8]), (0.5, [-95, 0, 8])]},
    "right_leg": {"rotation": [(0.0, Z), (0.25, [-22, 0, 0]), (0.5, [-6, 0, 0])]},
    "left_leg": {"rotation": [(0.0, Z), (0.25, [18, 0, 0]), (0.5, [6, 0, 0])]},
})
TP_EXECUTE = animation(1.0, {
    "torso": {"rotation": [(0.0, [10, 0, 0]), (0.25, [8, 0, 0]), (0.5, [-6, 0, 0]), (1.0, Z)],
              "position": [(0.0, Z), (0.5, [0, 0, 2]), (1.0, Z)]},
    "head": {"rotation": [(0.0, [6, 0, 0]), (0.5, [10, 0, 0]), (1.0, Z)]},
    "right_arm": {"rotation": [(0.0, [-105, 0, -4]), (0.12, [-108, 3, -4]), (0.25, [-102, -3, -4]),
                               (0.5, [-35, 0, -6]), (0.75, [-30, 0, -4]), (1.0, [-10, 0, 0])]},
    "left_arm": {"rotation": [(0.0, [-95, 0, 8]), (0.25, [-92, 0, 8]), (0.5, [-60, 0, 8]), (1.0, [-12, 0, 2])]},
    "right_leg": {"rotation": [(0.0, [-6, 0, 0]), (1.0, Z)]},
    "left_leg": {"rotation": [(0.0, [6, 0, 0]), (1.0, Z)]},
}, timeline={0.0: "impact;", 0.25: "react;", 0.5: "kill;"})
TP_FINISH = animation(0.5, {
    "torso": {"rotation": [(0.0, Z), (0.5, Z)]},
    "right_arm": {"rotation": [(0.0, [-10, 0, 0]), (0.5, Z)]},
    "left_arm": {"rotation": [(0.0, [-12, 0, 2]), (0.5, Z)]},
}, timeline={0.45: "finish;"})

# ---- first person (rig rest pose has the arms at -80 X; keyframes are OFFSETS from that rest pose)
FP_START = animation(0.5, {
    "right_arm": {"rotation": [(0.0, [30, 0, 0]), (0.15, [-12, 0, 0]), (0.3, [38, 0, -4]), (0.5, [-24, 0, -2])],
                  "position": [(0.0, [0, -3, 3]), (0.3, Z), (0.5, [0, 1, -5])]},
    "left_arm": {"rotation": [(0.0, [30, 0, 0]), (0.2, [-12, 0, 6]), (0.5, [-14, 0, 8])],
                 "position": [(0.0, [0, -3, 3]), (0.25, [0, 0, -2]), (0.5, [0, 0, -3])]},
    "camera": {"rotation": [(0.0, Z), (0.3, [3, 0, -1]), (0.5, [-2, 0, 0])],
               "position": [(0.0, Z), (0.3, [0, -1, -2]), (0.5, [0, 0, -4])]},
})
FP_EXECUTE = animation(1.0, {
    "right_arm": {"rotation": [(0.0, [-24, 0, -2]), (0.12, [-20, 2, -2]), (0.25, [-26, -2, -2]),
                               (0.5, [40, 0, -2]), (0.75, [26, 0, 0]), (1.0, [10, 0, 0])],
                  "position": [(0.0, [0, 1, -5]), (0.5, [0, -2, 2]), (1.0, [0, -1, 0])]},
    "left_arm": {"rotation": [(0.0, [-14, 0, 8]), (0.5, [-4, 0, 6]), (1.0, [4, 0, 2])],
                 "position": [(0.0, [0, 0, -3]), (0.5, [0, -1, 0]), (1.0, [0, -2, 2])]},
    "camera": {"rotation": [(0.0, [-2, 0, 0]), (0.25, [-3, 1, 1]), (0.5, [4, 0, -1.5]), (0.75, [1, 0, 0]), (1.0, Z)],
               "position": [(0.0, [0, 0, -4]), (0.5, [0, -1, -1]), (1.0, Z)]},
}, timeline={0.0: "impact;camera_shake:0.7:8;fov_punch:4;", 0.25: "react;",
             0.5: "kill;camera_shake:1.0:12;fov_punch:6;"})
FP_FINISH = animation(0.5, {
    "right_arm": {"rotation": [(0.0, [10, 0, 0]), (0.5, [30, 0, 0])], "position": [(0.0, [0, -1, 0]), (0.5, [0, -3, 3])]},
    "left_arm": {"rotation": [(0.0, [4, 0, 2]), (0.5, [30, 0, 0])], "position": [(0.0, [0, -2, 2]), (0.5, [0, -3, 3])]},
    "camera": {"rotation": [(0.0, Z), (0.5, Z)], "position": [(0.0, Z), (0.5, Z)]},
}, timeline={0.45: "finish;"})

# ---- zombie victim
ZV_START = animation(0.5, {
    "torso": {"rotation": [(0.0, [8, 0, 0]), (0.25, [15, 0, 0]), (0.5, [12, 0, 0])]},
    "head": {"rotation": [(0.0, [10, 0, 0]), (0.5, [18, 0, 0])]},
    "right_arm": {"rotation": [(0.0, [-40, 0, 0]), (0.25, [-70, 0, -10]), (0.5, [-85, 0, -8])]},
    "left_arm": {"rotation": [(0.0, [-40, 0, 0]), (0.25, [-70, 0, 10]), (0.5, [-85, 0, 8])]},
    "right_leg": {"rotation": [(0.0, Z), (0.5, [-8, 0, 0])]},
    "left_leg": {"rotation": [(0.0, Z), (0.5, [8, 0, 0])]},
}, timeline={0.0: "start;"})
ZV_EXECUTE = animation(1.0, {
    "head": {"rotation": [(0.0, [18, 0, 0]), (0.25, [-30, 0, 0]), (0.5, [-45, 0, 0]), (1.0, [-25, 0, 0])]},
    "torso": {"rotation": [(0.0, [12, 0, 0]), (0.25, [-6, 0, 0]), (0.5, [-14, 0, 0]), (1.0, [-8, 0, 0])]},
    "right_arm": {"rotation": [(0.0, [-85, 0, -8]), (0.25, [-100, 0, -20]), (0.5, [-140, 0, -35]), (1.0, [-20, 0, -10])]},
    "left_arm": {"rotation": [(0.0, [-85, 0, 8]), (0.25, [-100, 0, 20]), (0.5, [-140, 0, 35]), (1.0, [-20, 0, 10])]},
    "root": {"position": [(0.0, Z), (0.5, Z), (0.75, [0, 0, 3]), (1.0, [0, 0, 5])]},
    "right_leg": {"rotation": [(0.0, [-8, 0, 0]), (0.5, [-14, 0, 0]), (1.0, [-4, 0, 0])]},
    "left_leg": {"rotation": [(0.0, [8, 0, 0]), (0.5, [14, 0, 0]), (1.0, [4, 0, 0])]},
}, timeline={0.25: "react;", 0.5: "kill;"})
ZV_DEATH = animation(0.5, {
    "root": {"rotation": ([(0.0, Z), (0.5, [-90, 0, 0])], "easeInQuad"),
             "position": ([(0.0, [0, 0, 5]), (0.5, [0, 2, 8])], "easeInQuad")},
    # hold the pose the EXECUTE animation ended in, so there is no pop at the hand-over
    "torso": {"rotation": [(0.0, [-8, 0, 0]), (0.5, [-8, 0, 0])]},
    "right_leg": {"rotation": [(0.0, [-4, 0, 0]), (0.5, [-4, 0, 0])]},
    "left_leg": {"rotation": [(0.0, [4, 0, 0]), (0.5, [4, 0, 0])]},
    "head": {"rotation": [(0.0, [-25, 0, 0]), (0.5, [-10, 0, 0])]},
    "right_arm": {"rotation": [(0.0, [-20, 0, -10]), (0.5, [-40, 0, -25])]},
    "left_arm": {"rotation": [(0.0, [-20, 0, 10]), (0.5, [-40, 0, 25])]},
}, timeline={0.45: "finish;"})


# ============================================================================ generic moves
# Rig reminders (see the README):
#   first-person arms rest at X=-80, keyframes are OFFSETS:  X - = raise/forward, X + = lower/back
#   arm Y rotation sweeps SIDEWAYS (right arm: + = outward, left arm: - = outward); Z only twists a forward-pointing arm
#   position: -Z = forward, +Y = up, -X = to the right (camera and arms)
#   generic victim 'root' bone (feet pivot):  rotation X + = lean toward the player, - = lean back; position -Z = toward the player
#   every victim animation ENDS in the neutral pose so the vanilla death animation (the mob keels over) takes over seamlessly
O = [1, 1, 1]  # neutral scale


def mirror_y(points):
    """Mirror a [(t,[x,y,z])] track for the other arm (Y and Z swap sign)."""
    return [(t, [v[0], -v[1], -v[2]]) for t, v in points]


# ---------------------------------------------------------------- stab (victim only; players re-use the zombie animations)
STAB_V_START = animation(0.5, {"root": {"rotation": [(0.0, Z), (0.5, [10, 0, 0])],
                                        "position": [(0.0, Z), (0.5, [0, 1, -2])]}}, timeline={0.0: "start;"})
STAB_V_EXECUTE = animation(1.0, {"root": {
    "rotation": [(0.0, [10, 0, 0]), (0.25, [-12, 0, 0]), (0.5, [-22, 0, 0]), (1.0, [-14, 0, 0])],
    "position": [(0.0, [0, 1, -2]), (0.25, [0, 2, 3]), (0.5, [0, 3, 6]), (1.0, [0, 0, 5])]}},
    timeline={0.25: "react;", 0.5: "kill;"})
STAB_V_DEATH = animation(0.5, {"root": {"rotation": [(0.0, [-14, 0, 0]), (0.4, Z), (0.5, Z)],
                                        "position": [(0.0, [0, 0, 5]), (0.4, Z), (0.5, Z)]}}, timeline={0.45: "finish;"})

# ---------------------------------------------------------------- slam: both fists overhead, hammered down
# timeline: START 0.7s (arms up, then down - contact at the end = tick 14), EXECUTE 0.4s, FINISH 0.8s; lethal tick 18
_slam_arm_rot = [(0.0, [30, 0, 0]), (0.25, [-40, 0, 0]), (0.45, [-76, 0, 0]), (0.6, [-76, 0, 0]), (0.7, [4, 0, 0])]
_slam_arm_pos = [(0.0, [0, -3, 3]), (0.25, [0, -2, 0]), (0.45, [0, -2, -3]), (0.6, [0, -2, -3]), (0.7, [0, -3, -8])]
SLAM_FP_START = animation(0.7, {
    "right_arm": {"rotation": _slam_arm_rot, "position": _slam_arm_pos},
    "left_arm": {"rotation": _slam_arm_rot, "position": _slam_arm_pos},
    "camera": {"rotation": [(0.0, Z), (0.45, [-6, 0, 0]), (0.6, [-7, 0, 0]), (0.7, [10, 0, 0])],
               "position": [(0.0, Z), (0.45, [0, 2, 2]), (0.7, [0, -4, -6])]},
})
_slam_exec_rot = [(0.0, [4, 0, 0]), (0.1, [-4, 0, 0]), (0.2, [8, 0, 0]), (0.4, [2, 0, 0])]
_slam_exec_pos = [(0.0, [0, -3, -8]), (0.1, [0, -2, -6]), (0.2, [0, -3, -9]), (0.4, [0, -2, -4])]
SLAM_FP_EXECUTE = animation(0.4, {
    "right_arm": {"rotation": _slam_exec_rot, "position": _slam_exec_pos},
    "left_arm": {"rotation": _slam_exec_rot, "position": _slam_exec_pos},
    "camera": {"rotation": [(0.0, [10, 0, 0]), (0.2, [14, 0, 0]), (0.4, [6, 0, 0])],
               "position": [(0.0, [0, -4, -6]), (0.4, [0, -2, -3])]},
}, timeline={0.0: "impact;camera_shake:1.3:14;fov_punch:8;", 0.1: "react;", 0.2: "kill;camera_shake:1.0:12;fov_punch:5;"})
SLAM_FP_FINISH = animation(0.8, {
    "right_arm": {"rotation": [(0.0, [2, 0, 0]), (0.8, [30, 0, 0])], "position": [(0.0, [0, -2, -4]), (0.8, [0, -3, 3])]},
    "left_arm": {"rotation": [(0.0, [2, 0, 0]), (0.8, [30, 0, 0])], "position": [(0.0, [0, -2, -4]), (0.8, [0, -3, 3])]},
    "camera": {"rotation": [(0.0, [6, 0, 0]), (0.8, Z)], "position": [(0.0, [0, -2, -3]), (0.8, Z)]},
}, timeline={0.75: "finish;"})

SLAM_TP_START = animation(0.7, {
    "torso": {"rotation": [(0.0, Z), (0.4, [-16, 0, 0]), (0.6, [-20, 0, 0]), (0.7, [28, 0, 0])],
              "position": [(0.0, Z), (0.6, Z), (0.7, [0, -1, -3])]},
    "head": {"rotation": [(0.0, Z), (0.4, [-8, 0, 0]), (0.7, [14, 0, 0])]},
    "right_arm": {"rotation": [(0.0, Z), (0.3, [-165, 0, 0]), (0.6, [-170, 0, 0]), (0.7, [-35, 0, 0])]},
    "left_arm": {"rotation": [(0.0, Z), (0.3, [-165, 0, 0]), (0.6, [-170, 0, 0]), (0.7, [-35, 0, 0])]},
    "right_leg": {"rotation": [(0.0, Z), (0.6, [8, 0, 0]), (0.7, [-18, 0, 0])]},
    "left_leg": {"rotation": [(0.0, Z), (0.6, [-8, 0, 0]), (0.7, [14, 0, 0])]},
})
SLAM_TP_EXECUTE = animation(0.4, {
    "torso": {"rotation": [(0.0, [28, 0, 0]), (0.2, [32, 0, 0]), (0.4, [22, 0, 0])],
              "position": [(0.0, [0, -1, -3]), (0.4, [0, -1, -2])]},
    "head": {"rotation": [(0.0, [14, 0, 0]), (0.4, [10, 0, 0])]},
    "right_arm": {"rotation": [(0.0, [-35, 0, 0]), (0.2, [-28, 0, 0]), (0.4, [-30, 0, 0])]},
    "left_arm": {"rotation": [(0.0, [-35, 0, 0]), (0.2, [-28, 0, 0]), (0.4, [-30, 0, 0])]},
    "right_leg": {"rotation": [(0.0, [-18, 0, 0]), (0.4, [-14, 0, 0])]},
    "left_leg": {"rotation": [(0.0, [14, 0, 0]), (0.4, [12, 0, 0])]},
}, timeline={0.0: "impact;", 0.1: "react;", 0.2: "kill;"})
SLAM_TP_FINISH = animation(0.8, {
    "torso": {"rotation": [(0.0, [22, 0, 0]), (0.8, Z)], "position": [(0.0, [0, -1, -2]), (0.8, Z)]},
    "head": {"rotation": [(0.0, [10, 0, 0]), (0.8, Z)]},
    "right_arm": {"rotation": [(0.0, [-30, 0, 0]), (0.8, Z)]},
    "left_arm": {"rotation": [(0.0, [-30, 0, 0]), (0.8, Z)]},
    "right_leg": {"rotation": [(0.0, [-14, 0, 0]), (0.8, Z)]},
    "left_leg": {"rotation": [(0.0, [12, 0, 0]), (0.8, Z)]},
}, timeline={0.75: "finish;"})

# the squash lands exactly on tick 14 (END of START); a second crush on the lethal tick 18
SLAM_V_START = animation(0.7, {"root": {
    "rotation": [(0.0, Z), (0.2, [-3, 0, 2]), (0.4, [3, 0, -2]), (0.6, Z)],
    "scale": [(0.0, O), (0.6, O), (0.7, [1.18, 0.62, 1.18])]}}, timeline={0.0: "start;"})
SLAM_V_EXECUTE = animation(0.4, {"root": {
    "scale": [(0.0, [1.18, 0.62, 1.18]), (0.1, [1.1, 0.75, 1.1]), (0.2, [1.25, 0.5, 1.25]), (0.4, [1.12, 0.8, 1.12])],
    "rotation": [(0.0, Z), (0.2, [0, 0, 3]), (0.4, Z)]}}, timeline={0.1: "react;", 0.2: "kill;"})
SLAM_V_DEATH = animation(0.8, {"root": {
    "scale": [(0.0, [1.12, 0.8, 1.12]), (0.3, O), (0.8, O)]}}, timeline={0.75: "finish;"})

# ---------------------------------------------------------------- neck snap: both hands on the head, wrung round
# timeline: START 0.6s (reach the head), EXECUTE 0.7s (twist, SNAP at 0.5s = lethal tick 22), FINISH 0.8s
NECK_FP_START = animation(0.6, {
    "right_arm": {"rotation": [(0.0, [30, 0, 0]), (0.25, [-20, 0, 0]), (0.6, [-30, 0, 0])],
                  "position": [(0.0, [0, -3, 3]), (0.6, [2, 3, -5])]},
    "left_arm": {"rotation": [(0.0, [30, 0, 0]), (0.25, [-20, 0, 0]), (0.6, [-30, 0, 0])],
                 "position": [(0.0, [0, -3, 3]), (0.6, [-2, 3, -5])]},
    "camera": {"rotation": [(0.0, Z), (0.6, [2, 0, 0])], "position": [(0.0, Z), (0.6, [0, 0, -3])]},
})
_neck_r_rot = [(0.0, [-30, 0, 0]), (0.2, [-32, 10, 0]), (0.3, [-30, -22, 0]), (0.42, [-30, -38, 0]), (0.5, [-28, -52, 0]), (0.7, [-26, -46, 0])]
NECK_FP_EXECUTE = animation(0.7, {
    "right_arm": {"rotation": _neck_r_rot, "position": [(0.0, [2, 3, -5]), (0.7, [2, 3, -5])]},
    "left_arm": {"rotation": mirror_y(_neck_r_rot), "position": [(0.0, [-2, 3, -5]), (0.7, [-2, 3, -5])]},
    "camera": {"rotation": [(0.0, [2, 0, 0]), (0.2, [2, 0, 3]), (0.3, [2, 0, -4]), (0.5, [4, 0, -8]), (0.7, [3, 0, -6])],
               "position": [(0.0, [0, 0, -3]), (0.7, [0, 0, -3])]},
}, timeline={0.0: "impact;camera_shake:0.5:6;", 0.2: "react;camera_shake:0.4:6;", 0.5: "kill;camera_shake:1.1:12;fov_punch:6;"})
NECK_FP_FINISH = animation(0.8, {
    "right_arm": {"rotation": [(0.0, [-26, -46, 0]), (0.8, [30, 0, 0])], "position": [(0.0, [2, 3, -5]), (0.8, [0, -3, 3])]},
    "left_arm": {"rotation": [(0.0, [-26, 46, 0]), (0.8, [30, 0, 0])], "position": [(0.0, [-2, 3, -5]), (0.8, [0, -3, 3])]},
    "camera": {"rotation": [(0.0, [3, 0, -6]), (0.8, Z)], "position": [(0.0, [0, 0, -3]), (0.8, Z)]},
}, timeline={0.75: "finish;"})

NECK_TP_START = animation(0.6, {
    "torso": {"rotation": [(0.0, Z), (0.6, [14, 0, 0])]},
    "head": {"rotation": [(0.0, Z), (0.6, [8, 0, 0])]},
    "right_arm": {"rotation": [(0.0, Z), (0.25, [-110, 0, 0]), (0.6, [-115, -10, 0])]},
    "left_arm": {"rotation": [(0.0, Z), (0.25, [-110, 0, 0]), (0.6, [-115, 10, 0])]},
    "right_leg": {"rotation": [(0.0, Z), (0.6, [-8, 0, 0])]},
    "left_leg": {"rotation": [(0.0, Z), (0.6, [8, 0, 0])]},
})
NECK_TP_EXECUTE = animation(0.7, {
    "torso": {"rotation": [(0.0, [14, 0, 0]), (0.5, [16, -10, 0]), (0.7, [12, 0, 0])]},
    "head": {"rotation": [(0.0, [8, 0, 0]), (0.5, [10, 12, 0]), (0.7, [8, 0, 0])]},
    "right_arm": {"rotation": [(0.0, [-115, -10, 0]), (0.3, [-115, -26, 0]), (0.5, [-112, -44, 0]), (0.7, [-108, -38, 0])]},
    "left_arm": {"rotation": [(0.0, [-115, 10, 0]), (0.3, [-115, 26, 0]), (0.5, [-112, 44, 0]), (0.7, [-108, 38, 0])]},
    "right_leg": {"rotation": [(0.0, [-8, 0, 0]), (0.7, [-8, 0, 0])]},
    "left_leg": {"rotation": [(0.0, [8, 0, 0]), (0.7, [8, 0, 0])]},
}, timeline={0.0: "impact;", 0.2: "react;", 0.5: "kill;"})
NECK_TP_FINISH = animation(0.8, {
    "torso": {"rotation": [(0.0, [12, 0, 0]), (0.8, Z)]},
    "head": {"rotation": [(0.0, [8, 0, 0]), (0.8, Z)]},
    "right_arm": {"rotation": [(0.0, [-108, -38, 0]), (0.8, Z)]},
    "left_arm": {"rotation": [(0.0, [-108, 38, 0]), (0.8, Z)]},
    "right_leg": {"rotation": [(0.0, [-8, 0, 0]), (0.8, Z)]},
    "left_leg": {"rotation": [(0.0, [8, 0, 0]), (0.8, Z)]},
}, timeline={0.75: "finish;"})

NECK_V_START = animation(0.6, {"root": {"rotation": [(0.0, Z), (0.6, [6, 0, 0])],
                                        "position": [(0.0, Z), (0.6, [0, 2, -2])]}}, timeline={0.0: "start;"})
NECK_V_EXECUTE = animation(0.7, {"root": {
    "rotation": [(0.0, [6, 0, 0]), (0.2, [8, -12, 0]), (0.3, [8, 25, 0]), (0.42, [4, 40, 3]), (0.5, [0, -70, 6]), (0.7, [0, -60, 5])],
    "position": [(0.0, [0, 2, -2]), (0.5, [0, 5, -1]), (0.7, [0, 3, 0])]}}, timeline={0.2: "react;", 0.5: "kill;"})
NECK_V_DEATH = animation(0.8, {"root": {
    "rotation": [(0.0, [0, -60, 5]), (0.3, Z), (0.8, Z)],
    "position": [(0.0, [0, 3, 0]), (0.3, Z), (0.8, Z)]}}, timeline={0.75: "finish;"})

# ---------------------------------------------------------------- uppercut: crouch, launch, the mob spins up and drops
# timeline: START 0.4s (wind-up, contact at tick 8), EXECUTE 0.9s (airborne; back on the ground at ~tick 24), FINISH 0.9s
UPPER_FP_START = animation(0.4, {
    "right_arm": {"rotation": [(0.0, [30, 0, 0]), (0.2, [70, 0, 0]), (0.4, [-70, 0, 0])],
                  "position": [(0.0, [0, -3, 3]), (0.2, [0, -8, 6]), (0.4, [0, -4, -6])]},
    "left_arm": {"rotation": [(0.0, [30, 0, 0]), (0.2, [40, 0, 0]), (0.4, [10, 0, 0])],
                 "position": [(0.0, [0, -3, 3]), (0.2, [0, -6, 4]), (0.4, [0, 0, -2])]},
    "camera": {"rotation": [(0.0, Z), (0.2, [8, 0, 0]), (0.4, [-10, 0, 0])],
               "position": [(0.0, Z), (0.2, [0, -5, 2]), (0.4, [0, 3, -4])]},
})
UPPER_FP_EXECUTE = animation(0.9, {
    "right_arm": {"rotation": [(0.0, [-70, 0, 0]), (0.3, [-80, 0, 0]), (0.6, [-40, 0, 0]), (0.9, [-10, 0, 0])],
                  "position": [(0.0, [0, -4, -6]), (0.3, [0, 0, -8]), (0.6, [0, -1, -4]), (0.9, Z)]},
    "left_arm": {"rotation": [(0.0, [10, 0, 0]), (0.9, [0, 0, 0])], "position": [(0.0, [0, 0, -2]), (0.9, Z)]},
    "camera": {"rotation": [(0.0, [-10, 0, 0]), (0.1, [-16, 0, 0]), (0.5, [-18, 0, 2]), (0.9, [-6, 0, 0])],
               "position": [(0.0, [0, 3, -4]), (0.3, [0, 5, -2]), (0.9, Z)]},
}, timeline={0.0: "impact;camera_shake:1.2:12;fov_punch:9;", 0.1: "react;", 0.8: "kill;camera_shake:0.8:10;"})
UPPER_FP_FINISH = animation(0.9, {
    "right_arm": {"rotation": [(0.0, [-10, 0, 0]), (0.9, [30, 0, 0])], "position": [(0.0, Z), (0.9, [0, -3, 3])]},
    "left_arm": {"rotation": [(0.0, Z), (0.9, [30, 0, 0])], "position": [(0.0, Z), (0.9, [0, -3, 3])]},
    "camera": {"rotation": [(0.0, [-6, 0, 0]), (0.9, Z)], "position": [(0.0, Z), (0.9, Z)]},
}, timeline={0.85: "finish;"})

UPPER_TP_START = animation(0.4, {
    "torso": {"rotation": [(0.0, Z), (0.2, [22, 0, 0]), (0.4, [-10, 0, 0])],
              "position": [(0.0, Z), (0.2, [0, -4, 0]), (0.4, [0, 1, -1])]},
    "head": {"rotation": [(0.0, Z), (0.4, [-8, 0, 0])]},
    "right_arm": {"rotation": [(0.0, Z), (0.2, [25, 0, 0]), (0.4, [-165, 0, 0])]},
    "left_arm": {"rotation": [(0.0, Z), (0.2, [15, 0, 0]), (0.4, [-60, 0, 0])]},
    "right_leg": {"rotation": [(0.0, Z), (0.2, [-34, 0, 0]), (0.4, [-6, 0, 0])]},
    "left_leg": {"rotation": [(0.0, Z), (0.2, [28, 0, 0]), (0.4, [6, 0, 0])]},
})
UPPER_TP_EXECUTE = animation(0.9, {
    "torso": {"rotation": [(0.0, [-10, 0, 0]), (0.9, Z)], "position": [(0.0, [0, 1, -1]), (0.9, Z)]},
    "head": {"rotation": [(0.0, [-8, 0, 0]), (0.9, Z)]},
    "right_arm": {"rotation": [(0.0, [-165, 0, 0]), (0.4, [-150, 0, 0]), (0.9, [-30, 0, 0])]},
    "left_arm": {"rotation": [(0.0, [-60, 0, 0]), (0.9, [-10, 0, 0])]},
    "right_leg": {"rotation": [(0.0, [-6, 0, 0]), (0.9, Z)]},
    "left_leg": {"rotation": [(0.0, [6, 0, 0]), (0.9, Z)]},
}, timeline={0.0: "impact;", 0.1: "react;", 0.8: "kill;"})
UPPER_TP_FINISH = animation(0.9, {
    "right_arm": {"rotation": [(0.0, [-30, 0, 0]), (0.9, Z)]},
    "left_arm": {"rotation": [(0.0, [-10, 0, 0]), (0.9, Z)]},
}, timeline={0.85: "finish;"})

# the mob is thrown up (position Y in pixels: 28px = 1.75 blocks), spinning about its vertical axis; back down by ~tick 24
UPPER_V_START = animation(0.4, {"root": {"rotation": [(0.0, Z), (0.4, [3, 0, 0])]}}, timeline={0.0: "start;"})
UPPER_V_EXECUTE = animation(0.9, {"root": {
    "position": [(0.0, Z), (0.25, [0, 22, -2]), (0.45, [0, 28, 0]), (0.7, [0, 14, 3]), (0.9, [0, 2, 3])],
    "rotation": [(0.0, [3, 0, 0]), (0.45, [-12, 270, 0]), (0.9, [0, 540, 0])]}}, timeline={0.1: "react;", 0.8: "kill;"})
UPPER_V_DEATH = animation(0.9, {"root": {
    "rotation": [(0.0, [0, 540, 0]), (0.4, [0, 720, 0]), (0.9, [0, 720, 0])],
    "position": [(0.0, [0, 2, 3]), (0.3, Z), (0.9, Z)]}}, timeline={0.85: "finish;"})

# ---------------------------------------------------------------- rip: grab both sides and pull apart
# timeline: START 0.5s (grab), EXECUTE 1.0s (strain, shake; torn apart at 0.8s = lethal tick 26), FINISH 0.8s
_rip_r_rot = [(0.0, [-12, 4, 0]), (0.15, [-10, 8, 0]), (0.3, [-14, 13, 0]), (0.45, [-10, 18, 0]), (0.6, [-14, 24, 0]),
              (0.75, [-10, 28, 0]), (0.8, [-8, 38, 0]), (1.0, [-6, 34, 0])]
_rip_r_pos = [(0.0, [0, 0, -4]), (0.75, [-2, 0, -4]), (0.8, [-6, 0, -5]), (1.0, [-5, 0, -4])]
RIP_FP_START = animation(0.5, {
    "right_arm": {"rotation": [(0.0, [30, 0, 0]), (0.3, [-18, 22, 0]), (0.5, [-12, 4, 0])],
                  "position": [(0.0, [0, -3, 3]), (0.3, [-2, 0, -3]), (0.5, [0, 0, -4])]},
    "left_arm": {"rotation": [(0.0, [30, 0, 0]), (0.3, [-18, -22, 0]), (0.5, [-12, -4, 0])],
                 "position": [(0.0, [0, -3, 3]), (0.3, [2, 0, -3]), (0.5, [0, 0, -4])]},
    "camera": {"rotation": [(0.0, Z), (0.5, [3, 0, 0])], "position": [(0.0, Z), (0.5, [0, -1, -3])]},
})
RIP_FP_EXECUTE = animation(1.0, {
    "right_arm": {"rotation": _rip_r_rot, "position": _rip_r_pos},
    "left_arm": {"rotation": mirror_y(_rip_r_rot), "position": [(t, [-v[0], v[1], v[2]]) for t, v in _rip_r_pos]},
    "camera": {"rotation": [(0.0, [3, 0, 0]), (0.2, [3, 0, 1.5]), (0.4, [3, 0, -1.5]), (0.6, [4, 0, 2]), (0.8, [6, 0, 0]), (1.0, [3, 0, 0])],
               "position": [(0.0, [0, -1, -3]), (0.8, [0, -1, -5]), (1.0, [0, -1, -3])]},
}, timeline={0.0: "impact;camera_shake:0.6:10;", 0.4: "react;camera_shake:0.8:10;", 0.8: "kill;camera_shake:1.4:16;fov_punch:10;"})
RIP_FP_FINISH = animation(0.8, {
    "right_arm": {"rotation": [(0.0, [-6, 34, 0]), (0.8, [30, 0, 0])], "position": [(0.0, [-5, 0, -4]), (0.8, [0, -3, 3])]},
    "left_arm": {"rotation": [(0.0, [-6, -34, 0]), (0.8, [30, 0, 0])], "position": [(0.0, [5, 0, -4]), (0.8, [0, -3, 3])]},
    "camera": {"rotation": [(0.0, [3, 0, 0]), (0.8, Z)], "position": [(0.0, [0, -1, -3]), (0.8, Z)]},
}, timeline={0.75: "finish;"})

RIP_TP_START = animation(0.5, {
    "torso": {"rotation": [(0.0, Z), (0.5, [10, 0, 0])]},
    "right_arm": {"rotation": [(0.0, Z), (0.3, [-95, 18, 0]), (0.5, [-92, 8, 0])]},
    "left_arm": {"rotation": [(0.0, Z), (0.3, [-95, -18, 0]), (0.5, [-92, -8, 0])]},
    "right_leg": {"rotation": [(0.0, Z), (0.5, [-10, 0, 0])]},
    "left_leg": {"rotation": [(0.0, Z), (0.5, [10, 0, 0])]},
})
_rip_tp_r = [(0.0, [-92, 8, 0]), (0.2, [-90, 14, 0]), (0.4, [-92, 22, 0]), (0.6, [-90, 30, 0]), (0.78, [-92, 38, 0]), (0.8, [-88, 62, 0]), (1.0, [-85, 52, 0])]
RIP_TP_EXECUTE = animation(1.0, {
    "torso": {"rotation": [(0.0, [10, 0, 0]), (0.4, [12, 0, 0]), (0.8, [18, 0, 0]), (1.0, [12, 0, 0])]},
    "right_arm": {"rotation": _rip_tp_r},
    "left_arm": {"rotation": mirror_y(_rip_tp_r)},
    "right_leg": {"rotation": [(0.0, [-10, 0, 0]), (1.0, [-10, 0, 0])]},
    "left_leg": {"rotation": [(0.0, [10, 0, 0]), (1.0, [10, 0, 0])]},
}, timeline={0.0: "impact;", 0.4: "react;", 0.8: "kill;"})
RIP_TP_FINISH = animation(0.8, {
    "torso": {"rotation": [(0.0, [12, 0, 0]), (0.8, Z)]},
    "right_arm": {"rotation": [(0.0, [-85, 52, 0]), (0.8, Z)]},
    "left_arm": {"rotation": [(0.0, [-85, -52, 0]), (0.8, Z)]},
    "right_leg": {"rotation": [(0.0, [-10, 0, 0]), (0.8, Z)]},
    "left_leg": {"rotation": [(0.0, [10, 0, 0]), (0.8, Z)]},
}, timeline={0.75: "finish;"})

# stretched thin while being pulled (scale Y up, X/Z down), bursts flat on the lethal tick, then relaxes
RIP_V_START = animation(0.5, {"root": {"rotation": [(0.0, Z), (0.5, [4, 0, 0])],
                                       "position": [(0.0, Z), (0.5, [0, 1, -1])]}}, timeline={0.0: "start;"})
RIP_V_EXECUTE = animation(1.0, {"root": {
    "scale": [(0.0, O), (0.3, [0.92, 1.1, 0.92]), (0.6, [0.8, 1.25, 0.8]), (0.78, [0.7, 1.38, 0.7]), (0.8, [1.45, 0.3, 1.45]), (1.0, [1.3, 0.45, 1.3])],
    "rotation": [(0.0, [4, 0, 0]), (0.1, [4, 0, 3]), (0.2, [4, 0, -3]), (0.3, [4, 0, 3]), (0.4, [4, 0, -4]), (0.5, [4, 0, 4]),
                 (0.6, [4, 0, -5]), (0.7, [4, 0, 5]), (0.78, [4, 0, -6]), (0.8, Z), (1.0, Z)],
    "position": [(0.0, [0, 1, -1]), (0.6, [0, 3, 0]), (0.78, [0, 6, 0]), (0.8, Z)]}}, timeline={0.4: "react;", 0.8: "kill;"})
RIP_V_DEATH = animation(0.8, {"root": {"scale": [(0.0, [1.3, 0.45, 1.3]), (0.3, O), (0.8, O)]}}, timeline={0.75: "finish;"})


def move_files(name, fp, tp, victim):
    """Write the three animation files of a generic move."""
    prefix = f"player_glory_kill_{name}_"
    write_json(ASSETS / f"animations/{name}_player_first_person.animation.json", animations({
        prefix + "start": fp[0], prefix + "execute": fp[1], prefix + "finish": fp[2]}))
    write_json(ASSETS / f"animations/{name}_player.animation.json", animations({
        prefix + "start": tp[0], prefix + "execute": tp[1], prefix + "finish": tp[2]}))
    write_victim(name, victim)


def write_victim(name, victim):
    vprefix = f"victim_glory_kill_{name}_"
    write_json(ASSETS / f"animations/{name}_victim.animation.json", animations({
        vprefix + "start": victim[0], vprefix + "execute": victim[1], vprefix + "death": victim[2]}))


def build_png():
    try:
        from PIL import Image, ImageDraw
    except ImportError:
        print("Pillow not installed - skipping textures/glory_kill/zombie.png")
        return
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    skin, skin_dark = (96, 143, 76, 255), (74, 114, 58, 255)
    shirt, pants, eye, mouth = (46, 124, 140, 255), (44, 44, 120, 255), (20, 20, 20, 255), (50, 25, 25, 255)

    def box(u, v, w, h, dpt, color, dark=None):
        faces = {
            "top": (u + dpt, v, w, dpt), "bottom": (u + dpt + w, v, w, dpt),
            "right": (u, v + dpt, dpt, h), "front": (u + dpt, v + dpt, w, h),
            "left": (u + dpt + w, v + dpt, dpt, h), "back": (u + dpt + w + dpt, v + dpt, w, h),
        }
        for name, (x, y, fw, fh) in faces.items():
            c = dark if (dark and name in ("bottom", "back", "right")) else color
            d.rectangle([x, y, x + fw - 1, y + fh - 1], fill=c)

    box(0, 0, 8, 8, 8, skin, skin_dark)       # head
    box(16, 16, 8, 12, 4, shirt)              # body
    box(40, 16, 4, 12, 4, skin, skin_dark)    # arms (left arm re-uses this, mirrored)
    d.rectangle([40, 20, 55, 24], fill=shirt)  # sleeves
    box(0, 16, 4, 12, 4, pants)               # legs (left leg re-uses this, mirrored)
    d.rectangle([9, 11, 10, 12], fill=eye)     # eyes (front face of head is x 8..15, y 8..15)
    d.rectangle([13, 11, 14, 12], fill=eye)
    d.rectangle([10, 14, 13, 14], fill=mouth)
    path = ASSETS / "textures/glory_kill/zombie.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def main():
    for slim, suffix in ((False, ""), (True, "_slim")):
        write_json(ASSETS / f"geo/glory_kill_player{suffix}.geo.json",
                   humanoid(f"geometry.glorykill.player{suffix}", slim=slim))
        write_json(ASSETS / f"geo/glory_kill_player_first_person{suffix}.geo.json",
                   first_person(f"geometry.glorykill.player_first_person{suffix}", slim=slim))
    write_json(ASSETS / "geo/glory_kill_zombie.geo.json",
               humanoid("geometry.glorykill.zombie", overlays=False, zombie=True))
    write_json(ASSETS / "animations/zombie_player.animation.json", animations({
        "player_glory_kill_start": TP_START, "player_glory_kill_execute": TP_EXECUTE, "player_glory_kill_finish": TP_FINISH}))
    write_json(ASSETS / "animations/zombie_player_first_person.animation.json", animations({
        "player_glory_kill_start": FP_START, "player_glory_kill_execute": FP_EXECUTE, "player_glory_kill_finish": FP_FINISH}))
    write_json(ASSETS / "animations/zombie_victim.animation.json", animations({
        "zombie_glory_kill_start": ZV_START, "zombie_glory_kill_execute": ZV_EXECUTE, "zombie_glory_kill_death": ZV_DEATH}))
    write_json(ASSETS / "geo/glory_kill_generic.geo.json", generic_rig("geometry.glorykill.generic"))
    write_victim("stab", (STAB_V_START, STAB_V_EXECUTE, STAB_V_DEATH))  # stab re-uses the zombie player animations
    move_files("slam", (SLAM_FP_START, SLAM_FP_EXECUTE, SLAM_FP_FINISH), (SLAM_TP_START, SLAM_TP_EXECUTE, SLAM_TP_FINISH),
               (SLAM_V_START, SLAM_V_EXECUTE, SLAM_V_DEATH))
    move_files("neck_snap", (NECK_FP_START, NECK_FP_EXECUTE, NECK_FP_FINISH), (NECK_TP_START, NECK_TP_EXECUTE, NECK_TP_FINISH),
               (NECK_V_START, NECK_V_EXECUTE, NECK_V_DEATH))
    move_files("uppercut", (UPPER_FP_START, UPPER_FP_EXECUTE, UPPER_FP_FINISH), (UPPER_TP_START, UPPER_TP_EXECUTE, UPPER_TP_FINISH),
               (UPPER_V_START, UPPER_V_EXECUTE, UPPER_V_DEATH))
    move_files("rip", (RIP_FP_START, RIP_FP_EXECUTE, RIP_FP_FINISH), (RIP_TP_START, RIP_TP_EXECUTE, RIP_TP_FINISH),
               (RIP_V_START, RIP_V_EXECUTE, RIP_V_DEATH))
    build_png()
    print("assets generated in", ASSETS)


if __name__ == "__main__":
    main()
