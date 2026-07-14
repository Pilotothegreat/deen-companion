# Custom Workspace Rules

- **Use Installed Skills**: A library of community skills has been registered at `C:/Users/king/.agents/skills` via `.agents/skills.json`. Always check this library or your available skills list first when tasked with code refactoring, database queries, security testing, cloud setup, or other specialized tasks, and load the relevant `SKILL.md` file using the `view_file` tool to follow its best practices.

# Perplexity MCP — Agent Rules (Claude Sonnet 5.0 Thinking Priority)

You have access to the Perplexity MCP server. ALWAYS use these tools before writing
any code. NEVER use your internal knowledge for research — always verify via Perplexity.

## Primary Rule: Use Claude Sonnet 5.0 Thinking Everywhere Possible

The preferred model for ALL reasoning, planning, and research tasks is:
  model: "claude50sonnetthinking"

This applies to perplexity_reason, perplexity_ask, and perplexity_compute.
Do NOT default to Gemini 3.5 Flash or pplx_pro when claude50sonnetthinking is available.
If claude50sonnetthinking returns an unsupported model error, fall back to claude46sonnetthinking.

## Tool Selection — Ordered by Task

1. QUICK LOOKUP (docs, API versions, current events)
   → perplexity_search  [model: pplx_pro, no override needed]

2. SYNTHESIZED ANSWER WITH CITATIONS
   → perplexity_ask  [model: "claude50sonnetthinking"]

3. ARCHITECTURE / PLANNING / DEBUGGING STRATEGY
   → perplexity_reason  [model: "claude50sonnetthinking"]  ← USE THIS MOST

4. DEEP MULTI-SECTION REPORTS
   → perplexity_research  [model: pplx_alpha, then summarize with perplexity_reason]

5. MATH / MODELING / CODE EXECUTION ANALYSIS
   → perplexity_compute  [model: "claude50sonnetthinking"]

## Standard Invocation Templates

### For reasoning/planning (most common):
{
  "tool": "perplexity_reason",
  "arguments": {
    "query": "<your question>",
    "model": "claude50sonnetthinking",
    "sources": ["web"],
    "language": "en-US"
  }
}

### For cited answers:
{
  "tool": "perplexity_ask",
  "arguments": {
    "query": "<your question>",
    "model": "claude50sonnetthinking",
    "mode": "copilot",
    "sources": ["web"],
    "language": "en-US"
  }
}

### For security/CVE/academic research:
{
  "tool": "perplexity_reason",
  "arguments": {
    "query": "<your question>",
    "model": "claude50sonnetthinking",
    "sources": ["scholar", "web"],
    "language": "en-US"
  }
}

### For community/developer opinions:
{
  "tool": "perplexity_ask",
  "arguments": {
    "query": "<your question>",
    "model": "claude50sonnetthinking",
    "sources": ["social"],
    "language": "en-US"
  }
}

## Deen Companion Android App — Mandatory Pre-Code Workflow

NEVER write Kotlin/Jetpack Compose code without first running these steps:

Step 1 — PLAN with Claude Sonnet 5.0 Thinking:
{
  "tool": "perplexity_reason",
  "arguments": {
    "query": "Design the full architecture for [feature] in Jetpack Compose with Material Design 3. Include ViewModel state, navigation, RTL Arabic support where relevant, and a step-by-step implementation plan.",
    "model": "claude50sonnetthinking",
    "sources": ["web"],
    "language": "en-US"
  }
}

Step 2 — VERIFY latest APIs:
{
  "tool": "perplexity_search",
  "arguments": {
    "query": "latest Jetpack Compose [component] API 2026",
    "sources": ["web"],
    "language": "en-US"
  }
}

Step 3 — Then write the code based on the plan and verified docs.

## Source Guide

| Topic                              | sources value             |
|------------------------------------|---------------------------|
| Android / Compose / Kotlin         | ["web"]                   |
| Islamic content, Quran APIs, Hadith| ["scholar", "web"]        |
| Prayer time libs, Arabic rendering | ["web"]                   |
| Pentesting / CVEs / security tools | ["scholar", "web"]        |
| Developer community opinions       | ["social"]                |
| CTF writeups / exploit techniques  | ["web", "social"]         |

## Rate Limit Safety

- Max 3 consecutive Perplexity tool calls before pausing
- If vault is locked: run `npx perplexity-user-mcp doctor` in terminal
- If model errors: run `perplexity_models` tool to confirm available model IDs

## Check Available Models (run once after setup)

{
  "tool": "perplexity_models",
  "arguments": {}
}

Confirm "claude50sonnetthinking" appears in the output before relying on it.

# Memoirs & Custom Guidelines (Persisted Notes)

## Build & Release Protocol
1. **Gradle Build APK**: Run `./gradlew assembleDebug` to compile and package the latest version of the app.
2. **GitHub Push**: Commit and push changes to GitHub after completing major iterations (using standard Git commands: `git add`, `git commit`, `git push`).
3. **M3 Design Kits**: Align with Material 3 Expressive guidelines, using the custom shape-morphing wrapper (`MorphPolygonShape`) and centralized shapes/motion tokens.
4. **Model Selection**: Always use `claude50sonnetthinking` on Perplexity MCP for reasoning, planning, and coding architecture tasks.
5. **Sonnet 5 Guidance**: Leverage `claude50sonnetthinking` for all architectural designs, complex plans, and code generation due to its superior capabilities. Use Perplexity tools extensively to research new topics, verify library APIs, and fetch correct reference documentation before writing code.
6. **Command & Rule Logging**: Log all critical executed commands, instructions, and newly discovered project rules/protocols directly in `AGENTS.md` (or relevant workspace memoirs/logs) to maintain complete project history in the workstation.
7. **GitHub Context**: Note that Sonnet 5 is connected directly to the user's GitHub through the GitHub connector. Proactively pull context, check repository status/history (`git log`, `git status`, etc.), and remain fully aware of the repository's state to guide decisions.
