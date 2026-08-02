# 🛡️ UrgentCall Guard

> Ne ratez plus jamais un appel urgent, même en mode silencieux.
> Never miss an urgent call again, even in silent mode.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

**🇫🇷 [Français](#-français) &nbsp;|&nbsp; 🇬🇧 [English](#-english)**

---

## 🇫🇷 Français

### Qu'est-ce que c'est ?

**UrgentCall Guard** est une application Android qui surveille les appels reçus lorsque votre téléphone est en mode silencieux ou à faible volume. Si un appel est manqué et que l'appelant n'est ni dans votre liste blanche ni dans votre liste noire, l'application lui envoie automatiquement un SMS l'informant qu'un rappel dans les minutes qui suivent fera sonner votre téléphone à volume maximal — même en silencieux.

Vous restez ainsi joignable en cas d'urgence, sans sonner en permanence pour le reste.

### Fonctionnalités

- 🔇 **Détection automatique** des appels manqués en volume bas ou silencieux
- 📩 **SMS automatique** au numéro manqué, l'informant de la fenêtre de rappel
- 🔊 **Volume forcé au maximum** si l'appelant rappelle dans la fenêtre définie
- ⏱️ **Seuil de volume et durée de la fenêtre de rappel** entièrement configurables
- ✅ **Liste blanche** : contacts qui font toujours sonner le téléphone normalement
- 🚫 **Liste noire** : numéros ignorés (pas de SMS, pas de volume forcé), avec options pour bloquer numéros masqués/inconnus
- 🚨 **Numéros d'urgence reconnus automatiquement** (112, 15, 17, 18, 911, 999…) — toujours prioritaires, sans configuration
- 🔍 **Recherche dans les contacts** avec autocomplétion pour ajouter rapidement un numéro
- 🔔 **Notification discrète et permanente**, qui reflète l'état réel du volume, et dont le contenu s'actualise en temps réel
- 🔒 **100 % local** : aucune donnée (numéros, SMS, contacts) n'est envoyée à un serveur ou un tiers
- 💾 **Réglages sauvegardés** même en cas de désinstallation (via la sauvegarde Android)

### Pourquoi ces autorisations ?

| Permission | Pourquoi |
|---|---|
| Téléphone (`READ_PHONE_STATE`) | Détecter qu'un appel arrive |
| Contacts (`READ_CONTACTS`) | Identifier l'appelant et permettre la recherche/autocomplétion |
| SMS (`SEND_SMS`) | Envoyer le SMS automatique de rappel |
| Ne pas déranger (`ACCESS_NOTIFICATION_POLICY`) | Lire le mode sonnerie actuel et ajuster le volume |
| Notifications (`POST_NOTIFICATIONS`) | Afficher la notification de surveillance |

Rien de tout cela ne quitte votre téléphone — aucun serveur, aucune télémétrie, aucune publicité.

### Installation

Deux façons d'obtenir l'application :

1. **Depuis les releases GitHub** : téléchargez le dernier `.apk` depuis l'onglet [Releases](../../releases), puis installez-le (autorisez l'installation depuis des sources inconnues si demandé).
2. **En compilant vous-même** :
   ```bash
   git clone https://github.com/<votre-compte>/UrgentCallGuard.git
   cd UrgentCallGuard
   ./gradlew assembleDebug
   ```
   L'APK debug se trouve ensuite dans `app/build/outputs/apk/debug/`.

### Compilation d'une version release signée

