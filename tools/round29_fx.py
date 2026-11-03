"""
Round 29: the Omnipotence Core's terminal fills the window; the Forge gets a search box, a live simulated universe and
a "code display" (client/screen/OmnipotenceScreen, client/screen/forge/*). The code shown in the terminal is
hard-coded English on purpose (it is "the Core's program"); only the search hint is translatable.
"""

LANG = {
    'gui.theoreticalsciences.forge_search': 'Search recipes...',
}


def apply(g):
    g['LANG'].update(LANG)


def finish(g):
    pass
