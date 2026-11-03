"""
Round 13: research derivations in full mathematical form.

  * every research puzzle is rewritten (tools/puzzles2/*.json, checked by puzzle2.py): the relation in its accurate
    textbook form - integrals, derivatives, ∇, vectors, complex numbers, bra-kets, sums, tensors - drawn as a template
    whose boxes the player fills. Step 1 never names the law; Step 2 reveals its name and asks for a number.
  * every symbol is a glyph (tools/glyphs/*.json) learned through progression - by holding an item or finishing a
    research - and explained in the Book of Symbols.
  * Beyond the Omega asks for the whole Lagrangian: the Standard Model with gravity, the dark sector, thermodynamics,
    chemistry and information.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

LANG = {
    'puzzle.theoreticalsciences.step1': 'Step 1 - Derive it: fill every box',
    'puzzle.theoreticalsciences.step2': 'Step 2 - Solve:',
    'puzzle.theoreticalsciences.glossary': 'Book of Symbols',
    'puzzle.theoreticalsciences.missing_glyphs': 'You have not learned every symbol this derivation needs yet:',
    'puzzle.theoreticalsciences.missing_count': '%s symbol(s) not learned yet - see the hints panel',
    'puzzle.theoreticalsciences.not_equal': 'That is not the relation - check each box (order inside a sum or product does not matter).',
    'puzzle.theoreticalsciences.drop_here': 'Drag tiles into the boxes - hover a tile to see what it means',
    'glossary.theoreticalsciences.title': 'Book of Symbols',
    'glossary.theoreticalsciences.search': 'Search symbols...',
    'glossary.theoreticalsciences.empty': 'Nothing here.',
    'glossary.theoreticalsciences.unlock_start': 'Known from the start',
    'glossary.theoreticalsciences.unlock_item': 'Learned by holding: %s',
    'glossary.theoreticalsciences.unlock_research': 'Learned by completing: %s',
    'message.theoreticalsciences.glyphs_learned': 'New symbols learned: %s',
}


def apply(g):
    g['LANG'].update(LANG)


def finish(g):
    import puzzle2
    w, D = g['w'], g['D']
    glyphs = puzzle2.load_glyphs()
    P = puzzle2.load_puzzles()
    errs, warn = [], []
    puzzle2.check_glyphs(glyphs, errs)
    for rid, z in P.items(): puzzle2.check(rid, z, glyphs, errs, warn)
    if errs:
        raise SystemExit('round13: puzzle errors:\n' + '\n'.join(errs))
    w(os.path.join(D, 'ts_glyphs', 'glyphs.json'), {k: v for k, v in sorted(glyphs.items())})
    import glob
    n = 0
    for f in glob.glob(os.path.join(D, 'ts_research', '*.json')):
        rid = os.path.basename(f)[:-5]
        r = json.load(open(f))
        if rid in P and not r.get('grant_only'):
            r['puzzle'] = {k: v for k, v in P[rid].items() if not k.startswith('_')}
            w(f, r)
            n += 1
    print('round13 derivations merged:', n, 'glyphs:', len(glyphs))
    g['text_entry']('basics', 'symbols', 'The Book of Symbols', 'quantum_codex', [
        'Every research ends in a derivation: the situation is described, and the full relation is laid out in real notation with a box for every symbol. Fill them all from your tiles; the order inside a sum or product does not matter.',
        'The law is never named until you have derived it. Then its name appears, and you must use it - to compute a number from given values.',
        'Tiles are $(thing)symbols$(), and you only have the ones you have learned. Each is learned by holding a particular item, or by finishing a particular research. The $(thing)Book of Symbols$() (in the research tree and in every derivation) explains what each one means and how to learn it.'],
        sort=-1, spot=['research_terminal'])
