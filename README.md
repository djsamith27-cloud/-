# Wild Survival 3D (වනාන්තර පැවැත්ම 3D)

A native Android 3D First-Person Forest Island Survival game built with Kotlin, Jetpack Compose, and OpenGL ES 2.0, plus a standalone 3D WebGL version (`index.html`).

---

## 📥 GitHub එකෙන් Download කරන ආකාරය (Download from GitHub)

### 1. Android APK එක Download කිරීම (Releases / Actions)
1. **Releases හරහා:** මෙම GitHub repository එකේ දකුණු පස ඇති **Releases** -> **Latest Android APK (`latest-apk`)** වෙත ගොස් **`WildSurvival3D-debug.apk`** file එක download කර Android දුරකථනයේ install කරන්න.
2. **Actions හරහා:** ඉහළින් ඇති **Actions** tab -> **Build Android APK** workflow එක මත click කර **Artifacts** යටතේ ඇති **`WildSurvival3D-debug-apk`** download කරගන්න.
3. **AI Studio සිට Push කිරීම:** Google AI Studio හි ඉහළ-දකුණු **Settings / Export -> Push to GitHub** මගින් මෙම project එක ඔබේ GitHub repository එකට push කළ වහාම ඉහත GitHub Actions මගින් ස්වයංක්‍රීයව APK එක build වී **Releases** හි පළ වේ.

### 2. Source Code හෝ Web Version (`index.html`) Download කිරීම
- GitHub හි කොළ පැහැති **<> Code** බොත්තම click කර **Download ZIP** තෝරන්න.
- **GitHub Pages** (`Settings -> Pages -> Deploy from main branch`) සක්‍රීය කිරීමෙන් `index.html` WebGL 3D ක්‍රීඩාව කෙලින්ම browser එකෙන්ද play කළ හැක.

---

## ✨ Features
- **3D OpenGL ES 2.0 Island World:** Procedural ocean with animated specular wave shimmer, grassy island, sandy beach, freshwater pond, pine & oak trees, rocks, berry bushes, drifting clouds, sun/moon cycle (extended 180s day/night), and flickering campfires.
- **First-Person Controls & Viewmodel:** Virtual analog joystick, multi-touch look-drag, sprint, jump, crosshair raycast prompts, animated 3D hand with Stone Axe, Hunting Spear, Hand Torch, Berries, and Herbal Bandage.
- **Auto-Generated Survival Tasks (ස්වයංක්‍රීය මෙහෙයුම්):** Contextual survival tasks generated automatically with live progress bars and bonus score rewards.
- **Survival & Nighttime Wolves AI:** Health, hunger, thirst, and stamina management, campfire warmth regeneration & refueling, and nighttime wolves repelled by campfires and hand torches.
- **Circular Navigation Minimap & Crafting Book (නිර්මාණ පොත):** Real-time circular minimap and mobile-optimized crafting book (`All`, `Tools`, `Camp`, `Food`).
- **Procedural Sound Synthesizer:** Native `AudioTrack` PCM synthesizer for footsteps, chopping, mining, berry picking, drinking, crafting, wolf howls, and damage effects.

---

## 🛠️ Local Build Instructions
```bash
# Restore debug keystore (if not present) and build debug APK
base64 -d debug.keystore.base64 > debug.keystore
gradle assembleDebug
```
The compiled APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.
