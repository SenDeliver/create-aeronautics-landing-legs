# Landing Legs save format (frozen in phase 1, extended in phases 2 and 3)

These strings are stored in worlds. Renaming any of them breaks existing worlds.
Code constants: `src/main/java/dev/legworks/LegworksIds.java`.

## Registry names

| Kind | ID | Status |
|---|---|---|
| Block | `legworks:simple_leg_mount` | registered (phase 1) |
| Block | `legworks:hydraulic_leg_mount` | registered (phase 2) |
| Block | `legworks:leg_controller` | registered (phase 2) |
| Item (block item) | `legworks:simple_leg_mount` | registered |
| Item (block item) | `legworks:hydraulic_leg_mount`, `legworks:leg_controller` | registered (phase 2) |
| Item | `legworks:short_limb`, `legworks:long_limb`, `legworks:heavy_limb` | registered |
| Block entity type | `legworks:simple_leg_mount` | registered |
| Block entity type | `legworks:hydraulic_leg_mount`, `legworks:leg_controller` | registered (phase 2) |
| Item | `legworks:incomplete_hydraulic_leg_mount` | registered (phase 3). Sequenced Assembly transitional item (Create `SequencedAssemblyItem`); can sit in chests, on belts and depots. Not in the creative tab. |
| Display source (Create registry `create:display_source`) | `legworks:leg_controller_status`, `legworks:leg_mount_status` | registered (phase 3). A Display Link saves the chosen source id in its NBT (`Source.Id`), so these are frozen. |
| Data component | `legworks:limb` | registered |
| Data component | `legworks:link_selection` | registered (phase 2). Network-only, **not saved** in worlds (the Leg Controller item's current Linking selection). The registry name is still synced to clients, so keep it. |

Not saved in worlds (safe to change): creative tab `legworks:main`, lang keys, textures and models, the config file name `legworks-server.toml` (renaming only resets values to defaults), recipe ids (`legworks:crafting/*`, `legworks:mechanical_crafting/*`, `legworks:sequenced_assembly/*`; only recipe books remember them), the JEI plugin/category ids (`legworks:jei_plugin`, `legworks:limb_fitting`), the Jade provider id `legworks:leg_status` (Jade's client config only), Ponder scene and tag ids (`legworks:legs`).

Block states:

- `simple_leg_mount`: `facing` (down/up/north/south/west/east, default `down`).
- `hydraulic_leg_mount` (phase 2): `facing` (same values, default `down`) and `axis_along_first` (true/false; Create's `DirectionalAxisKineticBlock` property, picks which of the two axes across `facing` the shaft runs along).
- `leg_controller` (phase 2): no properties.

## `legworks:limb` data component

Any item carrying it is a Limb. Codec field names are frozen:

| Field | Type | Meaning |
|---|---|---|
| `reach` | float | maximum Reach, blocks |
| `nominal_rest` | float | default rest length (Simple Leg Ride Height) |
| `min_rest` | float | shortest rest Leveling and Crouch may use |
| `stiffness` | float | base Stiffness multiplier |

The three Limb items carry it as a default component, so item NBT does not store it; changing the default values in a later version applies to existing items.

## Block entity NBT

| Key | Type | Where | Meaning |
|---|---|---|---|
| `Limb` | compound (full `ItemStack`, `ItemStack.save`) | every Leg Mount | fitted Limb; absent = no Limb |
| `Rest` | double | every Leg Mount | current rest length; absent = start at Ride Height |
| `Settings` | compound | Hydraulic Leg Mount | value boxes, keys below |
| `Settings.Stiffness` | double | Hydraulic Leg Mount | Stiffness multiplier, 0.25–2.0 (value box 25–200 %), default 1.0 |
| `Settings.Damping` | double | Hydraulic Leg Mount | Damping ratio, 0.2–1.5 (steps of 0.1), default 0.8 |
| `Controller` | int array `[dx, dy, dz]` | Hydraulic Leg Mount | offset from this Mount to its Leg Controller; absent = not linked |
| `Fold` | double 0–1 (phase 2, new) | Hydraulic Leg Mount | Gear Up fold: 0 = Gear Down, 1 = folded |
| `Softening` | double 0–1 (phase 2, new) | Hydraulic Leg Mount | current Softening (0 = none, 1 = full) |
| `Links` | list of int arrays `[dx, dy, dz]` | Leg Controller | offsets from the Controller to each Linked Leg Mount (at most `maxLegsPerController`) |
| `RideHeight` | double | Leg Controller | Ride Height in blocks, steps of 0.1, 0 up to the longest Linked Limb's Reach (6.0 while no Linked Leg has a Limb, never above 16); **0 = Auto** (each Leg uses its Limb's nominal rest) |

Links are stored on both ends as offsets. They survive Sable assembly and splitting (pure translation). Simulated disassembly and merging can rotate the blocks, so both blocks implement Sable's `BlockSubLevelAssemblyListener` and rebuild the offsets from the real new positions after every move (`LinkMoves`). A Link whose other end did not move with it is dropped.

Also written by Create's `KineticBlockEntity` on the Hydraulic Leg Mount (not ours, don't reuse): `Speed`, `Sequence`, `NeedsSpeedUpdate`, `Source`, `Network`.

Sent only to clients (never saved): `GearUp` (Mount, Controller), `Conflict` (Controller), `RideHeightMax` (Controller, the current Ride Height scroll cap in tenths).
Phase 3 adds (client packet only, for goggles and Jade): on every Leg Mount `Status` {`Grounded` bool, `Overreach` bool, `Load` float (share of the Ship's weight), `Reach` float}; on the Hydraulic Leg Mount `Linked` bool and `TopSignal` int (and `Softening`, which was already saved, is now also sent); on the Leg Controller `Grounded` int and `Overreach` bool.
