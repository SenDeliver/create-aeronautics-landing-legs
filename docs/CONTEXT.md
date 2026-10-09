# Landing Legs glossary

Create Aeronautics: Landing Legs (mod id `legworks`) is a Create Aeronautics addon that gives Ships adaptive legs, so they can land on uneven terrain and stay level.

## Language

**Ship**:
A physics contraption simulated by Sable (a sub-level) — helicopter, airship, any assembled flying or driving build.
_Avoid_: contraption, sub-level (in player-facing text), vehicle

**Leg**:
A Leg Mount fitted with a Limb. It reaches down to the ground and holds its mount point at a chosen height, bending or stretching to match the terrain under it.
_Avoid_: wheel, landing gear block, strut

**Leg Mount**:
The block placed on a Ship's hull that a Limb is fitted to. Its type (Simple or Hydraulic) decides how the Leg behaves. It may face any direction; the Limb extends the way it faces, and only roughly downward Legs take part in Leveling. Off a Ship it is inert.
_Avoid_: leg block, socket

**Limb**:
An item fitted onto a Leg Mount that becomes the drawn leg below it. Any Limb fits any Leg Mount. The Limb decides the Leg's maximum Reach and base Stiffness, so different Limbs suit different Ship sizes. A Leg Mount without a Limb does nothing.
_Avoid_: leg item, strut, tire

**Leveling**:
Keeping a Ship's deck horizontal while it rests on Legs, by giving each Leg a different reach.
_Avoid_: balancing, stabilizing

**Landing**:
The phase where a descending Ship's Legs touch ground and take its weight.

**Walking**:
A possible future mode where a Leg Controller makes its Legs step and move a Ship over terrain, including swinging a Foot toward the side a redstone signal comes from. Not in v1.

**Foot**:
The end of a Leg that touches the ground. The only part of a Leg that physically meets the world.

**Bumper**:
A Leg facing sideways or up. It acts only as a spring against Ground it meets, never Levels.
_Avoid_: buffer, side leg

**Ground**:
Any solid collision surface a Foot can rest on, using the block's real shape, including the blocks of another Ship (which is pushed back by the Leg). Fluids, entities and non-solid blocks are never Ground.
_Avoid_: terrain (when meaning the contact surface), floor

**Reach**:
How far a Leg currently extends from its mount point down to its Foot. Each Leg has its own Reach, up to a maximum.
_Avoid_: length, extension

**Leg Controller**:
The block that coordinates a Leg Group so the Ship stays level. Needs no rotational power. Several Leg Controllers may share one Ship; they all aim for level, and differing Ride Heights settle in between.
_Avoid_: brain, gyro, stabilizer

**Leg Group**:
All Legs Linked to one Leg Controller. One Ship may carry several Leg Groups.

**Linking**:
Binding a Leg to a Leg Controller by clicking the Leg with the Controller item. Never automatic. A Leg belongs to at most one Controller, and both must be on the same Ship.
_Avoid_: pairing, binding, connecting

## Leg types

Two Leg types in v1, built and play-tested side by side.

**Simple Leg**:
A standalone Leg with no Controller and no power. It Levels the Ship on its own with fixed factory Stiffness and Damping. Cheapest recipe.
_Avoid_: basic leg, passive leg

**Hydraulic Leg**:
A Leg that needs its own rotational power and is Linked to a Leg Controller, which sets its Ride Height and Gear Up. Its Stiffness and Damping are set by the player on the Leg Mount. Without power it Softens fully and lets the Ship settle to the ground. Without a Linked Leg Controller it is inert: no push at all.
_Avoid_: powered leg, advanced leg

**Softening**:
A temporary drop in a Hydraulic Leg's effective Stiffness, caused by losing power (full Softening) or by a redstone signal on its top (stronger signal, softer Leg).
_Avoid_: releasing, unlocking, limp

**Stiffness**:
How hard a Leg's spring pushes back: the Limb's base Stiffness, scaled by the Leg Mount's setting on a Hydraulic Leg, then lowered by any Softening.
_Avoid_: hardness, strength, spring force

**Damping**:
How strongly a Leg calms bouncing. Fixed on a Simple Leg; set by the player on a Hydraulic Leg Mount.
_Avoid_: bounciness, shock absorption

**Foot grip**:
A grounded Foot holding its spot like a rubber pad, so the Ship does not slide sideways. A strong enough push makes it slip to a new spot. Standing on another Ship, the grip pushes that Ship back.
_Avoid_: friction, anchor

**Crouch**:
While any Foot is still searching for Ground, the grounded Legs shorten together so the Ship lowers before it can tip.
_Avoid_: squat, kneel

**Short Limb** / **Long Limb** / **Heavy Limb**:
The three v1 Limbs, from early-game (andesite) to late-game (sturdy). Short and Long differ in Reach; Heavy has high base Stiffness for large Ships.

## Stance

**Gear Down**:
The default state of a Leg Group: Legs extended to their maximum Reach, ready for Landing. A Simple Leg is always Gear Down.
_Avoid_: deployed, extended mode

**Gear Up**:
A Leg Group folded against the Ship for flight, while its Leg Controller receives a redstone signal.
_Avoid_: retracted, stowed, tucked

**Ride Height**:
How high a Hydraulic Leg Group holds the Ship's deck above the ground, set on the Leg Controller. Simple Legs have no Ride Height setting; they hold a fixed default.
_Avoid_: stance height, clearance

**Overreach**:
When the ground under a Leg Group is too uneven to level. Legs stretch to their maximum Reach and the Ship tilts toward the low side, as it would in real life.
_Avoid_: out of range, unlevelable
