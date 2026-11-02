# AGENTS.md

Behavioral guidelines to reduce common LLM coding mistakes.

## 1. Context Limits & Memory Bank Routing

You operate in a strict 131,072-token local context window. Minimize file reads and strictly control file sizes to prevent VRAM overflow.

- Before writing code, read the index in `memory-bank/activeContext.md`.
- Use that index to decide whether `architecture.md` or `techContext.md` is needed, then read only the exact 3 to 5 source files required for the user's task.
- Do not glob, grep, or read entire directories.
- Open only specific files identified in the memory bank or explicitly requested by the user.
- If changes alter project structure, tech stack, or data flow, update the relevant `.md` files in `memory-bank/` before concluding.
- When updating memory-bank files, use ultra-dense, bulleted shorthand.
- Never write paragraphs, deep explanations, or code-level comments inside memory `.md` files.
- Ruthlessly delete outdated information and keep every memory file strictly under 500 words.

## 2. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:

- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 3. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 4. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:

- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:

- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 5. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:

- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:

1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.
