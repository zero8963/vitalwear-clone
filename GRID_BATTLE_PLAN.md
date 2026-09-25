# Grid Battle Mode — plan (2026-09-25)

A separate, self-contained battle mode in the VitalWear **phone** app.
The original watch-linked battle structure is untouched.

## Concept
- New "Grid Battle" selection on the phone app (separate from standard battles).
- Real-time 6x3 grid battles, Battle Network-style: move on your half,
  fire buster shots, pick chips mid-fight.
- **Fighters are the players' Digimon partners** — their trained stats
  (HP/AP from the DIM normalization) feed the grid fighter, so training
  with Zero still matters here.
- **Skill-based**: dodging, positioning, and chip timing decide fights.

## Components (build order)
1. **Chip library** (~150 chips: ~115 standard / ~25 mega / ~10 giga).
   Original names (no Capcom IP). Fields: id, name, tier, element, damage,
   letter codes (A–Z, `*` wildcard), MB cost, hits, effect kind
   (projectile/melee/lob/beam/summon/trap/support), description.
   30-chip folders; code-matching rule for multi-select (same code or `*`).
2. **Chip compendium UI** — browse/search the library.
3. **NaviCust ("Program Grid")** — working customizer: place polyomino
   program parts on a grid, color rules, glitch penalty for bad placement,
   style derived from layout, persisted per Digimon, effects live in battle.
4. **Grid battle engine** — vs AI first (testable solo): movement, buster,
   chip queue, HP, win/lose, Digimon stat mapping.
5. **PvP** — phone-to-phone real-time sync over the existing multiplayer
   transport, as its own match selection.
6. **Balance pass** — with the user + his oldest son after they playtest.

## Decisions locked (2026-09-25)
- Separate mode/selection; watch battle path unchanged.
- Digimon partners fight (not separate Navi characters).
- ~150 chips, standard/mega/giga tiers, original names.
- Damage scale targets the normalized stat ballpark (Digimon HP in the
  tens); exact numbers tuned in the balance pass.
