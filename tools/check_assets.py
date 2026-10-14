#!/usr/bin/env python3
"""
Cross-checks the Java move definitions against the GeckoLib assets, so a typo in an animation name or a retimed
Blockbench export is caught before you start the game:

    python3 tools/check_assets.py

For every move (zombie + the generic moves) it verifies:
  * the model / animation files exist and contain the animation names the definition asks for
  * every bone an animation moves exists in the rig it is played on
  * the phase lengths match the server timeline (START / EXECUTE / FINISH)
  * `impact;` / `react;` / `kill;` keyframes fall on the timeline's impact / victimReact / lethal ticks
  * neighbouring phases join without a visible pop (end of phase N == start of phase N+1)
  * body (generic) victim animations END in the neutral pose, so the vanilla death animation can take over
  * keyframe times are sorted and inside the animation, instruction keyframes use known commands
Exit code 1 if there is any error; warnings do not fail the check.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFS = ROOT / "src/main/java/com/example/glorykill/common/definitions"
ASSETS = ROOT / "src/main/resources/assets/glorykill"

errors, warnings = [], []


def err(move, msg):
    errors.append(f"[{move}] {msg}")


def warn(move, msg):
    warnings.append(f"[{move}] {msg}")


# --------------------------------------------------------------------------- read the Java definitions
def parse_definitions():
    moves = []
    generic = (DEFS / "GenericGloryKills.java").read_text()
    for m in re.finditer(r"static GloryKillDefinition (\w+)\(\) \{(.*?)\n    \}\n", generic, re.S):
        body = m.group(2)
        name = re.search(r'id\("(\w+)"\)', body).group(1)
        timeline = [int(x) for x in re.search(r"GloryKillTimeline\(([^)]*)\)", body).group(1).replace(" ", "").split(",")]
        pm = re.search(r'playerSet\("(\w+)"\)', body)
        if pm:
            p = pm.group(1)
            player = [f"player_glory_kill_{p}_{ph}" for ph in ("start", "execute", "finish")]
        else:
            player = list(re.search(r'\.playerAnimations\(new AnimationSet\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)"\)', body).groups())
        v = re.search(r'victimSet\("(\w+)"\)', body).group(1)
        victim = [f"victim_glory_kill_{v}_{ph}" for ph in ("start", "execute", "death")]
        pf, vf = re.search(r'GloryKillAssets\.generic\("(\w+)",\s*"(\w+)"\)', body).groups()
        moves.append(dict(name=name, timeline=timeline, player=player, victim=victim, player_files=pf, victim_files=vf,
                          mode="BODY", victim_rig="geo/glory_kill_generic.geo.json"))
    zombie = (DEFS / "ZombieGloryKill.java").read_text()
    timeline = [int(x) for x in re.search(r"GloryKillTimeline\(([^)]*)\)", zombie).group(1).replace(" ", "").split(",")]
    player = list(re.search(r"\.playerAnimations\(new AnimationSet\(\"([^\"]+)\",\s*\"([^\"]+)\",\s*\"([^\"]+)\"\)", zombie).groups())
    victim = list(re.search(r"\.victimAnimations\(new AnimationSet\(\"([^\"]+)\",\s*\"([^\"]+)\",\s*\"([^\"]+)\"\)", zombie).groups())
    moves.append(dict(name="zombie", timeline=timeline, player=player, victim=victim, player_files="zombie",
                      victim_files="zombie", mode="MODEL", victim_rig="geo/glory_kill_zombie.geo.json"))
    return moves


def load(path):
    p = ASSETS / path
    if not p.exists():
        return None
    return json.loads(p.read_text())


def bones_of(geo):
    return {b["name"] for b in geo["minecraft:geometry"][0]["bones"]}


# --------------------------------------------------------------------------- animation helpers
DEFAULT = {"rotation": [0, 0, 0], "position": [0, 0, 0], "scale": [1, 1, 1]}
TOL = {"rotation": 1.5, "position": 0.6, "scale": 0.02}


def vec(v):
    return v["vector"] if isinstance(v, dict) else v


def end_value(anim, bone, channel):
    kf = anim.get("bones", {}).get(bone, {}).get(channel)
    if not kf:
        return DEFAULT[channel]
    return vec(kf[max(kf, key=float)])


def start_value(anim, bone, channel):
    kf = anim.get("bones", {}).get(bone, {}).get(channel)
    if not kf:
        return DEFAULT[channel]
    return vec(kf[min(kf, key=float)])


def norm_rot(v):  # a full turn is the same pose
    return [((x + 180) % 360) - 180 for x in v]


KNOWN = {"impact", "react", "kill", "finish", "start", "camera_shake", "fov_punch"}


def check_anim(move, label, anim, rig_bones):
    length = anim["animation_length"]
    for bone, channels in anim.get("bones", {}).items():
        if bone not in rig_bones:
            err(move, f"{label}: animates bone '{bone}' which is not in the rig {sorted(rig_bones)}")
        for channel, kf in channels.items():
            times = [float(t) for t in kf]
            if times != sorted(times):
                err(move, f"{label}: {bone}.{channel} keyframes are not sorted")
            if times and max(times) > length + 1e-6:
                err(move, f"{label}: {bone}.{channel} has a keyframe at {max(times)}s after the end ({length}s)")
            for t, v in kf.items():
                if len(vec(v)) != 3:
                    err(move, f"{label}: {bone}.{channel}@{t} must have 3 numbers")
    for t, instr in anim.get("timeline", {}).items():
        if float(t) > length + 1e-6:
            err(move, f"{label}: instruction at {t}s is after the end ({length}s)")
        for part in str(instr).split(";"):
            part = part.strip()
            if part and part.split(":")[0] not in KNOWN:
                warn(move, f"{label}: unknown instruction '{part}'")


def instruction_ticks(anim, name):
    out = []
    for t, instr in anim.get("timeline", {}).items():
        if name in [p.strip().split(":")[0] for p in str(instr).split(";")]:
            out.append(float(t) * 20.0)
    return out


def check_move(m):
    name = m["name"]
    exec_start, impact, react, lethal, finish_phase, end = m["timeline"]
    expected = [exec_start / 20.0, (finish_phase - exec_start) / 20.0, (end - finish_phase) / 20.0]

    tracks = {
        "first-person": (f"animations/{m['player_files']}_player_first_person.animation.json", m["player"],
                         "geo/glory_kill_player_first_person.geo.json"),
        "third-person": (f"animations/{m['player_files']}_player.animation.json", m["player"],
                         "geo/glory_kill_player.geo.json"),
        "victim": (f"animations/{m['victim_files']}_victim.animation.json", m["victim"], m["victim_rig"]),
    }
    loaded = {}
    for label, (afile, names, rig) in tracks.items():
        anims, geo = load(afile), load(rig)
        if anims is None:
            err(name, f"{label}: missing animation file {afile}")
            continue
        if geo is None:
            err(name, f"{label}: missing model {rig}")
            continue
        rig_bones = bones_of(geo)
        seq = []
        for phase, anim_name in zip(("start", "execute", "finish/death"), names):
            anim = anims["animations"].get(anim_name)
            if anim is None:
                err(name, f"{label}: {afile} has no animation '{anim_name}' (has {sorted(anims['animations'])})")
                seq.append(None)
                continue
            seq.append(anim)
            check_anim(name, f"{label} {anim_name}", anim, rig_bones)
        loaded[label] = seq
        if all(seq):
            # phase lengths vs the server timeline
            for phase, anim, want in zip(("START", "EXECUTE", "FINISH"), seq, expected):
                if abs(anim["animation_length"] - want) > 0.06:
                    warn(name, f"{label} {phase}: animation is {anim['animation_length']}s but the timeline gives it {want}s "
                               f"(it will be cut off / hold its last frame - update the timeline numbers or the animation)")
            # no pop between phases
            for (a, b, pa, pb) in ((seq[0], seq[1], "START", "EXECUTE"), (seq[1], seq[2], "EXECUTE", "FINISH")):
                bones = set(a.get("bones", {})) | set(b.get("bones", {}))
                for bone in sorted(bones):
                    for ch in ("rotation", "position", "scale"):
                        ea, sb = end_value(a, bone, ch), start_value(b, bone, ch)
                        if ch == "rotation":
                            ea, sb = norm_rot(ea), norm_rot(sb)
                        if any(abs(x - y) > TOL[ch] for x, y in zip(ea, sb)):
                            warn(name, f"{label}: {bone}.{ch} jumps {pa}->{pb}: {ea} -> {sb}")
            # keyframe events line up with the server timeline
            for instr, tick in (("impact", impact), ("react", react), ("kill", lethal)):
                for t in instruction_ticks(seq[1], instr):
                    at = exec_start + t
                    if abs(at - tick) > 1.01:
                        warn(name, f"{label}: '{instr};' keyframe falls on tick {at:.0f} but the timeline says {tick}")

    # body victims must finish neutral so the vanilla death animation continues seamlessly
    if m["mode"] == "BODY" and "victim" in loaded and loaded["victim"][2]:
        death = loaded["victim"][2]
        for ch in ("rotation", "position", "scale"):
            v = end_value(death, "root", ch)
            v = norm_rot(v) if ch == "rotation" else v
            if any(abs(x - y) > TOL[ch] for x, y in zip(v, DEFAULT[ch])):
                err(name, f"victim death animation ends with root.{ch} = {v}, it must end neutral {DEFAULT[ch]}")
    return expected


def main():
    moves = parse_definitions()
    print(f"checking {len(moves)} moves: {', '.join(m['name'] for m in moves)}\n")
    for m in moves:
        exp = check_move(m)
        print(f"  {m['name']:<10} {m['mode']:<5} timeline {tuple(m['timeline'])}  phases {exp[0]:.2f}s / {exp[1]:.2f}s / {exp[2]:.2f}s")
    # shared textures referenced by the generic rig
    if not (ASSETS / "textures/glory_kill/zombie.png").exists():
        err("assets", "textures/glory_kill/zombie.png is missing (used as the placeholder texture of the generic rig)")
    print()
    for w in warnings:
        print("WARNING ", w)
    for e in errors:
        print("ERROR   ", e)
    print(f"\n{len(errors)} error(s), {len(warnings)} warning(s)")
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
