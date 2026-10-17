# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Three roles, all authenticated via the same app shell (`FISHER`, `INSPECTOR`, `ADMIN` in code):

- **Pescadores / armadores (FISHER)** — the primary audience, especially for the public institutional portal. Artisanal fishers who need to register vessels and gear, log faenas (fishing trips) and catches, hold and renew fishing permits, and check species/closure rules before going out.
- **Inspectores (INSPECTOR)** — field/compliance staff who consult the same operational data (trips, permits, species, closures) to verify legality.
- **Administradores (ADMIN)** — internal staff who manage users, vessels, and review the audit log.

This is a course project simulating an AUNAP-style government deployment; the primary user for design purposes is still the artisanal fisher/citizen, since the public landing page's central job is getting them to understand the system and register or log in.

## Product Purpose

SIGEPES (Sistema de Gestión de Pesca Sostenible) digitizes the legal core of artisanal fishing regulation in Colombia: digital trip/catch logbooks, permit and license validation, a species catalog with legal minimum sizes, closure (veda) periods and zones, and an admin audit trail. Success means a fisher can prove legal compliance digitally and a regulator can verify it, reducing illegal, unreported, and unregulated (IUU) fishing.

## Positioning

A compliance-and-legality system, not a marketplace or environmental-monitoring platform: its mechanism is turning statutory obligations (Ley 13 de 1990, Ley 1851 de 2017, AUNAP's mandate) into a structured digital record — logbook entries, permits, species/closure rules, and an audit trail — that a neighboring generic fishing app would not have to model correctly against Colombian law.

## Operating Context

- MVP v1.0 ("Núcleo de control y legalidad") is implemented: RF-01 auth, RF-02 vessels/gear, RF-03 trip/catch logbook, RF-05 closures, RF-06 species & minimum sizes, RF-08 permits/licenses, RF-20 admin & audit.
- Explicitly out of scope for now (planned v1.1/v2.0, data model already prepared): quotas (RF-07), geolocation (RF-04), traceability (RF-09), reporting/denuncias (RF-10), alerts (RF-11), dashboard/tablero (RF-12), Marine Protected Areas (RF-13), environmental data (RF-14), documents (RF-15), marketplace (RF-16), MSY/RMS (RF-17), reports (RF-18), education (RF-19). Placeholder module folders for several of these already exist in the frontend (`inspections`, `map`, `marketplace`, `education`, `notifications`, `users`) — treat them as scaffolding for later increments, not confirmed near-term design work.
- Deployment/dev: frontend (React 19 + TypeScript + Vite + MUI) on :5173, backend (Spring Boot + Java 21 + PostgreSQL, Flyway migrations) on :8080, Docker Compose for local spin-up.
- The frontend ships `vite-plugin-pwa`, suggesting an installable/offline-tolerant experience matters for fishers who may be at sea or in low-connectivity coastal areas.

## Capabilities and Constraints

- Backend keeps Spanish resource/field names to match what the frontend already expects (per backend README) — a durable naming constraint across the stack.
- Role-based navigation is enforced in the frontend (`navConfig.tsx`): most modules are visible to all three roles; "Embarcaciones" is FISHER/ADMIN only; "Auditoría" is ADMIN only.
- Regulatory framework future work must respect: Ley 13 de 1990 (Estatuto General de Pesca), Ley 1851 de 2017 (anti-illegal-fishing, formalization of artisanal fishers), AUNAP as the regulating/sanctioning authority, and ODS 14 / meta 14.4 (UN SDG on ending IUU fishing and overfishing).

## Brand Commitments

- Name: SIGEPES — Sistema de Gestión de Pesca Sostenible.
- Existing institutional visual identity already in place on the public portal (dark navy `#071E30` header/hero, secondary accent color, `Logotype` component, `Footer`, `TopUtilityBar`, `WaveDivider` illustration) — this is incumbent visual truth for `document`/refinement work, not yet captured in a DESIGN.md.

## Evidence on Hand

- This is an academic/course project, not a real government deployment. Institutional-sounding details on the public portal (phone `01 8000 000 000`, email `contacto@sigepes.gov.co`, the hero photo credit) are illustrative placeholders for the simulated scenario, not verified real-world facts — future work must not treat them as claims to fact-check or expand into real institutional commitments, but may keep or restyle them as placeholder content.
- Legal/regulatory citations (Ley 13 de 1990, Ley 1851 de 2017, AUNAP, ODS 14 meta 14.4) are real and should be preserved accurately since they anchor the simulated system's credibility.
- Demo login credential is documented in `README.md`/`backend/README.md` for evaluators (`pescador@sigepes.gov.co`); this is intentional seed/demo data for a course project, not a production secret.

## Product Principles

1. Legality first: every feature should make it easier to prove or verify compliance (permits, minimum sizes, closures), not just log activity.
2. Design for the artisanal fisher as the primary audience, even though inspectors and admins share the same app shell — the public portal in particular should read as "built for pescadores," not as generic enterprise software.
3. Respect the MVP boundary: v1.0 features (auth, vessels, logbook, species, closures, permits, audit) are real; anything in the roadmap (quotas, geolocation, traceability, marketplace, etc.) is future-state and should not be designed as if already delivered.
4. Preserve the Spanish-language, government-institutional register (formal, legally precise) established by the existing content and regulatory citations.

## Accessibility & Inclusion

Target WCAG 2.1 AA, consistent with Colombian government digital accessibility norms (NTC 5854) given the .gov.co institutional framing, even though this is a simulated deployment.
