# E.V MARK X

Workspace for three independent Android apps planned for this repository:

- **EV-AI-Assistant** — AI assistant for news and data analysis. Requires your own Gemini API key.
- **EV-Wallet** — local income and expense tracker in EGP.
- **EV-Calories** — local food and calorie log; calorie values are estimates.

## Upload status

The project source folders and build workflows still need to be added to this repository. Once uploaded, each app should live in its own folder and keep its own Gradle configuration and GitHub Actions workflow.

## APK builds

After the project files are uploaded, run the corresponding workflow from **Actions** and download the generated debug APK artifact. Debug APKs are not signed Play Store release builds.

## Privacy

Wallet and calorie entries are intended to be stored locally by the apps. Do not commit API keys or personal information.