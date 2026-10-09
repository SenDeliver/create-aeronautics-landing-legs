
First public release as **Create Aeronautics: Landing Legs** (mod id `legworks`).

### Added

- Simple Leg Mount: levels a Ship on its own, no power, fixed Stiffness and Damping.
- Hydraulic Leg Mount: powered (4 SU per RPM), Stiffness and Damping value boxes, Softening on power loss or top redstone.
- Leg Controller: Links up to 8 Hydraulic Legs within 16 blocks, Ride Height (Auto, or up to the longest Linked Limb's Reach), Gear Up / Gear Down by redstone.
- Short, Long and Heavy Limbs, each with its own Reach, Stiffness and drawn model. Any item with the `legworks:limb` data component works as a Limb.
- Leveling, Foot grip, Crouch, Overreach, Bumpers (sideways or upward Mounts), standing on other Ships, entity collision on the drawn leg.
- Recipes: crafting table, Mechanical Crafter and Sequenced Assembly.
- Create-style tooltips, Ponder scenes, Engineer's Goggles info, Jade and JEI support, Display Link sources for the Leg Controller and Leg Mounts.
- Fix for Display Links on a Ship losing their target when the Ship is assembled (Sable 2.0.x).
- Server config for springs, Leveling and Hydraulic limits.

### Notes

- Earlier 0.1.x builds were internal test versions and were not published. Worlds from 0.1.x load in 1.0.0: block, item and NBT ids are unchanged.

[Unreleased]: https://github.com/SenDeliver/create-aeronautics-landing-legs/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/SenDeliver/create-aeronautics-landing-legs/releases/tag/v1.0.0
