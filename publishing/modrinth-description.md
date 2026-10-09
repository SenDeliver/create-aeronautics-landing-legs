# Create Aeronautics: Landing Legs

**Adaptive landing legs for Create Aeronautics Ships. Land on uneven ground and stay level.**

![A Ship standing level over a pit: one Leg reaches into the hole, the others stand on the rim](https://raw.githubusercontent.com/SenDeliver/create-aeronautics-landing-legs/main/docs/images/hero-leveling-over-pit.png)

## What it does

Put a **Leg Mount** on your Ship's hull and fit a **Limb** into it. That makes a **Leg**. Each Leg reaches down to the ground on its own, so the deck stays level on slopes, steps and holes. Simple Legs work by themselves. Hydraulic Legs are run by a **Leg Controller**, which sets the Ride Height and folds the Legs up for flight.

## Features

- **Leveling**: every Leg picks its own Reach, so the Ship sits level on uneven ground.
- **Foot grip**: Feet hold like rubber pads, so a parked Ship does not slide off a slope.
- **Crouch**: while one Foot is still looking for ground, the other Legs lower the Ship together so it does not tip.
- **Overreach**: if the ground is too uneven, the Legs stretch to the max and the Ship tilts toward the low side.
- **Two Leg types**: Simple Legs (no power, no setup) and Hydraulic Legs (powered, adjustable).
- **Leg Controller**: sets Ride Height, links up to 8 Hydraulic Legs, **Gear Up / Gear Down** with redstone.
- **Softening**: a redstone signal on top of a Hydraulic Leg makes it softer, for example to kneel a parked Ship for loading.
- **Bumpers**: a Leg Mount facing sideways or up acts as a spring against ground it meets.
- **Three Limbs**: Short, Long and Heavy, each with its own Reach and Stiffness. Any item with the `legworks:limb` data component is a Limb too.

![Short, Long and Heavy Limbs side by side](https://raw.githubusercontent.com/SenDeliver/create-aeronautics-landing-legs/main/docs/images/limbs-short-long-heavy.png)

## Quick start

1. Craft a **Simple Leg Mount** (or a **Hydraulic Leg Mount**) and a **Limb**.
2. Place the Mount on the hull facing down, right-click it with the Limb.
3. Assemble the Ship as usual. Simple Legs are done.
4. For Hydraulic Legs: place a **Leg Controller** on the Ship, sneak-click it with a Leg Controller item, click each Hydraulic Mount to link.
5. Power each Hydraulic Mount through its shaft. Scroll the Controller's value box for Ride Height. Redstone on the Controller = Gear Up.

![Hydraulic Legs folded with Gear Up](https://raw.githubusercontent.com/SenDeliver/create-aeronautics-landing-legs/main/docs/images/gear-up-folded.png)

Ponder (hold `W` over an item) shows all of this step by step.

## Compatibility

- **Ponder** scenes for every block.
- **Engineer's Goggles**: Reach, load, Stiffness, power and warnings on every Leg.
- **Jade** and **JEI** (optional): status overlay, recipes, a "Leg Fitting" category and info pages.
- **Create Display Link**: Leg Controller and Leg Mounts are Display Link sources.

![Engineer's Goggles on a Leg Controller](https://raw.githubusercontent.com/SenDeliver/create-aeronautics-landing-legs/main/docs/images/goggles-leg-controller.png)

## Requirements

| Mod | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.228+ |
| [Create](https://modrinth.com/mod/create) | 6.0.10+, below 6.1.0 |
| [Create Aeronautics](https://modrinth.com/mod/create-aeronautics) | 1.3.2+, below 1.4.0 |
| [Sable](https://modrinth.com/mod/sable) | 2.0.5+, below 2.1 |
| [JEI](https://modrinth.com/mod/jei), [Jade](https://modrinth.com/mod/jade) | optional |

Install on **both** client and server.

## Known limitations

- No special landing mode yet: drops of more than about 20 blocks can tip the Ship over.
- Limbs cannot be inserted by Funnels. Fit them by hand.
- Pinned to Sable 2.0.x (uses mixins into Sable internals).

## Links

- [Source and docs](https://github.com/SenDeliver/create-aeronautics-landing-legs)
- [Issues](https://github.com/SenDeliver/create-aeronautics-landing-legs/issues)
- [Changelog](https://github.com/SenDeliver/create-aeronautics-landing-legs/blob/main/CHANGELOG.md)

MIT License. Built on Create, Create Aeronautics and Sable (not bundled). Thanks to the Offroad addon for the Wheel Mount pattern. Developed with AI assistance (Claude); design, testing and direction by SenDeliver.
