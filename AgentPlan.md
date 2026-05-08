# iBetcha — Agent Plan

This document maps the full build journey to the agents that will drive each phase.
Phases run roughly in sequence, though some overlap (e.g., reviews run alongside design).

---

## Phase 1 — Discovery & Clarity
**Goal:** Validate assumptions, surface risks, and explore product directions before committing to anything.

| Agent | Role |
|---|---|
| `nw-product-discoverer` | Validate problem-market fit, surface assumption risks in requirements |
| `nw-diverger` | JTBD analysis, competitive landscape, brainstorm 3-5 product/UX directions |
| `nw-diverger-reviewer` | Peer review of diverger directions before the team converges |

**Output:** Validated problem statement, ranked design directions, risk register.

---

## Phase 2 — UX & Product Definition
**Goal:** Translate requirements and chosen direction into concrete user journeys and BDD stories.

| Agent | Role |
|---|---|
| `nw-product-owner` | User journey maps, emotional arcs, user stories with BDD acceptance criteria |
| `nw-product-owner-reviewer` | Hard gate — blocks handoff to architecture if stories or DoR items fail |

**Output:** Full user story map, BDD acceptance criteria, Definition of Ready sign-off.

---

## Phase 3 — Domain & Architecture Design
**Goal:** Define the domain model, tech stack, and system-level design.

| Agent | Role |
|---|---|
| `nw-ddd-architect` | Bounded contexts, aggregates, bet lifecycle state machine, Event Storming |
| `nw-solution-architect` | Tech stack, component boundaries, API design, auth strategy, security posture |
| `nw-system-designer` | Scalability, push notification pipeline, S3 upload patterns, infra sizing |
| `nw-data-engineer` | Schema design, database selection, query patterns, data governance |

**Output:** Architecture SSOT document, domain model, infra blueprint, data schema.

**Agents can run in parallel:** `nw-ddd-architect` and `nw-system-designer` are largely independent.
`nw-solution-architect` should complete before `nw-data-engineer` starts (tech stack informs DB choice).

---

## Phase 4 — Architecture Review
**Goal:** Independent review of all design outputs before any code is written.

| Agent | Role |
|---|---|
| `nw-ddd-architect-reviewer` | Validates domain model, aggregate boundaries, ubiquitous language |
| `nw-solution-architect-reviewer` | Validates tech decisions, security posture, API design |
| `nw-system-designer-reviewer` | Validates scalability claims, trade-off analysis, SPOF detection |
| `nw-data-engineer-reviewer` | Validates schema, query patterns, data governance |

**Output:** Review reports — go/no-go per area. Blockers must be resolved before Phase 5.

---

## Phase 5 — Acceptance Tests
**Goal:** Turn user stories into executable E2E specifications that drive Outside-In TDD.

| Agent | Role |
|---|---|
| `nw-acceptance-designer` | Given/When/Then acceptance tests from user stories and architecture |
| `nw-acceptance-designer-reviewer` | Validates coverage, executability, and BDD correctness |

**Output:** Executable acceptance test suite covering all critical flows.

---

## Phase 6 — Build
**Goal:** Implement the application using Outside-In TDD, guided by acceptance tests.

| Agent | Role |
|---|---|
| `nw-software-crafter` | Feature implementation per bounded context, Outside-In TDD, progressive refactoring |
| `nw-software-crafter-reviewer` | Code quality, implementation correctness, TDD discipline |
| `nw-platform-architect` | CI/CD pipelines, AWS infra, S3 setup, push notification service, prod readiness |
| `nw-platform-architect-reviewer` | Reviews deployment config, observability, and production handoff |

**Note:** Multiple `nw-software-crafter` agents can run in parallel across bounded contexts.

---

## Phase 7 — Optimize & Ship
**Goal:** Tighten the test suite, fix bugs, document, and hand off to production.

| Agent | Role |
|---|---|
| `nw-test-optimizer` | Reduce test noise and redundancy without losing coverage |
| `nw-test-optimizer-reviewer` | Hard-blocks if coverage drops or production code is touched |
| `nw-troubleshooter` | Root-cause analysis for bugs and unexpected behaviors |
| `nw-documentarist` | API docs, onboarding docs (Diátaxis: tutorials, how-tos, reference, explanation) |

---

## Agent Dependency Map

```
Phase 1 (Discovery)
  └── Phase 2 (UX/Product)
        └── Phase 3 (Architecture) ──────────────────────────┐
              ├── nw-ddd-architect ──┐                        │
              ├── nw-system-designer ┤ (parallel)             │
              ├── nw-solution-architect ─── nw-data-engineer  │
              └── Phase 4 (Review) ◄────────────────────────-┘
                    └── Phase 5 (Acceptance Tests)
                          └── Phase 6 (Build) ─── Phase 7 (Ship)
```

---

## Current Status

| Phase | Status |
|---|---|
| Phase 1 — Discovery | COMPLETE — decisions locked in docs/phase1-decisions.md |
| Phase 2 — UX/Product | COMPLETE — PO output reviewed, PASS with 6 non-blocking warnings |
| Phase 3 — Architecture | COMPLETE — 4 docs produced, 1 conflict to resolve (timeout scheduling) |
| Phase 4 — Review | COMPLETE — all 4 PASS, alignment issues documented |
| Phase 5 — Acceptance Tests | COMPLETE — 189 scenarios, 14 feature files, 13 walking skeletons |
| Phase 6 — Build | IN PROGRESS — Walking Skeleton 1 complete (backend + frontend compile clean) |
| Phase 7 — Ship | Not started |
