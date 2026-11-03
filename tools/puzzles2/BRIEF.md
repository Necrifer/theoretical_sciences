# Research puzzles v2 - authoring brief

Theoretical Sciences is a hard-sci-fi Minecraft mod with a research tree. Each research node has a two-step puzzle:

1. **Derive** - the player sees a description of a physical/mathematical situation (`goal`) and a TEMPLATE of the
   right-hand side: the real notation (integrals, derivatives, ∇, vectors, bra-kets, fractions, powers...) with an empty
   box for every variable, number, function name and +/− sign. They drag tiles into the boxes. The law is NOT named.
2. **Solve** - once derived, the law's `name` is revealed and they must compute a number (`ask`, `given`, `expr`).

Tiles are GLYPHS. Every glyph is unlocked by progression (holding/crafting a certain item, or completing a research).
The Research window has a glossary book explaining every glyph, so a player can work the derivation out themselves.

Read `/home/claude/qh/tools/puzzle2.py` - its docstring is the exact format and FORM grammar, and its `check()` is the
validator you must pass. See `/home/claude/qh/tools/puzzles2/_example.json` for a complete example.

## Your job (group N)
For every entry of `/home/claude/qh/tools/puzzles2/_todo_N.json` (research id, its progression wave, title, insight,
prerequisites, description, and the OLD simplistic puzzle for reference), write a new puzzle into
`/home/claude/qh/tools/puzzles2/group_N.json` as `{ "<id>": {puzzle}, ... }`, and every glyph you need that is not
already in `/home/claude/qh/tools/glyphs/base.json` into `/home/claude/qh/tools/glyphs/group_N.json`.
Do NOT edit base.json, the other groups' files, or anything else.

## The formulas
- Use the relation in its **full, accurate, textbook form**, the way it appears in a physics/maths text: with calculus,
  vectors, complex numbers, operators, sums, tensors where the real formula has them. Examples of the level wanted:
  - time-dependent Schrödinger: `i * ħ * pd ( ket ( ψ ) , t )` = `hat ( H ) * ket ( ψ )`  (lhs and form)
  - Maxwell–Ampère: `curl ( vec ( B ) )` = `μ0 * vec ( J ) + μ0 * ε0 * pd ( vec ( E ) , t )`
  - Planck spectrum integrated: `int ( 0 , ∞ , 8 * π * h * ν ^ 3 / c ^ 3 / ( exp ( h * ν / ( kB * T ) ) - 1 ) , ν )`
  - Fourier/Euler, Cauchy integral, Gaussian integral, Boltzmann distribution normalised by Z = Σ e^(−E/kT),
    Navier–Stokes, geodesic equation, Einstein field equations with indices, Dirac equation with γ^μ ∂_μ, Fermi's
    golden rule with |⟨f|V|i⟩|², Shannon entropy as −Σ p log p, Michaelis–Menten from d[P]/dt, Lotka–Volterra ODEs...
- Pick the relation the research is genuinely about (the old puzzle shows the intended topic; upgrade it to the full
  form, or choose a deeper, more fundamental relation on the same topic). Speculative topics (axioms, paradoxes,
  pleroma, simulation...) get the closest real mathematics (Gödel numbering, Cantor's diagonal/cardinal arithmetic,
  Bekenstein bound as an integral, Kolmogorov complexity, path integrals, Wheeler–DeWitt, ...).
- Size grows with insight: insight < 40: 6-12 slots; < 150: 10-20; < 500: 14-30; higher: 20-45 (see `slots()`).
- `goal` must NOT name the law, the equation, the effect or any person (the validator checks words of `name` against
  `goal`). Describe the physical situation and what to derive. 2-4 sentences.
- `name`: the standard name, e.g. "Stefan–Boltzmann law (integrated Planck spectrum)".
- `lhs`: in FORM grammar too (it is drawn but not filled).
- Step 2: `expr` (OLD numeric grammar of puzzle_check.py: + - * / ^ ( ), sqrt ln exp sin cos, π, numbers, variables -
  no other functions) is a closed form the player can compute from `given`, e.g. the integral evaluated, a special
  case, the equilibrium value. `ask` says exactly what to compute (it may give the evaluated integral or a standard
  result if a pure calculation would be unreasonable, but the player must still apply it to the given values).
  Answers between 1e-6 and 1e9, sensible units, clean given values.
- `vars`: meaning of every non-generic name in lhs/form/expr IN THIS PUZZLE. `method`: how to derive it (techniques,
  not the law's name). `distractors`: 2-6 tiles (leaf/number/function glyphs, or + / -).

## Glyphs (tiles) and their unlocks
- Every token in `lhs` and `form` (except numbers and ( ) ,) must be a glyph: in base.json or your group_N.json.
  Look at base.json first - many constants, variables, operators, calculus/vector/quantum heads and functions exist.
- New glyph entry: `"<token>": {"name": "...", "cat": "<constants|variables|operators|calculus|vectors|complex|quantum|relativity|thermo|chemistry|biology|sets|logic|indices|functions|fields|particles>", "meaning": "1-2 sentences a player can learn from", "unit": "...", "unlock": {...}}`
- Token style: short (1-4 characters), may be Unicode/Greek (Γ, ζ, ℏ is NOT used - use ħ, kB, mN, Tc, Fμν). A token's
  first character is drawn normally and the rest as a subscript, except two-letter Greek index pairs like μν.
  Tensors: `F _ μν`, `g ^ μν`, `γ ^ μ`, `pd` is ∂f/∂x; for ∂_μ write `∂ _ μ` with glyph `∂` (a leaf) if needed.
- `unlock`: `{"start": true}` (only for truly elementary symbols), `{"item": "<item id>"}` (unlocks the first time
  the player holds it), or `{"research": "<research id>"}` (unlocks when that research is complete). Make them
  THEMATIC: the symbol is learned from the thing it describes (ħ from a quantum item, Λ from dark energy research...).
- PROGRESSION RULE (checked): each glyph a puzzle uses must unlock no later than that research's wave
  (item wave <= research wave; research unlock wave = that research's wave + 0.5, so it must be a strictly earlier
  research). Tables: `/home/claude/qh/tools/glyphs/_items_by_wave.txt`, `/home/claude/qh/tools/glyphs/_research_by_wave.txt`.
  A glyph must never be unlocked by the very research whose puzzle needs it.
- If another group defines the same token, the earliest unlock wins at merge - so keep meanings general.

## Finish
Run `python3 /home/claude/qh/tools/puzzle2.py /home/claude/qh/tools/puzzles2/group_N.json -v` until it prints
"... puzzles OK" with no errors (warnings via -w are fine). Every id of your todo list must be present.
Report: count, glyphs added, and anything you were unsure about.
