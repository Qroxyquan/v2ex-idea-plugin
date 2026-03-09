# V2EX Reader for IntelliJ IDEA

Browse V2EX directly inside IntelliJ IDEA without bouncing between your editor and a browser tab that definitely looks unrelated to work.

V2EX Reader is a Tool Window plugin for developers who want a clean in-IDE reading flow for topics, replies, search results, and lightweight interaction. It is built for IntelliJ IDEA `2024.2+` and targets a simple, fast, read-first experience.

## Features

- Browse V2EX feeds from built-in tabs such as `All`, `Hot`, `Tech`, `Creative`, `Play`, `Apple`, and `Jobs`.
- Load tab content using V2EX tab definitions, for example `all -> https://www.v2ex.com/?tab=all`.
- Open topic details inside IDEA and scroll through replies in the same view.
- Display reply count and latest activity time in the topic list.
- Search V2EX topics through an in-IDE Google results page using `site:v2ex.com/t`.
- Intercept topic links from the embedded search browser and open them as parsed topic detail pages.
- Show images from topic content and replies.
- Post replies inside IDEA when an `A2 Token` is configured.
- Keep the reply editor collapsed by default so the detail view stays clean.

## Why This Plugin Exists

Because context switching is expensive, and because "checking one V2EX thread" somehow always starts with opening a browser and ends with fifteen tabs.

This plugin keeps the whole flow inside IDEA:

- browse the homepage feed
- search without leaving the IDE
- open a topic
- read replies
- optionally reply if you are logged in

It is a focused workflow, not a full V2EX client.

## Screens and Flow

### Home Feed

- Topic list in a Tool Window
- Tab-based navigation
- Sorted by latest activity
- Reply count and activity time shown in each row

<img width="504" height="892" alt="Home feed" src="https://github.com/user-attachments/assets/5c78fe9c-805a-48a6-a092-4ce1603776fb" />

### Search

- Search opens an embedded browser inside IDEA
- Built-in navigation: `Home`, `Back`, `Forward`, `Refresh`
- Clicking a V2EX topic result opens the parsed topic detail view

<img width="524" height="822" alt="Search view" src="https://github.com/user-attachments/assets/2dc8c94b-b453-44ff-a9af-4ee02603623d" />

### Topic Detail

- Topic content rendered inside IDEA
- Replies shown in a scrollable detail page
- Image rendering supported for topics and replies
- `Refresh Detail` and `Open in Browser` actions available

<img width="524" height="848" alt="Topic detail" src="https://github.com/user-attachments/assets/66f58894-8d62-4423-a765-6b1c2b20b465" />

## Installation

### Install from ZIP

1. Build the plugin:

```bash
./gradlew buildPlugin
```

2. In IntelliJ IDEA, open:

```text
Settings -> Plugins -> Gear Icon -> Install Plugin from Disk...
```

3. Select the ZIP file from:

```text
build/distributions/
```

4. Restart the IDE.

## Configuration

Open:

```text
Settings -> Tools -> V2EX Reader
```

Available settings:

- `API Token`
  Used for V2EX API requests. Optional, but recommended for better request stability.

- `A2 Token`
  Used for logged-in actions such as posting replies.

## Compatibility

- IntelliJ Platform build: `242.*` to `252.*`
- Recommended IDE: IntelliJ IDEA `2024.2+`
- Build runtime: `JDK 21`

## Development

Run the plugin in a sandbox IDE:

```bash
./gradlew runIde
```

Run tests:

```bash
./gradlew test
```

Build the plugin package:

```bash
./gradlew buildPlugin
```

## Privacy and Legal

- [Privacy Policy](./PRIVACY_POLICY.md)
- [EULA](./EULA.md)
- [License](./LICENSE)

## Notes

- This is an unofficial plugin and is not affiliated with V2EX or JetBrains.
- Search quality depends on Google result availability in the current network environment.
- If V2EX changes its page structure, some HTML parsing features may require updates.

## Roadmap

- Better Marketplace presentation assets
- More robust topic search fallback strategies
- Additional tabs and richer topic metadata
- More polished logged-in interactions

## Contact

For feedback or Marketplace-related issues:

`ylinchunquan@outlook.com`
