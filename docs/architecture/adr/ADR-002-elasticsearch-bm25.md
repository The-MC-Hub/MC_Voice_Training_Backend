# ADR-002: Full-Text Lesson Search via Elasticsearch BM25 Engine

## Status
Accepted

## Date
2026-02-01

## Context
Voice lessons require high-precision full-text search based on speech difficulty, phonetic terms, tags, and sample text scripts. Standard regex queries in MongoDB exhibit high CPU consumption and poor performance on large text volumes.

## Decision
We deploy **Elasticsearch** alongside MongoDB specifically for full-text search indexing using the BM25 relevance scoring algorithm.

## Consequences
- **Positive**: Sub-millisecond text search performance, fuzzy term matching, accent-insensitive Vietnamese tokenization.
- **Negative**: Requires synchronization mechanisms (`VoiceLessonSearchDocument`) between MongoDB and Elasticsearch.
