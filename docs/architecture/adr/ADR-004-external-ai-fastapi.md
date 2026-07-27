# ADR-004: Decoupled AI Voice Scoring Engine via Python FastAPI

## Status
Accepted

## Date
2026-02-15

## Context
Audio analysis (pronunciation scoring, pitch detection, speech-to-text accuracy) and Text-To-Speech (TTS) generation require specialized Python AI libraries (PyTorch, librosa, Whisper). Running these heavy models inside the JVM process causes high RAM overhead and risks thread exhaustion.

## Decision
We decouple the AI Scoring Engine into an external **Python FastAPI service** deployed on dedicated GPU/CPU nodes, accessed via HTTP proxy endpoints in `VoiceController`.

## Consequences
- **Positive**: Isolation of heavy AI compute workloads, independent deployment lifecycle for AI models, clean JVM memory footprint.
- **Negative**: Network latency overhead on audio file transmission; requires fallback handling when the AI service is sleeping or unreachable.
