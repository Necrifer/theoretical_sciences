# Research puzzle authoring brief

Theoretical Sciences is a hard-sci-fi Minecraft mod. Each research node now has a puzzle: the player must BUILD an
equation from tiles (any algebraically equivalent arrangement is accepted - it is tested numerically) and then
SOLVE it for given values (they type the number; 1% tolerance). Hints can reveal what the variables mean, the method,
or the first tiles; paying expensive materials reveals the whole equation.

Read `/home/claude/qh/tools/puzzle_check.py` first - its docstring is the exact format and grammar, and it is the
validator you must pass.

## Your job
For every research id in your list file (`/home/claude/qh/tools/puzzles/_todo_N.txt`: `id | insight | title |
description`), write one puzzle into `/home/claude/qh/tools/puzzles/group_N.json` as `{ "<id>": {puzzle}, ... }`.

## Rules
- **Real maths/physics/chemistry/biology.** The equation must be a genuine, standard relation that the research topic is
  actually about (e.g. special_relativity -> Lorentz factor; casimir_effect -> Casimir pressure; thermodynamics_of_information
  -> Landauer limit; set_theory -> |P(S)| = 2^n; enzyme kinetics -> Michaelis-Menten; Arrhenius; Nernst; Hardy-Weinberg;
  logistic growth; radioactive decay; Schwarzschild radius; Shannon entropy; Euler characteristic ...). For speculative
  sci-fi topics (Omnilex, axiom theory, pleroma...), pick the closest real relation that captures the idea
  (e.g. Bekenstein bound, Kolmogorov complexity bound, Drake-like products, entropy of a de Sitter horizon, Gödel
  numbering via prime powers, cardinal arithmetic like 2^n > n, etc.) and say so in `goal`.
- **Difficulty grows with insight:** insight < 40: 3-7 tokens; < 150: 5-12 tokens; < 500: 8-16; higher: 10-22.
- **Tokens:** `expr` is space-separated tokens. Allowed: numbers, variables, + - * / ^ ( ), sqrt ln exp sin cos
  (each followed by a parenthesised argument), π. No implicit multiplication. Variables: short (1-4 chars), may be
  Greek/Unicode (γ, λ, ħ, ρ, ω, Δx, kB, c, G, M, T, N, n, k, p, r, v...). Keep them short and distinct.
- **Positive domain:** the expression must evaluate for random positive values 0.3-3 of every variable in at least 5 of
  8 tries (so avoid things like sqrt(1 - v^2/c^2) where v>c breaks it too often - rewrite or choose a different form, e.g.
  `1 / sqrt ( 1 - b ^ 2 )` breaks for b>1 in random tests; instead pick forms that stay real, or accept that the
  validator rejects it and choose another relation).
- **given:** clean values that give a sensible answer a player can compute with a calculator (avoid answers < 1e-6
  or > 1e9; use convenient units, e.g. c = 1, ħ = 1, or say "in units where ..." in `goal`).
- **distractors:** 2-5 plausible wrong tiles (an extra operator, a wrong power, a wrong constant).
- **vars:** each variable's meaning in plain words (what a hint reveals).
- **method:** 1-2 sentences explaining how to derive or recall it (what a hint reveals). Do not just restate the formula.
- **goal:** one sentence, starting with an action: "Find ...", "Derive ...", "Show how many ...". Mention units if any.
- `unit` optional (e.g. "J", "years", "").

## Finish
Run `python3 /home/claude/qh/tools/puzzle_check.py /home/claude/qh/tools/puzzles/group_N.json -v` until it prints
"N puzzles OK", and make sure every id from your list is present. Do not modify any other file. Report: count, and any
research where you were unsure the relation fits.