Le dépôt inclut un workflow GitHub Actions (`.github/workflows/build-apk.yml`) qui génère un **Android App Bundle (`.aab`) signé**, prêt pour le Google Play Store. Il nécessite 4 secrets configurés dans les réglages du dépôt (`Settings → Secrets and variables → Actions`) :

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Voir la section [Contribuer](#contribuer--contributing) pour plus de détails techniques.

### Stack technique

- **Kotlin** natif, sans framework externe
- Android SDK — `minSdk 26` (Android 8.0), `targetSdk`/`compileSdk 35`
- Aucune dépendance réseau, aucun backend, aucune base de données externe
- Stockage local via `SharedPreferences`

### Feuille de route / limitations connues

- La permission `SEND_SMS` est soumise à la politique Google Play sur les permissions sensibles ; la publication sur le Play Store nécessite de remplir un formulaire de déclaration justifiant cet usage.
- Le comportement de mise en veille du service peut varier selon les constructeurs (Xiaomi, Huawei, Oppo…) qui imposent des gestionnaires de batterie plus agressifs que le standard Android.

### Soutenir le projet

L'application restera toujours gratuite. Si elle vous rend service, un don via [PayPal](https://www.paypal.me/lokoyote) aide à financer son développement. 💙

### Licence

Ce projet est distribué sous licence **[GNU General Public License v3.0 (GPL-3.0)](LICENSE)**.

En résumé : vous êtes libre d'utiliser, d'étudier, de modifier et de redistribuer ce code, y compris à des fins commerciales, à condition que toute version modifiée ou dérivée reste elle aussi sous GPL-3.0 et que son code source soit mis à disposition. Le texte intégral et juridiquement contraignant se trouve dans le fichier [`LICENSE`](LICENSE).

---

## 🇬🇧 English

### What is this?

**UrgentCall Guard** is an Android app that watches incoming calls while your phone is on silent mode or at low volume. If a call is missed and the caller is neither whitelisted nor blacklisted, the app automatically sends them an SMS explaining that calling back within the next few minutes will make your phone ring at maximum volume — even in silent mode.

You stay reachable in a genuine emergency, without your phone ringing loudly for everything else.

### Features

- 🔇 **Automatic detection** of missed calls while on low volume or silent mode
- 📩 **Automatic SMS** to the missed caller, explaining the recall window
- 🔊 **Forced maximum volume** if the caller calls back within the configured window
- ⏱️ **Fully configurable** volume threshold and recall window duration
- ✅ **Whitelist**: contacts who always ring normally, no matter the phone's volume state
- 🚫 **Blacklist**: ignored numbers (no SMS, no forced volume), with options to block hidden/unknown numbers
- 🚨 **Emergency numbers recognized automatically** (112, 911, 999, 15, 17, 18…) — always take priority, no setup needed
- 🔍 **Contact search with autocomplete** for quickly adding a number
- 🔔 **Discreet, persistent notification** that reflects the real volume state and updates live
- 🔒 **100% local**: no data (numbers, SMS content, contacts) is ever sent to a server or third party
- 💾 **Settings persist** across uninstalls (via Android's backup system)

### Why these permissions?

| Permission | Why |
|---|---|
| Phone (`READ_PHONE_STATE`) | Detect an incoming call |
| Contacts (`READ_CONTACTS`) | Identify the caller and enable search/autocomplete |
| SMS (`SEND_SMS`) | Send the automatic recall SMS |
| Do Not Disturb access (`ACCESS_NOTIFICATION_POLICY`) | Read the current ringer mode and adjust volume |
| Notifications (`POST_NOTIFICATIONS`) | Display the monitoring notification |

None of this ever leaves your phone — no server, no telemetry, no ads.

### Installation

Two ways to get the app:

1. **From GitHub Releases**: download the latest `.apk` from the [Releases](../../releases) tab, then install it (allow installs from unknown sources if prompted).
2. **Build it yourself**:
   ```bash
   git clone https://github.com/<your-account>/UrgentCallGuard.git
   cd UrgentCallGuard
   ./gradlew assembleDebug
   ```
   The debug APK will be in `app/build/outputs/apk/debug/`.

### Building a signed release

The repo includes a GitHub Actions workflow (`.github/workflows/build-apk.yml`) that produces a **signed Android App Bundle (`.aab`)**, ready for the Google Play Store. It requires 4 secrets set in the repo settings (`Settings → Secrets and variables → Actions`):

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

See [Contributing](#contribuer--contributing) for technical details.

### Tech stack

- Native **Kotlin**, no external framework
- Android SDK — `minSdk 26` (Android 8.0), `targetSdk`/`compileSdk 35`
- No network dependency, no backend, no external database
- Local storage via `SharedPreferences`

### Roadmap / known limitations

- The `SEND_SMS` permission falls under Google Play's sensitive permissions policy; publishing on the Play Store requires filling out a permissions declaration form justifying this use case.
- Background service behavior may vary on manufacturers (Xiaomi, Huawei, Oppo…) with battery managers more aggressive than stock Android.

### Support the project

The app will always stay free. If it's useful to you, a donation via [PayPal](https://www.paypal.me/lokoyote) helps fund its development. 💙

### License

This project is licensed under the **[GNU General Public License v3.0 (GPL-3.0)](LICENSE)**.

In short: you're free to use, study, modify, and redistribute this code — including commercially — as long as any modified or derivative version stays under GPL-3.0 too, with its source code made available. See the [`LICENSE`](LICENSE) file for the full, legally binding text.

---

## Contribuer / Contributing

Les *pull requests* sont bienvenues. Pour toute modification majeure, ouvrez d'abord une *issue* pour en discuter.
Pull requests are welcome. For major changes, please open an issue first to discuss what you'd like to change.

```bash
./gradlew assembleDebug     # build debug APK
./gradlew bundleRelease     # build signed release AAB (needs signing env vars)
```
