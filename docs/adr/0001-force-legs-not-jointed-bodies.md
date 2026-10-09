# Legs are force actors, not jointed physics bodies

A Leg is a single block on the Ship that probes the ground and pushes the Ship up through Sable's physics tick (like a spring); the knee bend is only a client-side animation. We rejected building each leg segment as its own Sable body joined by motorised hinges, because it adds several physics bodies per Leg on a server already bottlenecked by Sable, and jointed rigs jitter and are hard to tune.

## Consequences

- Only the Foot collides with the world; the drawn upper leg does not.
- Walking later means moving the Foot target, not driving joints.

## Amendment (spike rounds 2–5)

The drawn leg now also blocks players and entities: a Sable entity-collision mixin checks each Leg Mount's extended collision shape (body + strut + Foot pad at the current Reach). World contact is still force-only through the Foot. The extended shape is hidden from Sable's physics collider (`BlockSubLevelCollisionShape`/`SableCollisionContext` return a full block), so the Ship's rigid body never collides via the leg and nothing fights the spring. The mixin targets Sable internals; pin `sable [2.0.5,2.1)`.
