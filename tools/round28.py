"""
Round 28.

* Settings on machine screens, the Core's Forge and the Anomaly Engine glide to their new value instead of jumping
  (client/screen/Smooth); the physics controls gain a gauge that can be dragged like a slider.
* The Omnipotence Core lists everything of this mod but will not materialize any of it: "access denied".
* (The creative-item ban was removed again: it belongs to the modpack, as a script, not to the mod.)
"""

LANG = {
    'message.theoreticalsciences.core_access_denied': '[Omnipotence] ACCESS DENIED: %s cannot be made by the Core.',
}


def apply(g):
    g['LANG'].update(LANG)
    try:
        import round29_fx          # the full-screen Forge's text
        g['LANG'].update(round29_fx.LANG)
    except ImportError:
        pass
    try:
        import round29_fix         # the release-check fixes' text
        g['LANG'].update(round29_fix.LANG)
    except ImportError:
        pass


def finish(g):
    pass
