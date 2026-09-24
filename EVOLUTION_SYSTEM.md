# Evolution + Care System (2026-09-21)

The phone app now evolves Digimon off the **card's real data** instead of
`characterId + 1` with random win counts, and runs a **lifespan/care system**
(the DIM "life expectancy" self-delete).

## New files (phone module)

| File | Job |
|---|---|
| `card/DimCardAdapter.kt` | Reads a VB-DIM-Reader `Card` into `DigimonBaseStats` and `EvolutionPath` lists. Handles DIM + BEM. |
| `monster/EvolutionEngine.kt` | Pure Kotlin. Compares a `PerformanceSnapshot` against each `EvolutionPath`; returns per-requirement progress + which evolutions are unlocked. Branching trees supported (multiple candidates). |
| `monster/CareManager.kt` | Pure Kotlin. Lifespan clock, care mistakes (overwork / neglect), death. All knobs in `CareTuning`. |

`EvolutionEngine` and `CareManager` have zero Android imports so they can be
shared with the wear module later (wear `MonsterManager` is still on the old
system — mirror is a follow-up).

## Modified

- `monster/PhoneMonsterManager.kt` — new persisted state (losses, vitalPoints,
  per-stage counters, baseHp/baseAp, full care state); `recordBattleResult(won)`,
  `addVitalPoints()`, `addTrophy()`, `syncStepsToVitalPoints()`, `tickCare()`,
  `evolveTo(toIndex, hours)`, `getEvolutionCandidates()`, `applyCardBaseStats()`,
  `clearMonster()` (the self-delete; keeps cards + dev settings). `updateAging()`
  now piggybacks `tickCare()` so the care clock runs in real time. Legacy
  `evolve()` no longer wipes lifetime wins and reseeds care/base stats.
- `ui/BattleScene.kt` — player HP = DIM `baseHp` + bonus (was flat 500);
  player damage = (DIM `baseAp` + bonus) / 4. Defaults preserve old balance
  when no card data is loaded.
- `PhoneMainActivity.kt` — P2P battle result now records wins AND losses.
- `lab/MapAdventureActivity.kt` — player defeat now records a loss.

## DIM data mapping (verified against VB-DIM-Reader 7845f2a)

| Card field | App use |
|---|---|
| `CharacterStatsEntry.hp / ap` | Base HP / attack in battles |
| `stage`, `attribute`, `type`, `dp` | Stored on monster; attribute synced |
| `DimStatBlock.isUnlockRequired` | Secret-character gating (existing logic) |
| `TransformationRequirementsEntry.fromCharacterIndex → toCharacterIndex` | **The real evolution tree** (branching, not +1) |
| `requiredVitalValues` | VP target — earned via steps/exercise |
| `requiredTrophies` | Trophy target |
| `requiredBattles` / `requiredWinRatio` | Battle targets (losses now tracked, ratio is real) |
| `DimEvolutionRequirementBlock.hoursUntilEvolution` | Min hours at stage (BEM defaults to 1h) |

## Requirement scope decision

Requirements are evaluated **per stage** (stageVitalPoints, stageBattles,
stageWins, stageTrophies, timeAlive). Lifetime wins/losses are kept separately
for win ratio display and NFC payloads. Revisit when the DIM file lands if the
card's semantics turn out to be lifetime-based.

## Care rules (tunable in `CareTuning`)

- Fresh monster per stage: 14 days + 24h per stage index of lifespan.
- Each battle costs 0.5h; **>5 battles in a row with no activity = care mistake**
  (−24h, counter kept until activity resets it).
- **No activity for 36h = care mistake** (−24h, at most once per tick; ticks
  clamp at 72h so a dead phone doesn't insta-kill).
- 500 steps = 1 VP; 4000 steps/day = "looked after" (resets overwork, marks day active).
- Lifespan hits 0 → Digimon dies → `clearMonster()` (self-delete).

## Still stubbed / follow-ups

1. **Trophies** — `addTrophy()` exists but nothing awards them yet. Decide: boss
   victories in MapAdventure? Tournament wins?
2. **Step sync** — call `monsterManager.syncStepsToVitalPoints(phoneGpsManager steps)`
   on a timer (e.g. in the aging loop or tracking service). Currently nothing calls it.
3. **Death UX** — death currently just clears the monster; add an egg/farewell screen.
4. **DIGIVOLVE button** (`PhoneMainActivity` `canEvolve`) still uses the old
   winsRequired gating — switch it to `getEvolutionCandidates()` and offer the
   choice UI when multiple branches unlock.
5. **Workout → VP** — `WorkoutManager` should call `addVitalPoints()` on completion.
6. **Watch mirror** — DONE 2026-09-21. `DimCardAdapter.kt`, `EvolutionEngine.kt`,
   and `CareManager.kt` copied into the wear module; the watch `MonsterManager`
   carries the same counters/care fields under its `current_`/`stored_` slot
   prefixes. The foreground service now: syncs steps to VP, ticks the care
   clock, and evolves along the card's real tree (throttled to once per minute
   on Dispatchers.IO) instead of `characterId + 1` after 12h. Battles record
   wins AND losses; player HP uses the DIM base HP; completed workouts earn
   2 VP. Watch improvements over the phone baseline: neglect penalties fire at
   most once per 24h (the 5s loop can't insta-kill through repeats), and the
   step delta treats a lower total as the midnight counter reset. Stored
   (sleeping) partners are fully frozen from steps, care, and evolution.

## To verify against the real DIM file (when attached)

- `attribute` enum values (what 0/1/2/… mean)
- VP earn rate (is 500 steps/VP sane vs. the bracelet's VP?)
- Whether lifespan/age bytes exist per character we should read instead of the
  app-side 14-day default
- Per-stage vs lifetime requirement semantics
- `type` field meaning

## Watch storage (sleep) + workout tab (2026-09-21)
- Watch menu no longer lists exercises at the bottom. A **Workouts** chip opens its
  own WORKOUTS tab listing Push-ups / Pull-ups / Sprints / Squats; picking one goes
  to the existing TRAINING countdown screen.
- **Storage** chip opens a STORAGE tab showing the active partner and the stored
  (sleeping) partner. Storage lives in the watch `MonsterManager` under the
  `stored_` prefs prefix (`storeCurrentMonster()`, `wakeStoredMonster()`,
  `clearStoredMonster()`, `getStoredMonster()`, `getStoredAt()`).
- Sleep is fully frozen: the foreground service only ages/evolves the active
  (`current_`) monster, so a stored Digimon gains no steps, time, or evolution
  progress while asleep. Waking swaps the two partners.
- Flow: Storage -> "Put Partner to Sleep" -> stored, active slot freed ->
  card list (Lab) -> pick new partner -> train. Stored partner shows its
  card/character and how long it has been asleep.
