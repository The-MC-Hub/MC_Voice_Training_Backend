# ADR-001: Selection of MongoDB Atlas as Primary Persistence Store

## Status
Accepted

## Date
2026-01-15

## Context
The MC Hub platform manages diverse, rapidly evolving data schemas including flexible user profiles, voice training attempts, audio analysis metadata, course structures, custom MC portfolios, and gamification logs. Relational databases require complex migration scripts for polymorphic data types such as interactive quiz questions and custom MC booking parameters.

## Decision
We select **MongoDB Atlas** as the primary document persistence store for the application backend.

## Consequences
- **Positive**: High schema flexibility for nested entities (e.g. course lesson contents, quiz options), native support for geo-spatial indexing (MC location search), and horizontal auto-scaling via Atlas.
- **Negative**: Lack of multi-document ACID transactions across un-sharded collections without performance penalty; requires application-level integrity enforcement for cross-domain references.
