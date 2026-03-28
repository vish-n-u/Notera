# Conversational AI Call Feature — Design Spec

**Date:** 2026-03-22
**Status:** Approved

---

## Overview

Add a "Talk to AI" feature that lets users have a real-time voice conversation with an AI assistant — like being on a phone call. The app streams mic audio directly to Google's Gemini Live API over WebSocket, receives audio back, and plays it in real time. At the end of the call, the user can optionally save the conversation transcript as a note.

---

## Goals

- Real-time, low-latency voice conversation (feels like a phone call, not a chatbot)
- Client-side only — no backend involvement, app talks directly to Gemini Live
- General assistant (no app note context in v1)
- Optional save: user chooses to save transcript as a note after the call ends

---

## Architecture

4 new components. Existing code is only minimally touched (FAB + route).

```
Homepage FAB ("Talk to AI")
        ↓
  AICallScreen          ← full-screen Compose UI
        ↓
  AICallViewModel       ← call state, audio in/out, transcript accumulation
        ↓
  GeminiLiveClient      ← OkHttp WebSocket wrapper
        ↓
  Gemini Live API (wss://generativelanguage.googleapis.com/...)
```

### New Files

| File | Purpose |
|------|---------|
| `pages/AICall/AICallScreen.kt` | Full-screen call UI composable |
| `pages/AICall/AICallViewModel.kt` | Call state management, audio pipeline |
| `pages/AICall/GeminiLiveClient.kt` | WebSocket framing, JSON send/receive |

### Existing Files Modified

| File | Change |
|------|--------|
| `Routes.kt` | Add `AICall` route |
| `pages/Homepage/ExtendableFAB.kt` | Add 5th FAB option: "Talk to AI" |
| `MainActivity.kt` | Add `AICallScreen` to NavHost |

---

## Call Screen UI

Full-screen dark surface. Three zones:

```
┌─────────────────────────┐
│  [timer]                │
│      "Notera AI"        │
│      ● Listening...     │  ← state label
│                         │
│      ╭───────╮          │
│      │  ~~~  │          │  ← animated pulsing circle (Canvas)
│      │  AI   │          │     slow pulse = listening
│      │  ~~~  │          │     fast pulse = AI speaking
│      ╰───────╯          │
│                         │
│   [🔇]         [📵]     │  ← Mute (left), End call red (right)
└─────────────────────────┘
```

**UI States:**
- `Connecting` — spinner, "Connecting..."
- `Listening` — slow pulse animation, "Listening..."
- `AI Speaking` — fast waveform pulse, "Speaking..."
- `Muted` — mute icon active, "Muted"
- `Ended` — dialog: "Save this conversation as a note?"

Call duration timer shown top-right (counts up from 0:00).

Animation: `Canvas`-drawn circle using `animateFloatAsState` — no external library.

---

## Audio Pipeline

### Mic → Gemini

```
AudioRecord (16kHz, mono, PCM 16-bit)
    → 640-byte chunks (20ms of audio)
    → Base64 encode
    → WebSocket JSON:
{
  "realtimeInput": {
    "mediaChunks": [{ "mimeType": "audio/pcm", "data": "<base64>" }]
  }
}
```

### Gemini → Speaker

```
WebSocket receive JSON
    → extract audio chunks (base64)
    → decode to PCM bytes
    → AudioTrack (16kHz, mono) → immediate playback
```

### Connection Setup (sent once on open)

```json
{
  "setup": {
    "model": "models/gemini-2.0-flash-live",
    "generationConfig": { "responseModalities": ["AUDIO"] },
    "systemInstruction": {
      "parts": [{ "text": "You are a helpful AI assistant. Keep responses conversational and concise." }]
    }
  }
}
```

### Interruption Handling

When the user speaks while AI audio is playing:
- `AudioRecord` continues streaming user audio to Gemini
- Gemini automatically cancels its current response and listens
- App stops `AudioTrack` playback and transitions to `Listening` state

### Transcript

Gemini optionally returns text alongside audio in server messages. Both user turns and AI turns are collected into a list. On save, they are concatenated into the note body.

---

## ViewModel State

```kotlin
sealed class CallState {
    object Connecting : CallState()
    object Listening : CallState()
    object AISpeaking : CallState()
    object Muted : CallState()
    data class Ended(val transcript: String) : CallState()
}
```

Exposed as `StateFlow<CallState>` to `AICallScreen`.

### Call Lifecycle

```
init → connect WebSocket → send setup → state = Connecting
WebSocket open confirmed → start AudioRecord → state = Listening
Receiving audio from Gemini → state = AISpeaking → play AudioTrack
AI done speaking → state = Listening (loop continues)
User taps End → stop AudioRecord + close WebSocket → state = Ended(transcript)
```

---

## Save Flow

Triggered when state = `Ended`:

1. Dialog shown: "Save this conversation as a note?"
2. **Save:** creates `AudioText(flowType = AddText, header = "AI Conversation", text = transcript)` via `appViewModel` → navigates to `EditPage` so user can review/edit before final save
3. **Discard:** `navController.popBackStack()` → back to Homepage

---

## API Key

Gemini API key stored in `local.properties`:
```
GEMINI_API_KEY=your_key_here
```
Accessed via `BuildConfig.GEMINI_API_KEY`. Never hardcoded in source.

`local.properties` is already in `.gitignore`.

---

## Permissions

`RECORD_AUDIO` — already declared in `AndroidManifest.xml`. No new permissions needed.

---

## Dependencies

One new dependency needed in `build.gradle.kts`:
```kotlin
// OkHttp already present via Retrofit — no new dep needed for WebSocket
```

Gemini Live uses OkHttp WebSocket which is already a transitive dependency via Retrofit. No additional libraries required.

---

## Out of Scope (v1)

- Notes-aware context (AI knowing about user's existing notes)
- Conversation history across calls
- Custom AI persona or voice selection
- Call recording (audio file save) — transcript only
