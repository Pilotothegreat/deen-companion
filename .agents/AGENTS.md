# Custom Workspace Rules

- **Use Installed Skills**: A library of community skills has been registered at `C:/Users/king/.agents/skills` via `.agents/skills.json`. Always check this library or your available skills list first when tasked with code refactoring, database queries, security testing, cloud setup, or other specialized tasks, and load the relevant `SKILL.md` file using the `view_file` tool to follow its best practices.
- **No Perplexity for Coding or Planning**: Do NOT use Perplexity tools for coding, planning, reasoning, or pre-code workflows. All coding and planning tasks should be performed directly without invoking Perplexity.

# Memoirs & Custom Guidelines (Persisted Notes)

## Build & Release Protocol
1. **Gradle Build APK**: Run `./gradlew assembleDebug` to compile and package the latest version of the app.
2. **GitHub Push**: Commit and push changes to GitHub after completing major iterations (using standard Git commands: `git add`, `git commit`, `git push`).
3. **M3 Design Kits**: Align with Material 3 Expressive guidelines, using the custom shape-morphing wrapper (`MorphPolygonShape`) and centralized shapes/motion tokens.
4. **Direct Execution**: Perform all architectural design, planning, and code changes directly without Perplexity MCP calls.
5. **Command & Rule Logging**: Log all critical executed commands, instructions, and newly discovered project rules/protocols directly in `AGENTS.md` (or relevant workspace memoirs/logs) to maintain complete project history in the workstation.
6. **GitHub Context**: Proactively pull context, check repository status/history (`git log`, `git status`, etc.), and remain fully aware of the repository's state to guide decisions.
 state to guide decisions.
