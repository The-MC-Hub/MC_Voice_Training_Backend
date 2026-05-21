---
name: config
description: "Skill for the Config area of The-MC-Hub-Java-Backend. 3 symbols across 2 files."
---

# Config

3 symbols | 2 files | Cohesion: 100%

## When to Use

- Working with code in `src/`
- Understanding how JwtAuthenticationFilter, securityFilterChain, corsConfigurationSource work
- Modifying config-related functionality

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/mchub/config/SecurityConfig.java` | securityFilterChain, corsConfigurationSource |
| `src/main/java/com/mchub/config/JwtAuthenticationFilter.java` | JwtAuthenticationFilter |

## Entry Points

Start here when exploring this area:

- **`JwtAuthenticationFilter`** (Class) — `src/main/java/com/mchub/config/JwtAuthenticationFilter.java:30`
- **`securityFilterChain`** (Method) — `src/main/java/com/mchub/config/SecurityConfig.java:34`
- **`corsConfigurationSource`** (Method) — `src/main/java/com/mchub/config/SecurityConfig.java:55`

## Key Symbols

| Symbol | Type | File | Line |
|--------|------|------|------|
| `JwtAuthenticationFilter` | Class | `src/main/java/com/mchub/config/JwtAuthenticationFilter.java` | 30 |
| `securityFilterChain` | Method | `src/main/java/com/mchub/config/SecurityConfig.java` | 34 |
| `corsConfigurationSource` | Method | `src/main/java/com/mchub/config/SecurityConfig.java` | 55 |

## How to Explore

1. `gitnexus_context({name: "JwtAuthenticationFilter"})` — see callers and callees
2. `gitnexus_query({query: "config"})` — find related execution flows
3. Read key files listed above for implementation details
