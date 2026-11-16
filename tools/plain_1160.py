"""
1.16.0: first batch of "In plain words" drafts for the Codex (the hardest subjects first: sets and infinities,
imaginary and higher numbers, quantum physics, general relativity).

Each entry gets three short lines, shown as the entry's first page:
  line   : what it is, in one plain sentence, no symbols
  like   : an everyday comparison
  breaks : where that comparison stops being true

Drafts for the author to rewrite; tools/export_text.py exports them as "11 Codex - Plain words (draft).docx" and
tools/import_text.py reads them back like every other text.

Run:  python3 tools/plain_1160.py   (writes the "plain" field into ts_codex/entries.json)
"""
import json, os

HERE = os.path.dirname(os.path.abspath(__file__))
ENTRIES = os.path.join(HERE, '..', 'src/main/resources/assets/theoreticalsciences/ts_codex/entries.json')

P = {
    # ------------------------------------------------------------ mathematics
    'mathematics/math_overview': (
        "To reach the biggest infinity you first rebuild mathematics, one branch at a time, the way a ladder needs every rung.",
        "Learning a language: letters, then words, then sentences, then poems. You cannot skip to the poems.",
        "A language is invented by people. Many mathematicians think the rules of mathematics would hold even if nobody wrote them down."),
    'mathematics/logic_sets': (
        "A set is just a collection of things. Out of the empty collection, and collections of collections, you can build every number there is.",
        "Boxes inside boxes. An empty box is zero; a box holding that empty box is one; a box holding both is two; and so on.",
        "Real boxes take up room and can only nest so far. Sets weigh nothing and can nest forever, which is how infinite sets appear."),
    'mathematics/numbers': (
        "Each kind of number was invented to answer a question the last kind could not: negatives to take 5 from 3, fractions to share, and imaginary numbers to take the square root of a negative.",
        "Tools in a workshop. Every new tool does a job the old ones could not, and you keep all the old tools too.",
        "Tools are separate objects. Each number system contains the one before it completely: every whole number is also a fraction, a real number and a complex number."),
    'mathematics/transfinite': (
        "Some infinities are bigger than others. The counting numbers go on forever, but the points on a line are a strictly larger forever.",
        "A hotel with endless rooms. Even when every room is full, it can still fit endless new guests by moving everyone along. But a crowd as big as the points on a line will never fit, however you shuffle.",
        "No real hotel has endless rooms. Here \"bigger\" means only one thing: you cannot pair the two crowds up one to one without someone left over."),
    'mathematics/r72_foundations': (
        "All of mathematics rests on a few starting rules, the empty set, and the act of counting.",
        "The foundations of a house: nobody admires them, but everything above is only as steady as they are.",
        "A house can be rebuilt on better foundations. In mathematics the starting rules are chosen, and choosing different ones gives a different, equally valid mathematics."),
    'mathematics/r83_euler': (
        "Five of the most important numbers in mathematics, 0, 1, pi, e and the imaginary unit i, fit into one short, exact sentence.",
        "Walking half way round a circle of radius one. You start at 1 and finish at minus 1; adding 1 brings you back to 0.",
        "You cannot walk \"i steps\". The imaginary unit is not a distance but a quarter turn, a direction at right angles to the ordinary number line."),
    'mathematics/r83_e_everywhere': (
        "The number e, about 2.718, appears whenever something grows or shrinks at a speed that depends on how much of it there is.",
        "Money in a bank that pays interest on the interest, compounded every instant.",
        "Banks round to the nearest coin and pay at set times. e comes from the limit where the steps become infinitely small and many."),
    'mathematics/r86_continuation': (
        "A smooth function known in one small region can be stretched, step by step, to tell you its values far away.",
        "Following a winding road at night with a torch: you only ever see the next stretch, but each stretch tells you where the next one goes.",
        "A road could turn suddenly in the dark. A smooth (analytic) function cannot: what it does in any tiny patch fixes what it does everywhere it reaches."),
    'mathematics/analysis': (
        "Calculus studies change by cutting it into infinitely many, infinitely small pieces, then adding them back up.",
        "A curved path drawn as a staircase of tiny straight steps. The smaller the steps, the closer the staircase hugs the curve.",
        "Any real staircase still has steps. Calculus asks what happens in the limit, when there are no steps left at all."),
    'mathematics/platonic_realm': (
        "A world where mathematical objects exist as real things, because the mod takes seriously the old idea that they live somewhere apart from us.",
        "A library that holds every book that could ever be written correctly. People do not write the books; they only find them.",
        "Nobody has ever visited such a place. It is a philosophical idea, not an observed one; the realm in the mod is a way to picture it."),
    'mathematics/r77_one_realm': (
        "The different infinities are not a separate world: they sit at the top of the same world as everyday mathematics, far above it.",
        "A mountain whose foothills are ordinary numbers and whose peak, hidden in the clouds, is infinity.",
        "You could climb a real mountain. You can never count your way up to infinity; you can only reach it by a new rule that says \"and then all of those\"."),
    'mathematics/fractal_space': (
        "A fractal is a shape that looks the same when you zoom into it, again and again, without end.",
        "A fern: each leaf looks like a small copy of the whole plant, and each leaflet like a copy of the leaf.",
        "A real fern stops after a few levels. A mathematical fractal never stops, however far you zoom."),
    'endgame/beyond': (
        "Every set of mathematical rules big enough for arithmetic has true statements it can never prove, and some rules can even contradict themselves.",
        "A rulebook for a game that cannot settle every argument the game can produce, however many rules you add.",
        "A rulebook can be patched with a new rule. Gödel showed that any patch creates new unprovable statements of its own."),
    'endgame/cantor_diagonalizer': (
        "However you list the numbers, you can always build one that is not on the list, so the numbers on a line can never all be listed.",
        "A guest list. Write a new name whose first letter differs from guest one's, second letter from guest two's, and so on: that name cannot be anyone on the list.",
        "A guest list ends. Cantor's trick works even on a list that never ends, which is what makes it surprising."),
    'mathematics/quantum_computing': (
        "A quantum computer keeps many possible answers in play at once and nudges the wrong ones to cancel out, so it finds some answers in far fewer steps.",
        "Finding a name in an unsorted phone book. Checking page by page takes about half the book; Grover's search needs only about the square root as many looks.",
        "It does not simply try every answer at once and read them all off. Only one answer comes out per run, and the art is arranging for it to be the right one."),

    # ------------------------------------------------------------ quantum physics
    'quantum/quantum_foam': (
        "At the very smallest scale, far smaller than an atom, space itself may not be smooth but bubbling and uncertain.",
        "The sea seen from an aeroplane looks flat; up close it is churning waves and spray.",
        "Waves on the sea are made of water moving in space. Quantum foam would be space and time themselves fluctuating, with nothing underneath."),
    'quantum/foam_boundaries': (
        "The foam has two limits: below, where nothing has been decided yet, and above, where both of our best theories fail at the same time.",
        "The edges of a map, marked \"here be dragons\": not walls, but the places our charts stop being reliable.",
        "In the mod these edges are solid. In reality they are limits of our understanding, not places you could touch."),
    'quantum/quantum_entangler': (
        "Two particles can share one joint state, so measuring one instantly tells you what the other will show, however far apart they are.",
        "A pair of gloves posted in two boxes. Open one, find a left glove, and you know the other is the right.",
        "The gloves were left and right all along. Entangled particles have no definite answer until measured, and experiments (Bell tests) prove the glove picture cannot explain what we see. Still, no message can travel this way."),
    'quantum/casimir_press': (
        "Two metal plates very close together in empty space are pushed towards each other, because empty space is full of tiny fluctuations and fewer fit between the plates than outside.",
        "Two boats moored close in a choppy harbour drift together, because fewer waves fit between them than outside.",
        "The harbour is full of water. The plates are pushed by the fluctuations of a vacuum that has nothing in it at all."),
    'quantum/bose_einstein_condenser': (
        "Cool certain atoms to almost absolute zero and they stop behaving as separate atoms and move together as one giant wave.",
        "A stadium crowd doing a Mexican wave: thousands of people moving as one pattern.",
        "Every person in a crowd is still a separate person you could point to. In a condensate the atoms are truly in the same state; you cannot say which atom is which."),
    'biology/quantum_life': (
        "Living things use quantum effects: plants pass energy along many routes at once, and birds may sense north with entangled electrons.",
        "A delivery driver who tries every street at the same moment and arrives by the quickest.",
        "No driver can do that. Quantum waves can, but only for a very short time before the warm, wet cell scrambles them."),
    'chemistry/bonds': (
        "Atoms join because sharing an electron spread over both of them takes less energy than keeping it on one.",
        "Two neighbours sharing one large garden instead of two small ones, because together it costs them less to keep.",
        "Neighbours choose. The electron is not a ball split between houses; it is a single wave spread over both atoms at once."),
    'basics/guide_08_casimir': (
        "Empty space is not truly empty, and that leftover energy can push on things.",
        "A calm pond still trembles with tiny ripples you can only see up close.",
        "A pond's ripples can stop. The vacuum's fluctuations can never be removed; they are the lowest energy anything can have."),
    'basics/guide_10_zpe': (
        "Zero point energy is the energy that remains in a system even at its absolute lowest state.",
        "A guitar string that hums very faintly even when nobody plays it.",
        "A real string can be stilled. Quantum rules forbid perfect stillness, so this energy is always there. (Tapping it for power is the mod's fiction; in reality it cannot run a generator.)"),

    # ------------------------------------------------------------ general relativity
    'spacetime/relativity': (
        "Gravity is not a pull but a bending of space and time, and things simply follow the straightest path through that bent shape.",
        "A heavy ball on a stretched rubber sheet: marbles rolled past it curve in, as if pulled.",
        "The sheet needs gravity underneath to sag, and it only bends space. Real spacetime bends time as well, and that bending of time is most of what makes things fall."),
    'spacetime/r47_redshift': (
        "Light climbing away from a heavy object loses a little energy and turns slightly redder.",
        "A ball thrown up a hill slows as it climbs.",
        "Light does not slow down; it always moves at the same speed. It loses energy by stretching its wave, not by slowing."),
    'spacetime/r47_frame_drag': (
        "A spinning heavy object drags space around with it, the way a spoon drags honey.",
        "Stirring thick honey: things floating near the spoon get carried round.",
        "Honey is a substance. Space is not made of anything, yet it is still dragged."),
    'spacetime/r47_waves': (
        "When huge masses move violently, ripples in space itself spread out at the speed of light, stretching and squeezing everything they pass.",
        "Ripples spreading on a pond after a stone falls in.",
        "Pond ripples move water up and down. Gravitational waves stretch distances themselves, by less than a thousandth of a proton's width over kilometres."),
    'spacetime/r50_cow': (
        "Gravity was shown to act on a single particle while it travelled as a wave along two paths at once.",
        "Two runners on two tracks, one slightly uphill. The uphill runner falls a little behind, so they finish out of step.",
        "The runners are two people. Here it is one neutron taking both routes at the same time and arriving out of step with itself."),
    'spacetime/r50_hawking': (
        "A fast flowing fluid can trap sound the way a black hole traps light, and that lets us test black hole ideas in a lab.",
        "Swimming against a river that flows faster than you can swim: past a certain line you can never get upstream.",
        "A swimmer is pushed by the water. Light near a black hole is not pushed; space itself is flowing inward faster than light can travel through it."),
    'spacetime/r50_unification': (
        "Our theory of the very small (quantum physics) and of gravity (relativity) disagree, and joining them is one of the great open problems.",
        "Two maps of the same country drawn at different scales that do not match at the edges.",
        "Maps can be redrawn to match. Nobody yet knows what the joined theory is, or whether space itself is made of pieces."),
    'spacetime/r53_dilation': (
        "Time passes more slowly deep in a gravity well. A clock near a heavy mass ticks slower than one far away.",
        "Walking on a moving walkway beside a friend on the floor: you both walk, but you cover different ground.",
        "The walkway is something extra under your feet. Nothing is added to the slow clock; time itself runs at a different rate there, and both clocks feel completely normal."),
    'spacetime/r53_lensing': (
        "Mass bends the path of light, so a heavy object can act like a lens and show a distant star as a ring.",
        "Looking at a light through the bottom of a wine glass: it smears into a ring.",
        "Glass bends light by slowing it. Gravity bends light by curving the space it travels through; the light never slows."),
    'spacetime/r53_drag': (
        "Spinning masses twist space around them, and the difference in pull between your head and feet stretches you: those are tides.",
        "The Moon pulls the near side of the Earth's oceans harder than the far side, making two bulges.",
        "Ocean tides are gentle because the Moon is far away. Near a small black hole the difference is so steep it would stretch a person like spaghetti."),
    'spacetime/r53_mergers': (
        "Two dead stars or black holes spiral together, losing energy to gravitational waves, and the wave rises in pitch until they merge.",
        "A coin spinning down into a funnel goes round faster and faster as it falls.",
        "The coin slows because of friction. The stars lose energy by shaking space itself."),
    'spacetime/r53_warp': (
        "A warp bubble would squeeze space in front and stretch it behind, carrying a ship without the ship moving through space.",
        "Standing still on a moving walkway: you travel without walking.",
        "A walkway is a real machine. A warp bubble needs negative energy in amounts nobody knows how to make; it is allowed by the equations, not built."),
    'spacetime/r54_er_epr': (
        "One bold idea says two entangled particles are joined by a tiny wormhole, so entanglement and the shape of space may be the same thing.",
        "Two rooms connected by a secret corridor nobody can walk down.",
        "This is a proposal, not a proven fact; and no corridor here could carry anything faster than light."),
    'spacetime/r54_information': (
        "If a black hole evaporates into plain heat, the information about what fell in seems lost; but quantum physics says information can never be destroyed.",
        "Burning a letter: in principle the smoke and ash still hold every word, however hard to read.",
        "We know how ash comes from paper. Nobody yet knows for certain how the information gets out of a black hole."),
    'spacetime/r54_quantum_geometry': (
        "Some theories say space is not smooth but built from tiny pieces, so areas and volumes come in steps.",
        "A photo on a screen looks smooth until you zoom in and see the pixels.",
        "Pixels sit on a grid inside a screen. These pieces of space would not sit inside anything; they would be space."),
    'spacetime/r54_unruh': (
        "An accelerating observer sees empty space as warm and glowing, while a calm observer sees it dark and cold.",
        "Running through still air feels like wind on your face.",
        "The wind is real air you push through. The Unruh glow depends only on how you move; the same empty space is cold for someone else."),
    'spacetime/black_hole_interior': (
        "Inside a black hole every path leads further in; the centre is not a place you avoid but a moment in your future.",
        "A one way waterfall: past the edge every direction you can swim still goes downstream.",
        "Water falls through space. Inside a black hole space and time swap roles, so going inward is as unavoidable as tomorrow."),
    'singularities/lensing': (
        "A black hole bends the light behind it into a bright ring around a dark shadow.",
        "A glass marble on a table makes the pattern behind it into a ring.",
        "The marble is clear glass. A black hole is the darkest thing there is; the ring comes from light passing close by, not through it."),
    'hyperdimensions/tesseract': (
        "A four dimensional cube can only be seen as its shadow in our three dimensions, which seems to turn inside out as it rotates.",
        "A cube's shadow on a wall: a flat shape that changes oddly as the cube turns.",
        "We can see the cube that casts a shadow. Nobody can see the fourth direction; we only ever see shadows and slices."),
    'strings/compactification': (
        "String theory needs extra dimensions, curled up so small we cannot notice them.",
        "A garden hose seen from far away looks like a line; up close you see it is a tube you could walk round.",
        "A hose is curled in our three dimensions. These extra dimensions would be new directions altogether, and none has been found yet."),
}


def main():
    with open(ENTRIES, encoding='utf-8') as f:
        E = json.load(f)
    missing = [k for k in P if k not in E]
    if missing:
        raise SystemExit('unknown entries: %s' % missing)
    for k, (line, like, breaks) in P.items():
        for s in (line, like, breaks):
            if '—' in s or '–' in s or ' - ' in s:
                raise SystemExit('dash in %s: %s' % (k, s))
        E[k]['plain'] = {'line': line, 'like': like, 'breaks': breaks}
    with open(ENTRIES, 'w', encoding='utf-8') as f:
        json.dump(E, f, ensure_ascii=False, indent=1)
        f.write('\n')
    print('plain words on', len(P), 'entries')


if __name__ == '__main__':
    main()
