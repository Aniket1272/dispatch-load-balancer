# Day 2 Guide: finish the Dispatch Load Balancer

## 0. Apply the overlay (10 min)
Unzip `dispatch-load-balancer-day2-overlay.zip` **on top of** your Day 1 project folder.
Only `README.md` is overwritten; every other file is new.

New files:
```
src/main/java/com/assignment/dispatch/
  dto/        AssignedOrder, VehiclePlan, UnassignedOrder, PlanSummary, DispatchPlanResponse
  strategy/   DispatchStrategy, GreedyDispatchStrategy
  service/    DispatchPlanService
  controller/ DispatchPlanController
src/test/java/com/assignment/dispatch/
  strategy/   GreedyDispatchStrategyTest
  controller/ DispatchPlanControllerTest, DispatchPlanIntegrationTest
samples/      orders-edge-cases.json, invalid-payloads.json
postman/      Dispatch-Load-Balancer.postman_collection.json
scripts/      smoke-test.sh
```
No Day 1 file needs editing. (Optional: delete the `// GET /api/dispatch/plan -> Day 2` comment in `DispatchController`.)

## Schedule (about 6 hours)
| Time | Step |
|---|---|
| 0:00-0:15 | Apply overlay, run `mvn clean test`. Fix anything red before moving on. |
| 0:15-1:30 | Read `GreedyDispatchStrategy` top to bottom. You must be able to explain both phases and the 2-opt delta formula. |
| 1:30-2:30 | Read `GreedyDispatchStrategyTest`. Break the code on purpose (e.g. flip the priority comparator) and watch the right test fail. |
| 2:30-3:30 | `mvn spring-boot:run`, then run the Postman collection: Happy path, Edge cases, Validation errors. |
| 3:30-4:30 | Check the plan against the expected results below, then check the performance numbers. |
| 4:30-5:30 | Final README pass: confirm the assumptions section matches what you actually believe, then check Swagger UI. |
| 5:30-6:00 | Clean commit history (`git log`), tag `v1.0`, zip or push. |

## Expected results (manual evaluation)
Computed with an independent Python port of the algorithm, so small differences in the last digit are possible.
Restart the app between scenarios because data is upserted, not replaced.

**Scenario A: `samples/vehicles.json` + `samples/orders.json`**
| Vehicle | Load / capacity | Distance | Stops in order |
|---|---|---|---|
| VEH001 | 66 / 100 | ~40.15 km | ORD002, ORD008, ORD009 |
| VEH002 | 42 / 80 | ~16.08 km | ORD004, ORD010 |
| VEH003 | 108 / 120 | ~43.26 km | ORD005, ORD006, ORD001, ORD007, ORD003 |

Unassigned: none. Total about 99.49 km.

**Scenario B: same vehicles + `samples/orders-edge-cases.json` only**
- EDGE001 (500) is unassigned: heavier than every vehicle.
- EDGE002 (HIGH), EDGE006 (HIGH) and EDGE003 (MEDIUM) are assigned.
- EDGE004 and EDGE005 (both LOW) are unassigned: HIGH and MEDIUM orders already consumed the capacity.
  This demonstrates priority beating distance.

**Scenario C: `samples/invalid-payloads.json`**: each entry sent to `/orders` returns `400`.

## Performance check
`GreedyDispatchStrategyTest.handlesFiveThousandOrdersQuickly` must pass (limit 30 s; expect a few seconds).
Assignment is O(N x V). Sequencing is O(k^2) per route and capped by `max-stops-for-local-search` and a 1 s budget per route.

## Be ready to explain in the interview
1. Why a greedy heuristic: capacitated VRP is NP-hard, so an exact solver is not realistic for large inputs.
2. Why priority is applied at assignment time and distance at sequencing time (see README assumptions).
3. Why upsert uses `findAllById` + `saveAll` + `@Version`.
4. How you would scale it: spatial index (grid or k-d tree), clustering before routing, async plan jobs, PostgreSQL.
5. What you would change if the spec required HIGH-priority stops to be visited first inside a route.

## Final submission checklist
- [ ] `mvn clean test` all green
- [ ] Postman collection runs clean end to end
- [ ] README renders correctly and the run steps work on a fresh clone
- [ ] `.gitignore` excludes `target/`
- [ ] No leftover TODOs or debug prints
