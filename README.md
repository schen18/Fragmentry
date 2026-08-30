# Fragmentry

**Fragmentry** is a quiet, atmospheric, and non-linear space designed for capturing and exploring thought "fragments." It rejects the rigid structure of traditional note-taking apps in favor of an organic, "constellation-like" experience powered by on-device semantic analysis.

---

## 🌌 The Philosophy

Fragmentry is built on three core pillars:

1.  **The Void**: The app embraces negative space and deep obsidian backgrounds (`VoidBlack`). It is a space for thoughts to emerge and settle without visual clutter.
2.  **Organic over Linear**: Data is presented as a "field" or "constellation," not a rigid database table. Items are horizontally offset (organic jitter) based on their content, creating a unique, "shattered" look.
3.  **Quiet UX**: Interactions are intentionally subtle. We avoid "loud" UI elements, high-contrast borders, or frantic animations. Transition are "spectral"—slow, rhythmic fades that respect the user's focus.

---

## 📖 The Lexicon

To navigate the codebase or the application, one must understand its metaphorical glossary:

| Term | Technical Equivalent | Description |
| :--- | :--- | :--- |
| **Fragment** | Note / Entry | The primary unit of information (text + semantic embedding). |
| **Field** | Main List View | The non-linear landscape where all fragments reside. |
| **Spark** | Create / Capture | The act of bringing a new fragment into existence. |
| **Echo** | Detail View | A view where a fragment "resonates" with its peers. |
| **Resonance** | Similarity Search | Semantic relationships between fragments based on embeddings. |
| **Motif** | Key Tag / Theme | Recurrent concepts extracted automatically from fragment text. Inflected forms resonate as one — "dream" and "dreams" are a single motif. |
| **Constellation** | Category View | A group of fragments linked by a shared Motif, gathered semantically across surface forms. |
| **Collector** | Pinned / Favorites | A curated subset of fragments for "assembly." |
| **Veil** | Settings | The boundary layer for system tasks (Backup/Restore/Migration). |

---

## 🖥️ The Landscape (Screens)

### 1. The Field (Browsing)
The central anchor of the experience. Fragments appear in a non-linear list with staggered, organic entry animations.
- **Seek (Search)**: Tap "the field" label at the top to enter search mode. Seeking filters the field in real-time by raw text and automated Motifs — seeking "star" finds the "stars" thread.
- **Navigation**: A minimalist row of text labels at the bottom allows movement between screens.

### 2. The Spark (Capture)
A dedicated "Void Mode" for pure thought capture.
- All navigation is hidden to focus entirely on text entry.
- Supports long-form capture with a scrollable obsidian void.
- **Capture**: Tap the spectral "capture" affordance to preserve the thought.

### 3. The Echo (Exploration)
Accessed by tapping any fragment. The focused fragment takes center stage, while resonant thoughts drift into the "Chorus" below.
- **Motif Highlighting**: Recurring concepts are highlighted in `AmberPatina`, including their inflected forms — "dreams" glows when the shared motif is "dream". Tapping a Motif opens its **Constellation**.
- **Luminous state**: Pinned fragments emanate a soft amber aura.

### 4. The Constellation (Context)
A thread of all fragments sharing a Motif, gathered semantically — "dream" and "dreams" belong to the same constellation. The exploratory backstack ensures that pressing "Back" from an Echo returns you to your active thread.

### 5. The Collector (Assembly)
A curated space for your most important fragments.
- **The Shelf**: An ambient horizontal row of collected shards at the bottom of the Field.
- **Assembly**: A dedicated view for arranging and reviewing your curated thoughts.

### 6. The Veil (Maintenance)
A quiet space for system-level operations.
- **Preserve**: Export your entire memory field to a portable, versioned JSON file.
- **Summon**: Restore fragments from a backup. The "Smart Restore" logic automatically skips duplicates.
- **Migrate**: Import fragments from a plain text file (chunks separated by `___`), with quiet progress reporting and duplicate detection.

---

## ✨ Primary Interactions

- **Spark**: Bringing thoughts into the field via the capture screen.
- **Dissolve**: Long-press any fragment anywhere (Field, Shelf, Constellation) to thematically return it to the void. A quiet "recall" affordance follows every dissolution — nothing is lost by accident.
- **Resonate**: Automatic discovery of semantic links between fragments using on-device ML.
- **Collect**: Pinning a fragment to add it to your luminous shelf.

---

## 🛠️ Technical Underpinnings

- **100% Jetpack Compose**: A modern, declarative UI stack with custom atmospheric theming.
- **On-Device ML**: Uses **TensorFlow Lite** (`all-MiniLM-L6-v2-qint8`) to generate 384-dimensional semantic embeddings entirely offline. Fragments captured before the model settles are re-embedded automatically on the next launch, and the same embeddings unify inflected motifs into single constellations.
- **Room Persistence**: High-performance local storage with materialized semantic tags, indexed queries, versioned schema exports, and non-destructive migrations — thought history survives upgrades.
- **State Resilience**: The navigation stack, the focused fragment, and the active constellation all persist across system process death.

---

> [!IMPORTANT]
> **Privacy First**: All semantic analysis and storage happen on-device, and platform cloud backups exclude the database entirely. Your thoughts never leave the local void unless you deliberately export a backup through the Veil.
