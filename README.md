# E.V MARK X

A workspace containing three independent Android apps:

- **EV-AI-Assistant** — AI assistant for news and data analysis. Requires your own Gemini API key.
- **EV-Wallet** — local income and expense tracker in EGP.
- **EV-Calories** — local food and calorie log; calorie values are estimates.

## Project files

The three standalone Android project folders are bundled in `EV-OMNI-Projects.zip`. Extract the archive to access each app's Gradle files, Android source, README, and GitHub Actions workflow.

## APK builds

The included workflows are stored inside each app folder. To run them independently, extract one app folder into the repository root of a separate repository, push to `main`, then open **Actions** and download the generated debug APK artifact. These are debug builds, not Play Store release-signed APKs.

## Privacy

Wallet and calorie entries are stored locally by the apps. Do not commit API keys or personal information.