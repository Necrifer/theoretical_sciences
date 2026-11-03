"""
Round 33: EMI support (src/main/java/com/theoreticalsciences/compat/emi).

The EMI pages reuse the JEI pages' lang keys (tab names, "Catalyst - not consumed", research, coil tier, physics
windows, structure layers...). Only the few lines EMI shows that JEI does not are new.
"""

LANG = {
    # machine pages: energy over the whole run (FE/t x ticks); generators show it with a "+"
    'emi.theoreticalsciences.total_energy': 'Total: %s FE',
    # structure pages: the layer-by-layer view replaces JEI's rotating 3D view
    'emi.theoreticalsciences.layer_help': 'Click the view or the arrows to change layer (right-click goes down)',
    'emi.theoreticalsciences.must_be_air': 'Must be left empty (air)',
}
