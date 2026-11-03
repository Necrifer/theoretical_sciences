"""
Round 12: every multiblock runs on real physics (QMD / NuclearCraft: Neoteric style).

The old tuning dial is gone. Each multiblock has a physics model (data/theoreticalsciences/ts_physics/<machine>.json):
the operator sets real parameters (dipole field, RF voltage, plasma current, throat radius...), the machine computes
what follows from them with real equations, checks the limits that keep it alive (superconductor critical field and
temperature, beta and density limits, Eddington luminosity, Buchdahl and TOV limits, cosmic censorship, error
thresholds...), and every recipe states an operating window it must be run inside. Every multiblock is cooled through
a coolant loop: heat raises the cold-mass temperature, which lowers the superconductors' critical field, and so on.

tools/physics_sim.py mirrors the Java runtime and is used here to prove that every recipe has a working point with the
coils and coolants available when it unlocks.
"""
import glob
import json
import math
import os

import physics_sim as ps

NS = 'theoreticalsciences'
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
RECIPE_DIR = os.path.join(ROOT, 'src/main/resources/data/theoreticalsciences/recipe')

# ------------------------------------------------------------------ coolants
# temperature the cold mass is held at (K), kJ carried per mB (incl. the cryoplant's recirculation), what comes back
COOLANTS = {
    'minecraft:water': dict(temperature=300, capacity=40, returns='', name='Water'),
    f'{NS}:ionic_liquid': dict(temperature=300, capacity=80, returns='', name='Ionic Liquid'),
    f'{NS}:superfluid_helium': dict(temperature=2.0, capacity=6, returns=f'{NS}:warm_helium', name='Superfluid Helium'),
    f'{NS}:liquid_antihelium': dict(temperature=2.0, capacity=8, returns='', name='Liquid Antihelium'),
    f'{NS}:bose_einstein_condensate': dict(temperature=0.01, capacity=0.5, returns='', name='Bose-Einstein Condensate'),
    f'{NS}:neutrino_flux': dict(temperature=10, capacity=20, returns='', name='Neutrino Flux'),
    f'{NS}:metallic_hydrogen': dict(temperature=20, capacity=30, returns='', name='Metallic Hydrogen'),
    f'{NS}:antihydrogen': dict(temperature=20, capacity=30, returns='', name='Antihydrogen'),
    f'{NS}:dark_matter': dict(temperature=3, capacity=1, returns='', name='Dark Matter'),
    f'{NS}:liquid_spacetime': dict(temperature=3, capacity=200, returns='', name='Liquid Spacetime'),
}

MODELS = {}      # machine -> dict (model json + lang)
WINDOWS = {}     # machine -> {recipe: (vars, windows)}


# ------------------------------------------------------------------ little DSL
def P(id, name, unit, lo, hi, step, default, desc, bind=None):
    d = dict(id=id, unit=unit, min=lo, max=hi, step=step, default=default, _name=name, _desc=desc)
    if bind: d['bind'] = bind
    return d


def D(id, name, unit, expr, formula, desc, show=True):
    d = dict(id=id, unit=unit, expr=expr, formula=formula, _name=name, _desc=desc)
    if not show: d['show'] = False
    return d


def K(id, expr):
    return dict(id=id, expr=str(expr))


def S(id, name, unit, init, ddt, lo, hi, oor, desc):
    return dict(id=id, unit=unit, init=init, ddt=ddt, min=lo, max=hi, out_of_range=oor, _name=name, _desc=desc)


def C(id, kind, expr, msg, formula=''):
    return dict(id=id, kind=kind, expr=expr, formula=formula, _name=msg)


def V(id, name, default):
    return (id, name, default)


def model(mid, title, intro, disturb, params, derived, constraints, rate, rate_formula, rate_desc, heat,
          consts=(), states=(), rvars=(), idle_heat='0', quench_energy='0', kappa=40, heat_capacity=40, timescale=1, rate_max=4):
    MODELS[mid] = dict(title=title, intro=intro, disturb=disturb, params=list(params), consts=list(consts), states=list(states),
                       derived=list(derived), constraints=list(constraints), rate=rate, rate_formula=rate_formula, rate_desc=rate_desc,
                       heat=heat, idle_heat=idle_heat, quench_energy=quench_energy, kappa=kappa, heat_capacity=heat_capacity,
                       timescale=timescale, rate_max=rate_max, rvars=list(rvars))


def magnet(field_expr, what='magnets'):
    """Superconducting magnet limits: the cryogenic interlock and the quench (critical field falls as T rises)."""
    d = [D('Bc', 'Critical field', 'T', 'sc_B0*pos(1-(T/sc_Tc)^2)', 'B_c(T) = B_c2(0)·(1 − (T/T_c)²)',
           'The highest field the coil conductor can carry while superconducting, at the present cold-mass temperature. Hotter means lower.')]
    c = [C('cryo', 'stop', '0.95*sc_Tc - T', f'Interlock: {what} not superconducting (too warm)', 'T < 0.95·T_c'),
         C('quench', 'quench', f'Bc - ({field_expr})', f'Field above the critical field: the {what} quench', 'B < B_c(T)')]
    return d, c


def overheat(tmax, what='structure'):
    return C('overheat', 'damage', f'{tmax} - T', f'The {what} is overheating', f'T < {tmax} K')


# =================================================================================== 1-3 accelerators
def ring(mid, rho, title, intro, species):
    md, mc = magnet('max(Bf, Bpole)', 'dipoles and quadrupoles')
    if species == 'ion':
        rv = [V('A', 'Ion mass number A', 1), V('Z', 'Ion charge Z', 1)]
        mass, charge = 'A*0.9315', 'Z'
        extra = [D('EN', 'Energy per nucleon', 'GeV', 'E/A', 'E_N = E/A', 'Beam energy per nucleon.'),
                 D('sNN', 'Collision energy √s_NN', 'GeV', '2*EN', '√s_NN = 2·E_N',
                   'Centre-of-mass energy per colliding nucleon pair, head-on. This is what decides what the collision can make.'),
                 D('eBj', 'Bjorken energy density', 'GeV/fm³', '1.3*ln(pos(sNN/2)+1e-9)', 'ε_Bj ≈ 1.3·ln(√s_NN / 2 GeV)',
                   'Energy density reached in the overlap region (Bjorken estimate). Above ~1 GeV/fm³ hadrons melt into quark-gluon plasma.')]
    else:
        rv = [V('m', 'Particle mass m', 0.938), V('q', 'Particle charge q', 1)]
        mass, charge = 'm', 'q'
        extra = []
    params = [P('B', 'Dipole field B', 'T', 0, 60, 0.01, 1, 'Field of the bending dipoles. Sets the momentum the ring can hold: p = 0.2998·q·B·ρ.'),
              P('V', 'RF voltage V', 'MV', 0, 1000, 1, 10, 'Accelerating voltage per turn. Must at least replace what the beam radiates away each turn.'),
              P('G', 'Quadrupole gradient G', 'T/m', 0, 400, 0.5, 20, 'Focusing strength of the FODO quadrupoles. Too weak or too strong and the betatron motion is unstable.'),
              P('I', 'Beam current I', 'mA', 0, 500, 1, 10, 'Circulating current. More current, more collisions - and more space charge and radiated power.')]
    consts = [K('rho', rho), K('Lc', 20), K('Lq', 1.0), K('ap', 0.03), K('en', 2e-6), K('mp', mass), K('qe', charge)]
    derived = [D('Bf', 'Effective dipole field', 'T', 'B*(1+0.004*d)', 'B_eff = B·(1 + δ_field)', 'The field the beam actually sees: the set field plus the drifting field error.'),
               D('p', 'Beam momentum p', 'GeV/c', '0.2998*qe*Bf*rho', 'p = 0.2998·q·B·ρ', 'Momentum held on the design orbit of bending radius ρ.'),
               D('E', 'Total energy E', 'GeV', 'sqrt(p^2+mp^2)', 'E = √(p² + m²)', 'Total relativistic energy of one particle.'),
               D('Ek', 'Kinetic energy', 'GeV', 'E - mp', 'E_k = E − m', 'Kinetic energy of one particle.'),
               D('gam', 'Lorentz factor γ', '', 'E/mp', 'γ = E/m', 'How relativistic the beam is.'),
               D('bet', 'Velocity β', 'c', 'p/E', 'β = p/E', 'Speed as a fraction of light.'),
               D('U0', 'Synchrotron loss / turn', 'GeV', '6.034e-18*qe^2*gam^4*bet^3/rho', 'U₀ = 6.03×10⁻¹⁸·q²·γ⁴·β³/ρ',
                 'Energy radiated per turn by a particle bent on radius ρ. Grows as γ⁴: light particles radiate enormously.'),
               D('Psr', 'Synchrotron power', 'MW', 'U0*I', 'P = U₀·I', 'Power the whole beam radiates.'),
               D('kq', 'Quad strength k', '1/m²', '0.2998*qe*G/p', 'k = 0.2998·q·G/p', 'Normalised focusing strength of the quadrupoles.'),
               D('x', 'sin(μ/2)', '', 'Lc*kq*Lq/4', 'sin(μ/2) = L·k·l_q/4', 'Half the FODO phase advance. Stable only between 0 and 1; 0.707 (μ = 90°) is ideal.', show=False),
               D('mu', 'Phase advance μ', '°', '114.592*atan(x/sqrt(pos(1-x^2)))', 'μ = 2·asin(L·k·l_q/4)', 'Betatron phase advance per FODO cell. 90° gives the smallest beam.'),
               D('bmax', 'Peak beta function', 'm', 'Lc*(1+x)/(2*x*sqrt(pos(1-x^2)))', 'β_max = L(1+sin(μ/2))/sin μ', 'Largest beta function in the lattice: the beam is widest here.'),
               D('sig', 'Beam size σ', 'mm', '1000*sqrt(bmax*en/(gam*bet))', 'σ = √(β_max·ε_n/(βγ))', 'RMS beam radius at the widest point (normalised emittance 2 µm).'),
               D('Bpole', 'Quad pole-tip field', 'T', 'G*ap', 'B_pole = G·a', 'Field at the quadrupole pole tip (aperture 3 cm).'),
               D('dQ', 'Space-charge tune shift', '', '0.004*I*qe^2/(mp*bet^2*gam^3)', 'ΔQ ∝ I·q²/(m·β²·γ³)',
                 'How much the beam\'s own charge defocuses it. Worst at low energy; above 0.25 the beam blows up.')] + extra + md
    cons = [C('rf', 'stop', 'qe*V/1000 - U0', 'RF cannot replace the synchrotron loss: beam lost', 'q·V ≥ U₀'),
            C('focus_lo', 'stop', 'x - 0.02', 'Quadrupoles too weak: the beam is not focused', 'sin(μ/2) > 0'),
            C('focus_hi', 'stop', '0.98 - x', 'Quadrupoles too strong: betatron motion unstable', 'sin(μ/2) < 1'),
            C('space_charge', 'stop', '0.25 - dQ', 'Space-charge tune shift too large: the beam blows up', 'ΔQ < 0.25'),
            C('aperture', 'wear', 'ap*1000 - 6*sig', 'Beam halo scraping the vacuum chamber', '6σ < 30 mm')] + mc
    model(mid, title, intro, 'Dipole field error (power-supply ripple, magnet hysteresis): the beam sees B·(1 ± 0.4%·d).',
          params, derived, cons, rate='I/100*2*x*sqrt(pos(1-x^2))', rate_formula='rate = I/100 mA · sin μ',
          rate_desc='Collision rate: the beam current, times how well the lattice focuses (best at μ = 90°).',
          heat='4 + 10*Psr + 0.0004*I*Ek', idle_heat='3', quench_energy='60*B^2', consts=consts, rvars=rv, kappa=40, heat_capacity=40)


ring('synchrotron', 120, 'Synchrotron', 'A separated-function storage ring of bending radius 120 m: dipoles bend, quadrupoles focus, RF cavities accelerate. Its energy is set by the field you can hold in the dipoles - and that by how cold they are.', 'particle')
ring('quark_gluon_collider', 600, 'Quark-Gluon Collider', 'Two counter-rotating rings of bending radius 600 m cross at the detector. Heavy ions make quark-gluon plasma; protons make whatever √s allows.', 'ion')

WINDOWS['synchrotron'] = {
    'synchrotron_strange_matter': ({'m': 0.938, 'q': 1}, [('Ek', 20, 40)]),
    'pentaquark_shard': ({'m': 0.938, 'q': 1}, [('Ek', 80, 120)]),
    'glueball_core': ({'m': 0.000511, 'q': 1}, [('Ek', 4, 6)]),
    'bottom_top_quarks': ({'m': 0.938, 'q': 1}, [('Ek', 500, 700)]),
    'hyperon_crystal': ({'m': 0.938, 'q': 1}, [('Ek', 1500, 2500)]),
    'higgs_boson_crystal': ({'m': 0.10566, 'q': 1}, [('Ek', 62.0, 63.0)]),
}
WINDOWS['quark_gluon_collider'] = {
    'quark_gluon_plasma': ({'A': 208, 'Z': 82}, [('sNN', 150, 600)]),
    'strange_matter': ({'A': 197, 'Z': 79}, [('sNN', 6, 12)]),
    'magnetic_monopole': ({'A': 1, 'Z': 1}, [('sNN', 2000, 3500)]),
    'superpartner_shard': ({'A': 1, 'Z': 1}, [('sNN', 3600, 4200)]),
}

# ---- nuclide forge: heavy-ion linac on a fixed target (fusion-evaporation)
_md, _mc = magnet('Bs', 'focusing solenoids')
model('nuclide_forge', 'Nuclide Forge',
      'A superconducting heavy-ion linac (40 niobium cavities) fires a projectile into an actinide target. Fuse just above the Coulomb barrier with the compound nucleus cool enough to survive: one atom at a time, the island of stability.',
      'Cavity field drift (microphonics, Lorentz detuning): every cavity gives V·(1 ± 1%·d).',
      [P('V', 'Cavity voltage V', 'MV', 0, 12, 0.01, 1, 'Accelerating voltage of each of the 40 cavities.'),
       P('phi', 'Synchronous phase φ', '°', -90, 0, 1, -30, 'RF phase the ions ride on. Closer to 0° gives more energy; negative phases are needed for longitudinal (phase) focusing.'),
       P('qi', 'Ion charge state', 'e', 1, 30, 1, 10, 'Charge state after the stripper foil. Energy gain is q·V per cavity.'),
       P('I', 'Beam intensity', 'pµA', 0, 10, 0.1, 1, 'Particle current on target. More ions, more fusions - and more target heating.'),
       P('Bs', 'Solenoid field', 'T', 0, 20, 0.05, 2, 'Field of the focusing solenoids between cavities.')],
      [D('Vf', 'Effective cavity voltage', 'MV', 'V*(1+0.01*d)', 'V_eff = V·(1 + δ)', 'Voltage actually delivered, with the drifting detuning.'),
       D('Elab', 'Beam energy (lab)', 'MeV', 'ncav*Vf*qi*cos(phi*0.0174533)', 'E_lab = N·q·V·cos φ', 'Kinetic energy of the ions leaving the linac.'),
       D('Ecm', 'Centre-of-mass energy', 'MeV', 'Elab*A2/(A1+A2)', 'E_cm = E_lab·A_t/(A_p + A_t)', 'Energy available for the collision.'),
       D('VC', 'Coulomb barrier', 'MeV', '1.44*Z1*Z2/(1.16*(cbrt(A1)+cbrt(A2))+2.2)', 'V_C = 1.44·Z₁Z₂ / R  (MeV·fm)', 'Electrostatic repulsion the nuclei must overcome to touch.'),
       D('Estar', 'Excitation energy E*', 'MeV', 'Ecm + Q', 'E* = E_cm + Q', 'How hot the compound nucleus is born. Too cold and it never formed; too hot and it fissions before it cools.'),
       D('xs', 'Relative cross-section', '', 'exp(-((Estar-Eopt)/5)^2)*step(Ecm-VC)', 'σ/σ_max = e^−((E*−E*_opt)/5)²', 'How likely a fusion-evaporation event is, relative to its best.'),
       D('tr', 'Linac transmission', '', 'clamp(3*sin(-phi*0.0174533), 0, 1)', 'T ≈ 3·sin|φ|', 'Fraction of the beam inside the RF bucket (phase acceptance).'),
       D('brho', 'Magnetic rigidity', 'T·m', 'sqrt(Elab*(Elab+2*A1*931.5))/(299.8*qi)', 'Bρ = p/(q)', 'How hard the ions are to bend or focus.'),
       D('fsol', 'Solenoid focal length', 'm', '4*brho^2/(Bs^2*0.5)', 'f = 4(Bρ)²/(B²·l)', 'Focal length of each 0.5 m solenoid.'),
       D('Pcav', 'Cavity RF loss', 'kW', 'ncav*Vf^2*(140*exp(-17.67/T)/T + 0.002)', 'P ∝ V²·R_s,  R_BCS ∝ e^(−Δ/kT)/T',
         'Heat dissipated in the niobium cavity walls. BCS surface resistance falls exponentially with temperature: 2 K costs a fraction of 4 K.'),
       D('Pb', 'Beam power on target', 'kW', 'I*Elab/1000', 'P = I·E', 'Beam power deposited in the target.')] + _md,
      [C('nb_cryo', 'stop', '8.7 - T', 'Interlock: niobium cavities not superconducting', 'T < 8.7 K'),
       C('nb_quench', 'quench', '9.2 - T', 'Cavity quench: the niobium went normal', 'T < 9.2 K'),
       C('phase', 'stop', '-phi - 2', 'Phase at the crest: no phase focusing, the bunch spreads out', 'φ < 0'),
       C('barrier', 'stop', 'Ecm - VC', 'Below the Coulomb barrier: nothing fuses', 'E_cm ≥ V_C'),
       C('focus', 'stop', '10 - fsol', 'Solenoids too weak to focus the beam', 'f ≤ 10 m')] + _mc,
      rate='I*tr*xs/2', rate_formula='rate = I·T·σ/σ_max / 2', rate_desc='Fusions per second: beam intensity × transmission × cross-section.',
      heat='Pcav + 0.05*Pb', idle_heat='2', quench_energy='30*Vf^2*ncav/4',
      consts=[K('ncav', 40)],
      rvars=[V('A1', 'Projectile A', 48), V('Z1', 'Projectile Z', 20), V('A2', 'Target A', 244), V('Z2', 'Target Z', 94),
             V('Q', 'Fusion Q-value', -163), V('Eopt', 'Optimal E*', 38)])
WINDOWS['nuclide_forge'] = {
    'flerovium_ingot': ({'A1': 48, 'Z1': 20, 'A2': 244, 'Z2': 94, 'Q': -163, 'Eopt': 38}, [('Estar', 32, 44)]),
    'unbihexium_ingot': ({'A1': 70, 'Z1': 30, 'A2': 248, 'Z2': 96, 'Q': -265, 'Eopt': 30}, [('Estar', 25, 35)]),
}


# =================================================================================== 4-5 tokamaks
FUELS = {'DT': dict(EG=1180, Cf=3.68e-18, Ef=17.6, fch=0.2, Zf=1.0),
         'DHe3': dict(EG=4700, Cf=7.4e-19, Ef=18.3, fch=1.0, Zf=1.5),
         'pB11': dict(EG=22460, Cf=3.2e-17, Ef=8.7, fch=1.0, Zf=3.0)}


def tokamak(mid, title, intro, R0, a, pa_max, bt_max, pref):
    md, mc = magnet('Bpk', 'toroidal field coils')
    model(mid, title, intro, 'Impurity influx from the wall: Z_eff rises by up to 30% (d = −1 clean ... +1 dirty), and bremsstrahlung with it.',
          [P('Bt', 'Toroidal field Bt', 'T', 0, bt_max, 0.05, 3, 'Field on the plasma axis. Raises the β limit and the safety factor.'),
           P('Ip', 'Plasma current Ip', 'MA', 0, 30, 0.1, 2, 'Current driven through the plasma. Improves confinement (τ_E ∝ I^0.93) and the density limit, but lowers the safety factor.'),
           P('n', 'Density n', '10²⁰/m³', 0, 5, 0.01, 0.5, 'Ion density. Fusion power grows as n², but so does bremsstrahlung, and there is the Greenwald limit.'),
           P('Pa', 'Auxiliary heating', 'MW', 0, pa_max, 1, 20, 'Neutral beams and RF heating power put into the plasma.')],
          [D('Zeff', 'Effective charge', '', 'Zf*(1+0.15*(d+1))', 'Z_eff', 'Average ion charge including impurities; drives radiation losses.'),
           D('sv', 'Reactivity ⟨σv⟩', 'm³/s', 'Cf*Tp^(-2/3)*exp(-3*(EG/(4*Tp))^(1/3))', '⟨σv⟩ = C·T^(−2/3)·exp(−3(E_G/4T)^(1/3))',
             'Fusion reactivity of the fuel at the plasma temperature (Gamow form; E_G is the Gamow energy).'),
           D('Pfus', 'Fusion power', 'MW', 'n^2*sv*Ef*Vp*4.005e20', 'P_fus = ¼n²⟨σv⟩E_f·V', 'Total fusion power of the plasma.'),
           D('Pch', 'Charged-particle heating', 'MW', 'fch*Pfus', 'P_α = f_ch·P_fus', 'Part of the fusion power that stays in the plasma and heats it.'),
           D('Pbr', 'Bremsstrahlung', 'MW', '5.35e-3*Zeff*n^2*sqrt(Tp)*Vp', 'P_br = 5.35×10⁻³·Z_eff·n²·√T·V', 'Radiation from electrons braking on ions. Grows with Z_eff.'),
           D('Ploss', 'Loss power', 'MW', 'max(Pa + Pch - Pbr, 0.01)', 'P_L = P_aux + P_α − P_rad', 'Power crossing the plasma edge (sets the confinement scaling).', show=False),
           D('tauE', 'Confinement time τ_E', 's', '0.0562*Ip^0.93*Bt^0.15*(10*n)^0.41*Ploss^(-0.69)*R0^1.97*1.7^0.78*(a/R0)^0.58*1.19',
             'τ_E = 0.0562·I^0.93·B^0.15·n^0.41·P^−0.69·R^1.97·κ^0.78·ε^0.58·M^0.19', 'Energy confinement time, IPB98(y,2) scaling law.'),
           D('W', 'Stored energy', 'MJ', '0.048*n*Tp*Vp', 'W = 3nTV', 'Thermal energy in the plasma.'),
           D('trip', 'Triple product nTτ', '10²⁰ keV s', 'n*Tp*tauE', 'n·T·τ_E', 'Lawson\'s figure of merit. Deuterium-tritium ignites near 30.'),
           D('Qf', 'Gain Q', '', 'Pfus/max(Pa, 0.1)', 'Q = P_fus/P_aux', 'Fusion power out per heating power in.'),
           D('beta', 'Plasma β', '%', '8.05*n*Tp/Bt^2', 'β = 2μ₀p/B²', 'Plasma pressure over magnetic pressure.'),
           D('blim', 'Troyon β limit', '%', '2.8*Ip/(a*Bt)', 'β_max = β_N·I/(aB)', 'Above this the plasma goes unstable and disrupts.'),
           D('nG', 'Greenwald density', '10²⁰/m³', 'Ip/(pi*a^2)', 'n_G = I/(πa²)', 'Above this density the edge radiates and the plasma disrupts.'),
           D('q95', 'Safety factor q₉₅', '', '5*a^2*Bt*(1+1.7^2)/2/(R0*Ip)', 'q = 5a²B(1+κ²)/(2RI)', 'Field-line winding at the edge. Below 2 the kink mode tears the plasma apart.'),
           D('Bpk', 'Peak coil field', 'T', 'Bt*R0/(R0-a-1)', 'B_peak = B·R/(R − a − 1 m)', 'Field on the inner leg of the toroidal coils, where it is highest.')] + md,
          [C('beta_limit', 'quench', 'blim - beta', 'β above the Troyon limit: the plasma disrupts', 'β < β_N·I/(aB)'),
           C('greenwald', 'quench', 'nG - n', 'Above the Greenwald density: radiative collapse, disruption', 'n < I/(πa²)'),
           C('kink', 'quench', 'q95 - 2', 'Safety factor below 2: external kink, disruption', 'q₉₅ > 2')] + mc,
          rate=f'(Pch + Pa)/{pref}', rate_formula=f'rate = (P_α + P_aux) / {pref} MW', rate_desc='Smelting rate follows the power heating the plasma: fusion heating plus your own.',
          heat='10 + 0.01*Pfus + 0.02*Pa', idle_heat='4', quench_energy='20*W',
          consts=[K('R0', R0), K('a', a), K('Vp', f'2*pi^2*R0*a^2*1.7')],
          states=[S('Tp', 'Plasma temperature', 'keV', '0.1', '(Pa + Pch - Pbr - W/tauE)/(0.048*n*Vp+0.01)', 0.01, 3000, 'stop',
                    'Ion temperature. Rises while heating beats losses: dT/dt = (P_aux + P_α − P_br − W/τ_E)/(3nV).')],
          rvars=[V('EG', 'Gamow energy E_G (keV)', 1180), V('Cf', 'Reactivity constant', 3.68e-18), V('Ef', 'Energy per reaction (MeV)', 17.6),
                 V('fch', 'Charged fraction', 0.2), V('Zf', 'Fuel Z_eff', 1.0)],
          kappa=40, heat_capacity=60)


tokamak('stellar_plasma_crucible', 'Stellar Plasma Crucible', 'A tokamak (R = 6.2 m, a = 2 m): a ring of plasma confined by toroidal and poloidal fields, heated until its fuel fuses. The metals are smelted in the burning plasma.', 6.2, 2.0, 2000, 16, 400)
tokamak('transcendent_plasma_forge', 'Transcendent Plasma Forge', 'A far larger tokamak (R = 14 m, a = 4.5 m) able to burn the aneutronic fuels - deuterium-helium-3 and proton-boron - at hundreds of keV.', 14.0, 4.5, 20000, 60, 5000)


def fuel(name, lo, hi):
    return (dict(FUELS[name]), [('Tp', lo, hi)])


def _fuelvars(name):
    f = FUELS[name]
    return {'EG': f['EG'], 'Cf': f['Cf'], 'Ef': f['Ef'], 'fch': f['fch'], 'Zf': f['Zf']}


WINDOWS['stellar_plasma_crucible'] = {
    'chronosteel': (_fuelvars('DT'), [('Tp', 8, 20)]),
    'exotic_solder': (_fuelvars('DT'), [('Tp', 5, 12)]),
    'stellar_plasma_from_core': (_fuelvars('DT'), [('Tp', 15, 30)]),
    'spacetime_alloy': (_fuelvars('DT'), [('Tp', 25, 40)]),
    'cosmic_neutronium': (_fuelvars('DT'), [('Tp', 40, 60)]),
}
WINDOWS['transcendent_plasma_forge'] = {
    'residual_starlight': (_fuelvars('DT'), [('Tp', 15, 30)]),
    'forge_cosmic_neutronium': (_fuelvars('DHe3'), [('Tp', 60, 100)]),
    'hypernova_alloy_ingot': (_fuelvars('DHe3'), [('Tp', 100, 160)]),
    'hypernova_plasma': (_fuelvars('pB11'), [('Tp', 150, 300)]),
    'transcendent_metal_ingot': (_fuelvars('pB11'), [('Tp', 300, 600)]),
}

# =================================================================================== 6-9 black holes
model('kugelblitz_chamber', 'Kugelblitz Chamber',
      'Focus enough light into a small enough spot and it collapses into a black hole (the hoop conjecture). Then the hard part: a micro black hole evaporates faster the smaller it is, so it must be fed exactly as fast as it radiates - an unstable balance.',
      'Feed-coupling efficiency drifts: only ṁ·(1 + 5%·d) actually falls in.',
      [P('lgE', 'Pulse energy', 'log₁₀ J', 20, 30, 0.01, 25.6, 'Total energy of the converging laser pulse, as a power of ten. E/c² is the mass of the black hole it forms.'),
       P('Eg', 'Photon energy', 'GeV', 1, 10000, 1, 100, 'Energy per photon. Shorter wavelength = smaller focal spot (λ = 1.24×10⁻¹⁵ m·GeV / E).'),
       P('NA', 'Numerical aperture', '', 0.05, 0.95, 0.01, 0.5, 'How steeply the beams converge. The diffraction-limited spot is λ/(2·NA).'),
       P('mdot', 'Feed rate ṁ', 'g/s', 0, 1000000, 0.001, 10, 'Mass fed into the hole. At equilibrium it equals the Hawking mass loss.')],
      [D('M0d', 'Pulse mass E/c²', 'kg', 'M0', 'M = E/c²', 'Mass-energy of the pulse: the mass of the hole it forms.'),
       D('rs0', 'Schwarzschild radius (pulse)', 'm', '1.485e-27*M0', 'r_s = 2GM/c²', 'The pulse must be focused inside this radius to collapse.'),
       D('spot', 'Focal spot radius', 'm', '1.24e-15/(Eg*2*NA)', 'r = λ/(2·NA)', 'Diffraction-limited focal spot.'),
       D('lgM', 'Black hole mass', 'log₁₀ kg', 'log10(M)', 'log₁₀ M', 'Mass of the captive black hole, as a power of ten.'),
       D('TH', 'Hawking temperature', 'K', '1.227e23/M', 'T_H = ħc³/(8πGMk)', 'Temperature of the hole\'s thermal radiation.'),
       D('PH', 'Hawking power', 'W', '3.563e32/M^2', 'P = ħc⁶/(15360πG²M²)', 'Power radiated. Grows as 1/M²: smaller holes burn brighter.'),
       D('mdeq', 'Equilibrium feed', 'g/s', '1000*PH/c2', 'ṁ_eq = P/c²', 'Feed rate that exactly balances evaporation at the present mass. The balance is unstable: below it the hole shrinks ever faster.'),
       D('life', 'Evaporation time (unfed)', 's', '8.41e-17*M^3', 't = 5120πG²M³/(ħc⁴)', 'How long the hole would last with nothing fed in.')],
      [C('hoop', 'stop', 'rs0 - spot', 'Hoop conjecture not met: the pulse is not focused inside its own Schwarzschild radius', 'r_spot < r_s')],
      rate='1', rate_formula='rate = 1 (while the hole is held in the window)', rate_desc='The chamber works at a fixed pace once the hole is in range.',
      heat='20 + 1e-14*PH', idle_heat='4', quench_energy='800',
      consts=[K('c2', 8.988e16), K('M0', '10^lgE/c2')],
      states=[S('M', 'Black hole mass M', 'kg', 'M0', 'mdot/1000*(1+0.05*d) - PH/c2', 1e6, 1e13, 'quench',
                'Mass of the captive hole: dM/dt = ṁ − P_H/c². Below 10⁶ kg it dies in a final burst; above 10¹³ kg it outgrows the chamber.')],
      timescale=3e8, kappa=40)
WINDOWS['kugelblitz_chamber'] = {
    'micro_singularity': ({}, [('lgM', 8.7, 9.0)]),
    'micro_singularity_from_seed': ({}, [('lgM', 8.7, 9.0)]),
    'kugelblitz_seed': ({}, [('lgM', 8.6, 8.9)]),
    'gravitational_wave_crystal': ({}, [('lgM', 8.8, 9.1)]),
}

_md, _mc = magnet('Bh', 'horizon-threading coils')
model('penrose_ergosphere', 'Penrose Ergosphere',
      'Drop matter into a spinning black hole\'s ergosphere, split it so half falls in on a negative-energy orbit, and the other half escapes with more energy than you sent. The extra comes out of the hole\'s spin. Feed it angular momentum to keep it spinning.',
      'Accretion torque noise: the spin-up torque varies by ±20%·d.',
      [P('rinj', 'Splitting radius r/M', 'M', 1.0, 2.5, 0.005, 1.6, 'Where the infalling matter is split, in units of the hole\'s mass (G = c = 1). Must be inside the ergosphere (r < 2M) and outside the horizon.'),
       P('mdot', 'Injection rate', 'g/s', 0, 1000, 1, 100, 'Matter sent into the ergosphere.'),
       P('tau', 'Spin-up torque', '', 0, 1, 0.01, 0.3, 'Prograde matter fed at the inner orbit to spin the hole back up.'),
       P('Bh', 'Horizon field', 'T', 0, 20, 0.05, 1, 'Field threading the horizon: also extracts spin as Blandford-Znajek power.')],
      [D('rp', 'Horizon radius r₊', 'M', '1 + sqrt(pos(1 - a^2))', 'r₊ = M + √(M² − a²)', 'Event horizon radius of a Kerr hole of spin a.'),
       D('etamax', 'Maximum efficiency', '', '0.5*(sqrt(2/rp) - 1)', 'η_max = ½(√(2M/r₊) − 1)', 'Best possible Penrose efficiency for this spin; 20.7% for an extremal hole.'),
       D('eta', 'Efficiency η', '', 'etamax*clamp((2 - rinj)/(2 - rp + 1e-9), 0, 1)', 'η ≈ η_max·(2M − r)/(2M − r₊)', 'Extracted energy per unit rest mass: best deep in the ergosphere.'),
       D('Pp', 'Penrose power', 'TW', 'eta*mdot*1e-3*8.988e16/1e12', 'P = η·ṁc²', 'Power extracted by the Penrose process.'),
       D('Pbz', 'Blandford-Znajek power', 'TW', '50*a^2*Bh^2', 'P_BZ ∝ a²B²', 'Power extracted magnetically from the spin.')] + _md,
      [C('horizon', 'stop', 'rinj - rp - 0.01', 'Splitting point inside the horizon: nothing comes back', 'r > r₊'),
       C('ergo', 'stop', '2 - rinj', 'Outside the ergosphere: no negative-energy orbits exist', 'r < 2M')] + _mc,
      rate='(Pp + Pbz)/500', rate_formula='output = (P_Penrose + P_BZ) / 500 TW', rate_desc='Generator output scales with the extracted power.',
      heat='8 + 0.005*(Pp+Pbz)', idle_heat='3', quench_energy='40*Bh^2',
      states=[S('a', 'Spin a/M', '', '0.9', '0.003*tau*(1+0.2*d)*(1 - a) - 2e-3*eta*mdot/100 - 1e-4*a*Bh^2', -0.5, 0.998, 'damage',
                'Dimensionless spin. Extraction spins it down; the torque spins it up. At 0.998 (Thorne\'s limit) the horizon is close to tearing.')],
      kappa=40)
WINDOWS['penrose_ergosphere'] = {'kugelblitz_seed': ({}, []), 'micro_singularity': ({}, [])}

_md, _mc = magnet('Bcf', 'confinement coils')
model('white_hole_emitter', 'White Hole Emitter',
      'A white hole only emits. But it is unstable (Eardley): any matter falling toward it piles up at its horizon until it collapses into a black hole. Keep the chamber vacuum hard and the outflow strong enough to sweep it away.',
      'Chamber outgassing: the residual gas pressure fluctuates by a factor 10^(0.3·d).',
      [P('lgM', 'White hole mass', 'log₁₀ kg', 14, 22, 0.01, 17, 'Mass of the white hole, as a power of ten. Sets its Eddington luminosity.'),
       P('mdot', 'Outflow rate', 'kg/s', 0, 100, 0.01, 1, 'Mass expelled per second. Luminosity = 0.1·ṁc².'),
       P('lgP', 'Chamber vacuum', 'log₁₀ Pa', -14, 0, 0.1, -8, 'Residual gas pressure. Gas falling in is what makes white holes collapse.'),
       P('Bcf', 'Confinement field', 'T', 0, 30, 0.05, 2, 'Magnetic field holding the radiation pressure off the walls.')],
      [D('L', 'Luminosity', 'W', '0.1*mdot*8.988e16', 'L = 0.1·ṁc²', 'Power emitted by the outflow.'),
       D('lgL', 'Luminosity', 'log₁₀ W', 'log10(L+1)', 'log₁₀ L', 'Luminosity as a power of ten.'),
       D('LEdd', 'Eddington luminosity', 'W', '6.32*10^lgM', 'L_Edd = 4πGMm_p c/σ_T', 'Luminosity at which radiation pressure balances gravity.'),
       D('edd', 'Eddington ratio', '', 'L/LEdd', 'L/L_Edd', 'Above 1 the outflow is super-Eddington and batters the chamber.'),
       D('inflow', 'Infall rate', 'kg/s', '10^(lgP + 8 + 0.3*d)*(10^(lgM-17))^2', 'ṁ_in ∝ P·M²', 'Gas captured from the chamber vacuum.'),
       D('prad', 'Radiation pressure', 'Pa', 'L/(4*pi*9*2.998e8)', 'p = L/(4πr²c)', 'Radiation pressure on the 3 m chamber wall.'),
       D('pmag', 'Magnetic pressure', 'Pa', 'Bcf^2/(2*1.2566e-6)', 'p = B²/2μ₀', 'Pressure the confinement field can hold back.')] + _md,
      [C('eddington', 'wear', '1 - edd', 'Super-Eddington outflow batters the containment', 'L < L_Edd'),
       C('blowout', 'quench', 'pmag - prad', 'Radiation pressure exceeds the magnetic pressure: blow-out', 'B²/2μ₀ > L/4πr²c')] + _mc,
      rate='clamp(mdot, 0, 4)', rate_formula='rate = ṁ / (1 kg/s)', rate_desc='Ejecta production follows the outflow.',
      heat='10 + 2e-16*L', idle_heat='3', quench_energy='500',
      states=[S('xacc', 'Accreted fraction', '', '0', '(inflow - 0.5*mdot*min(1, 50*xacc))/(1e-6*10^lgM)*1e6', -1, 1, 'quench',
                'Matter piled up at the horizon, as a fraction of the Eardley threshold. At 1 the white hole collapses into a black hole.')],
      kappa=40)
WINDOWS['white_hole_emitter'] = {
    'ejecta_white_hole': ({}, [('lgL', 15.0, 16.0)]),
    'white_hole_fragment': ({}, [('lgL', 16.0, 17.0)]),
    'cosmic_string_segment': ({}, [('lgL', 16.5, 17.5)]),
    'stellar_core': ({}, [('lgL', 17.5, 18.5)]),
}

_md, _mc = magnet('Bf', 'charge-confinement coils')
model('eye_of_the_horizon', 'Eye of the Horizon',
      'A simulated Kerr-Newman black hole: mass, spin and charge, nothing else (the no-hair theorem). Its horizons, temperature and innermost stable orbit follow exactly. Cosmic censorship forbids a²+Q² > M².',
      'Frame-dragging noise: the effective spin is a + 0.002·d.',
      [P('aS', 'Spin a/M', '', 0, 1.2, 0.001, 0.5, 'Angular momentum per unit mass (G = c = 1).'),
       P('Qc', 'Charge Q/M', '', 0, 1.2, 0.001, 0.1, 'Electric charge per unit mass.'),
       P('Bf', 'Confinement field', 'T', 0, 600, 0.5, 50, 'Holds the charge in place (needs 100 T per unit Q/M).'),
       P('lgM', 'Mass', 'log₁₀ M☉', -6, 10, 0.01, 1, 'Mass of the simulated hole in solar masses, as a power of ten.')],
      [D('ae', 'Effective spin', '', 'aS + 0.002*d', 'a', 'Spin including frame-dragging noise.', show=False),
       D('disc', 'Censorship margin', '', '1 - ae^2 - Qc^2', '1 − a² − Q²', 'Must stay positive: otherwise there is no horizon and the singularity is naked.'),
       D('rp', 'Outer horizon r₊', 'M', '1 + sqrt(pos(disc))', 'r₊ = M + √(M² − a² − Q²)', 'Event horizon.'),
       D('rm', 'Inner horizon r₋', 'M', '1 - sqrt(pos(disc))', 'r₋ = M − √(M² − a² − Q²)', 'Cauchy horizon.'),
       D('ks', 'Surface gravity κ', '1/M', '(rp - rm)/(2*(rp^2 + ae^2))', 'κ = (r₊ − r₋)/(2(r₊² + a²))', 'Surface gravity; zero for an extremal hole.'),
       D('TH', 'Hawking temperature', 'K', '6.17e-8*4*ks/10^lgM', 'T = ħκ/(2πk)', 'Temperature of the horizon.'),
       D('OmH', 'Horizon angular velocity', '1/M', 'ae/(rp^2 + ae^2)', 'Ω_H = a/(r₊² + a²)', 'How fast the horizon rotates.'),
       D('Z1', 'Z₁', '', '1 + cbrt(pos(1-ae^2))*(cbrt(1+ae) + cbrt(pos(1-ae)))', 'Bardeen Z₁', 'Auxiliary for the ISCO.', show=False),
       D('Z2', 'Z₂', '', 'sqrt(3*ae^2 + Z1^2)', 'Bardeen Z₂', 'Auxiliary for the ISCO.', show=False),
       D('risco', 'Innermost stable orbit', 'M', '3 + Z2 - sqrt(pos((3 - Z1)*(3 + Z1 + 2*Z2)))', 'r_ISCO = 3 + Z₂ − √((3 − Z₁)(3 + Z₁ + 2Z₂))', 'Prograde ISCO (Bardeen, Press & Teukolsky).'),
       D('eisco', 'Accretion efficiency', '', '1 - sqrt(pos(1 - 2/(3*risco)))', 'η = 1 − √(1 − 2/(3r_ISCO))', 'Fraction of rest mass radiated by matter spiralling down to the ISCO: 5.7% (a = 0) to 42% (a = 1).')] + _md,
      [C('censor', 'quench', 'disc', 'a² + Q² > M²: naked singularity - cosmic censorship violated', 'a² + Q² ≤ M²'),
       C('charge_hold', 'stop', 'Bf - 100*Qc', 'Charge not held: field too weak', 'B ≥ 100 T·Q/M')] + _mc,
      rate='1', rate_formula='rate = 1', rate_desc='Simulations run at a fixed pace.', heat='15 + 0.02*Bf', idle_heat='4', quench_energy='0.5*Bf^2',
      kappa=40)
WINDOWS['eye_of_the_horizon'] = {
    'simulate_stellar_lifecycle': ({}, [('eisco', 0.15, 0.20)]),
    'simulate_galaxy': ({}, [('OmH', 0.30, 0.40)]),
    'simulate_cosmology': ({}, [('ks', 0.0, 0.05)]),
}

# =================================================================================== 10 neutron star forge
_md, _mc = magnet('Bcoil', 'flux-compression coils')
model('neutron_star_forge', 'Neutron Star Forge',
      'Matter squeezed until electrons are pressed into protons: neutron-degenerate matter, held up by the Pauli principle (P = Kρ^(5/3)). The mass and pressure you choose set the density and radius; general relativity sets the limits.',
      'Compressor pressure ripple: p·(1 ± 1%·d).',
      [P('Ms', 'Mass', 'M☉', 0.1, 3, 0.01, 1.4, 'Mass of the compressed star.'),
       P('lgp', 'Compression pressure', 'log₁₀ Pa', 25, 40, 0.01, 33, 'Central pressure applied, as a power of ten.'),
       P('f', 'Spin frequency', 'Hz', 0, 2000, 1, 10, 'Rotation rate imparted.'),
       P('Bcoil', 'Seed field', 'T', 0, 20, 0.01, 1, 'Field of the coils before flux compression amplifies it into the star.')],
      [D('rho', 'Density ρ', 'kg/m³', '(10^lgp*(1+0.01*d)/5348)^0.6', 'ρ = (P/K)^(3/5),  K = (3π²)^(2/3)ħ²/(5m_n^(8/3))', 'Density from non-relativistic neutron degeneracy pressure.'),
       D('lgrho', 'Density', 'log₁₀ kg/m³', 'log10(rho)', 'log₁₀ ρ', 'Neutron drip at 14.6; nuclear density at 17.4; deconfinement above ~18.'),
       D('R', 'Radius', 'km', 'cbrt(3*Ms*1.989e30/(4*pi*rho))/1000', 'R = (3M/4πρ)^(1/3)', 'Radius of the star.'),
       D('rs', 'Schwarzschild radius', 'km', '2.953*Ms', 'r_s = 2GM/c²', 'Radius at which it would become a black hole.'),
       D('Cc', 'Compactness', '', 'rs/R', 'C = r_s/R', 'Buchdahl: no static star can have C above 8/9.'),
       D('z', 'Surface redshift', '', '1/sqrt(pos(1 - Cc)) - 1', 'z = (1 − r_s/R)^(−½) − 1', 'Gravitational redshift of light from the surface.'),
       D('fK', 'Mass-shedding frequency', 'Hz', 'sqrt(6.674e-11*Ms*1.989e30/(R*1000)^3)/(2*pi)', 'f_K = √(GM/R³)/2π', 'Spinning faster than this flings the equator off.'),
       D('lgB', 'Surface field', 'log₁₀ G', 'log10(Bcoil*(3e5/R)^2*1e4 + 1)', 'B = B₀·(R₀/R)²', 'Field after flux compression from a 300 000 km core. Magnetars exceed 10¹⁴ G.'),
       D('EF', 'Neutron Fermi energy', 'MeV', '3.32e-42*(29.61*rho/1.675e-27)^(2/3)/1.602e-13', 'E_F = ħ²(3π²n)^(2/3)/(2m_n)', 'Energy of the fastest neutrons.')] + _md,
      [C('tov', 'quench', '2.2 - Ms', 'Above the TOV mass: collapse into a black hole', 'M < 2.2 M☉'),
       C('buchdahl', 'quench', '0.8889 - Cc', 'Buchdahl limit exceeded: no static star is possible', 'r_s/R < 8/9'),
       C('shedding', 'stop', 'fK - f', 'Spinning past the mass-shedding limit', 'f < f_K')] + _mc,
      rate='1', rate_formula='rate = 1', rate_desc='Compression proceeds at a fixed pace.', heat='12 + 0.5*Bcoil', idle_heat='3', quench_energy='60*Bcoil^2',
      kappa=40)
WINDOWS['neutron_star_forge'] = {
    'infall_rock_crushing': ({}, [('lgrho', 11.5, 14.0)]),
    'neutronium_compression': ({}, [('lgrho', 14.6, 17.5)]),
    'pulsar_timing_crystal': ({}, [('f', 300, 716), ('lgrho', 17.0, 18.0)]),
    'supernova_remnant': ({}, [('lgB', 12.0, 13.5)]),
    'quark_star_core': ({}, [('lgrho', 18.0, 19.0)]),
}

# =================================================================================== 11-12 vacuum
model('dark_energy_siphon', 'Dark Energy Siphon',
      'Two plates a hair apart exclude vacuum modes from the gap: a Casimir pressure P = π²ħc/(240a⁴) pushes them together, and the vacuum between them holds negative energy. At about 30 µm that energy density equals the dark energy of the universe.',
      'Plate parallelism error: the effective gap is a·(1 + 0.5%·d).',
      [P('a', 'Plate gap a', 'µm', 0.01, 300, 0.01, 1, 'Separation of the Casimir plates.'),
       P('A', 'Plate area', 'm²', 0.01, 100, 0.01, 1, 'Area of each plate.'),
       P('f', 'Cycle rate', 'Hz', 0, 1000, 1, 10, 'Plates are cycled open and shut to harvest energy each stroke.'),
       P('ks', 'Plate stiffness', 'N/m', 1, 1e9, 1, 1000, 'Spring constant of the plate suspension. The Casimir force must not pull the plates in.')],
      [D('ae', 'Effective gap', 'µm', 'a*(1+0.005*d)', 'a_eff', 'Gap including the parallelism error.'),
       D('Pc', 'Casimir pressure', 'Pa', '1.3e-27/(ae*1e-6)^4', 'P = π²ħc/(240a⁴)', 'Attractive pressure between the plates.'),
       D('u', 'Vacuum energy density', 'J/m³', 'Pc/3', '|u| = π²ħc/(720a⁴)', 'Magnitude of the (negative) energy density in the gap.'),
       D('lgu', 'Energy density / ρ_Λ', 'log₁₀', 'log10(u/6e-10)', 'log₁₀(|u|/ρ_Λ)', 'Compared with the dark energy density of the universe, ρ_Λ ≈ 6×10⁻¹⁰ J/m³.'),
       D('grad', 'Force gradient', 'N/m', '4*Pc*A/(ae*1e-6)', 'dF/da = 4PA/a', 'How fast the force grows as the gap closes. Must stay below the plate stiffness.'),
       D('lamT', 'Thermal wavelength', 'µm', '2.29e3/T', 'λ_T = ħc/kT', 'Above this gap thermal photons dominate the force, not the vacuum.'),
       D('Pext', 'Extracted power', 'W', 'u*A*ae*1e-6*f', 'P = |u|·A·a·f', 'Vacuum energy harvested per second.')],
      [C('pullin', 'quench', 'ks - grad', 'Casimir pull-in: the plates snap together', 'dF/da < k'),
       C('thermal', 'stop', 'lamT - ae', 'Thermal photons swamp the vacuum signal: cool the plates', 'a < ħc/kT'),
       overheat(400, 'plate stack')],
      rate='clamp(A*f/50, 0, 4)', rate_formula='rate = A·f / 50 m²/s', rate_desc='Plate area swept per second.',
      heat='3 + 0.002*A*f', idle_heat='1', kappa=40)
WINDOWS['dark_energy_siphon'] = {
    'cosmological_constant': ({}, [('lgu', -0.3, 0.3)]),
    'dark_energy': ({}, [('lgu', 6.0, 9.0)]),
}

_md, _mc = magnet('Bm', 'mirror-drive coils')
model('vacuum_harvester', 'Vacuum Fluctuation Harvester',
      'The dynamical Casimir effect: shake a cavity mirror at twice a mode frequency and real photons are pulled out of the vacuum - parametric amplification of nothing. Here the "photons" are whatever virtual particle the Foam resonates with where you stand.',
      'Mirror drive phase noise: the gain varies by ±2%·d.',
      [P('nu', 'Resonance tuning', '', 0, 100, 0.1, 50, 'Which virtual particle the cavity is tuned to. Match the Foam\'s local resonance (use a Field Calibrator to see it).', bind='setting'),
       P('eps', 'Modulation depth ε', '', 0, 0.01, 0.00001, 0.0001, 'Fractional mirror displacement per stroke.'),
       P('lgQ', 'Cavity quality Q', 'log₁₀', 3, 11, 0.01, 6, 'Loaded quality factor, as a power of ten. Higher Q stores the created quanta longer.'),
       P('Bm', 'Drive field', 'T', 0, 15, 0.05, 1, 'Field of the magnetic mirror drive (ε ≤ 0.002·B).')],
      [D('Gp', 'Parametric gain', '', 'eps*10^lgQ/4*(1+0.02*d)', 'G = εQ/4', 'Below 1 the cavity amplifies the vacuum stably; at 1 it runs away.'),
       D('Nph', 'Quanta per stroke', '', 'Gp^2/pos(1 - Gp^2)', 'N = G²/(1 − G²)', 'Real quanta created per mode per modulation period.'),
       D('Qmax', 'Achievable Q', 'log₁₀', 'log10(1e11/(1 + 1e4*exp(-17.67/T)))', 'Q_max ∝ 1/R_s(T)', 'Best Q the superconducting cavity reaches at this temperature.')] + _md,
      [C('avalanche', 'quench', '0.98 - Gp', 'Parametric instability: the photon number runs away', 'εQ/4 < 1'),
       C('qreach', 'stop', 'Qmax - lgQ', 'The cavity cannot reach that Q at this temperature', 'Q ≤ Q_max(T)'),
       C('drive', 'stop', '0.002*Bm - eps', 'Mirror drive too weak for that modulation depth', 'ε ≤ 0.002·B')] + _mc,
      rate='clamp(Nph/2, 0, 4)', rate_formula='rate = N/2', rate_desc='Harvest rate follows the quanta created per stroke.',
      heat='5 + 1000*eps*Bm', idle_heat='2', quench_energy='30*Bm^2', kappa=40)
WINDOWS['vacuum_harvester'] = {'vacuum_harvest': ({}, [])}

# =================================================================================== 13-17 spacetime
model('wormhole_projector', 'Wormhole Projector',
      'A Morris-Thorne wormhole: a throat of radius b₀ held open by exotic matter of negative energy, about b₀c²/2G of it. Cargo crossing it feels tides that grow with speed - go slowly through a wide throat.',
      'Exotic-matter leakage: the throat needs M·(1 ± 2%·d).',
      [P('b0', 'Throat radius b₀', 'm', 0.1, 1000, 0.1, 10, 'Radius of the wormhole throat.'),
       P('v', 'Transit speed', 'km/s', 0, 300000, 0.1, 1, 'Speed of cargo through the throat.'),
       P('Mx', 'Exotic matter', 'M_J', 0, 1000, 0.1, 10, 'Negative-energy matter supplied to the throat, in Jupiter masses.'),
       P('Lw', 'Throat length', 'm', 1, 10000, 1, 100, 'Proper length of the tunnel.')],
      [D('Mreq', 'Exotic matter needed', 'M_J', '0.3546*b0*(1+0.02*d)', 'M = b₀c²/(2G)', 'Exotic mass needed to hold the throat open.'),
       D('gam', 'Lorentz factor', '', '1/sqrt(pos(1 - (v/299792)^2))', 'γ = 1/√(1 − v²/c²)', 'Time dilation of the cargo.'),
       D('tide', 'Tidal acceleration', 'g', '(gam*v*1000)^2*0.25/b0^2/9.81', 'Δa = γ²v²ℓ/b₀²', 'Stretching across a 25 cm crate. Over 100 g it is torn apart.'),
       D('tt', 'Transit time', 's', 'Lw/(v*1000 + 1e-9)', 't = L/v', 'Time to cross the throat.')],
      [C('exotic', 'stop', 'Mx - Mreq', 'Not enough exotic matter: the throat pinches off', 'M_x ≥ b₀c²/2G'),
       C('tidal', 'stop', '100 - tide', 'Tidal forces would shred the cargo', 'Δa < 100 g'),
       overheat(500, 'throat frame')],
      rate='clamp(b0/100, 0, 4)', rate_formula='rate = b₀ / 100 m', rate_desc='A wider throat passes more cargo at once.',
      heat='8 + 0.01*Mx', idle_heat='2', kappa=40)
WINDOWS['wormhole_projector'] = {
    'distant_star_system': ({}, [('tt', 10, 60), ('b0', 50, 1000)]),
    'neighbouring_brane': ({}, [('b0', 200, 1000), ('tt', 1, 30)]),
}

_md, _mc = magnet('Bm', 'mouth-steering coils')
model('causality_loop_engine', 'Causality Loop Engine',
      'Take one mouth of a wormhole on a fast round trip: it ages less (twin paradox). Once its time lag exceeds the light-travel time between the mouths, the pair is a time machine - a closed timelike curve. Near that threshold, vacuum fluctuations pile up (Hawking\'s chronology protection).',
      'Mouth trajectory error: the trip speed is v·(1 ± 0.01%·d).',
      [P('v', 'Mouth speed', 'c', 0, 0.9999, 0.0001, 0.5, 'Speed of the travelling mouth.'),
       P('Tt', 'Trip duration', 's', 0, 10000, 1, 100, 'Duration of the round trip in the stay-at-home frame.'),
       P('L', 'Mouth separation', 'km', 1, 1e7, 1, 1000, 'Distance between the mouths once back.'),
       P('Bm', 'Steering field', 'T', 0, 15, 0.05, 1, 'Field that steers the charged mouth.')],
      [D('ve', 'Effective speed', 'c', 'v*(1+0.0001*d)', 'v', 'Speed including trajectory error.', show=False),
       D('gam', 'Lorentz factor', '', '1/sqrt(pos(1 - ve^2))', 'γ = 1/√(1 − v²)', 'Time dilation of the travelling mouth.'),
       D('lag', 'Time lag', 's', 'Tt*(1 - 1/gam)', 'Δt = T(1 − 1/γ)', 'How much less the travelling mouth has aged.'),
       D('tl', 'Light time', 's', 'L*1000/2.998e8', 't_L = L/c', 'Light travel time between the mouths.'),
       D('loop', 'Loop length', 's', 'lag - tl', 'Δt − L/c', 'How far back a signal can go: the length of the closed timelike curve.'),
       D('ratio', 'Lag / light time', '', 'lag/tl', 'Δt/(L/c)', 'A time machine exists above 1. Near 1, chronology protection bites.'),
       D('acc', 'Turnaround acceleration', 'g', 'gam*ve*2.998e8/(Tt/2 + 1e-9)/9.81', 'a = γvc/(T/2)', 'Acceleration needed to turn the mouth around.')] + _md,
      [C('ctc', 'stop', 'ratio - 1', 'No closed timelike curve yet: the lag is shorter than the light time', 'Δt > L/c'),
       C('chronology', 'quench', 'abs(ratio - 1) - 0.002', 'Chronology protection: vacuum polarisation diverges at the Cauchy horizon', '|Δt/(L/c) − 1| > 0.002'),
       C('near_horizon', 'wear', 'ratio - 1.05', 'Close to the chronology horizon: the structure strains', 'Δt/(L/c) > 1.05'),
       C('steer', 'stop', 'Bm*1e4 - acc', 'Steering field too weak for that turnaround', 'a ≤ 10⁴ g/T')] + _mc,
      rate='1', rate_formula='rate = 1', rate_desc='Loops run at a fixed pace.', heat='10 + 0.5*Bm', idle_heat='3', quench_energy='40*Bm^2', kappa=40)
WINDOWS['causality_loop_engine'] = {
    'closed_timelike_curve': ({}, [('loop', 1, 10)]),
    'bootstrap_artifact': ({}, [('loop', 10, 60)]),
}
for _r in ('loop_micro_singularity', 'loop_pocket_universe', 'loop_stellar_core', 'loop_superintelligence_core', 'loop_tesseract_core',
           'loop_white_hole_fragment', 'loop_wormhole_throat'):
    WINDOWS['causality_loop_engine'][_r] = ({}, [('loop', 60, 600)])

_md, _mc = magnet('Bs', 'radion-stabiliser coils')
model('tesseract_assembler', 'Tesseract Assembler',
      'A fifth dimension curled into a circle of radius R₅ shows up as a tower of Kaluza-Klein particles of mass nħc/R₅. A 4-cube rotated against our space casts a 3-D shadow whose volume (Cauchy) is the sum of |nᵢ| over the components of its tilt.',
      'Rotation-stage backlash: the xw angle is α + 0.5°·d.',
      [P('R5', 'Compact radius R₅', 'fm', 0.0001, 100, 0.0001, 1, 'Radius of the curled-up fifth dimension.'),
       P('al', 'xw rotation α', '°', 0, 90, 0.1, 0, 'Rotation of the 4-cube in the x-w plane.'),
       P('be', 'yw rotation β', '°', 0, 90, 0.1, 0, 'Rotation of the 4-cube in the y-w plane.'),
       P('Bs', 'Stabiliser field', 'T', 0, 15, 0.05, 2, 'Field holding the radion (the size of R₅) in place.')],
      [D('m1', 'First KK mass', 'GeV', '0.1973/R5', 'm₁ = ħc/R₅', 'Mass of the lightest Kaluza-Klein excitation.'),
       D('lgm1', 'First KK mass', 'log₁₀ GeV', 'log10(m1)', 'log₁₀ m₁', 'As a power of ten.'),
       D('ar', 'Effective α', 'rad', '(al + 0.5*d)*0.0174533', 'α', 'Rotation angle with backlash.', show=False),
       D('Vp', 'Shadow volume', '', 'abs(sin(ar)) + abs(cos(ar)*sin(be*0.0174533)) + abs(cos(ar)*cos(be*0.0174533))',
         'V = Σ|nᵢ|  (Cauchy projection formula)', 'Volume of the 3-D shadow of the unit tesseract: 1 face-on, up to √3 tilted.'),
       D('Bneed', 'Radion field needed', 'T', '1 + 2*log10(m1 + 1)', 'B ≥ 1 + 2·log₁₀(1 + m₁)', 'Heavier towers need firmer stabilisation.')] + _md,
      [C('radion', 'stop', 'Bs - Bneed', 'Radion not stabilised: R₅ drifts', 'B ≥ B_needed')] + _mc,
      rate='1', rate_formula='rate = 1', rate_desc='Assembly runs at a fixed pace.', heat='8 + 0.5*Bs', idle_heat='3', quench_energy='40*Bs^2', kappa=40)
_tess = ['klein_bottle', 'alcubierre_coil', 'wormhole_throat', 'grand_unified_core', 'hopf_fibration', 'tesseract_core', 'tesseract_lens',
         'einstein_rosen_core', 'planck_shard', 'penteract_lattice', 'cosmic_board', 'e8_lattice', 'spacetime_catalyst']
_vwin = [(1.0, 1.05), (1.30, 1.42), (1.55, 1.73)]
WINDOWS['tesseract_assembler'] = {}
for _i, _r in enumerate(_tess):
    lo = -1.0 + _i * 0.35
    WINDOWS['tesseract_assembler'][_r] = ({}, [('Vp',) + _vwin[_i % 3], ('lgm1', round(lo, 2), round(lo + 0.6, 2))])

model('multiverse_rift', 'Multiverse Rift',
      'Randall-Sundrum: our brane sits at the end of a warped fifth dimension, and energies there are suppressed by e^(−πkr). With kr ≈ 11 the Planck scale shrinks to TeV. The brane tensions must cancel almost exactly - the cosmological-constant problem, by hand.',
      'Tension drift: the residual vacuum energy is δ + 0.002·d. Re-tune δ to cancel it.',
      [P('kr', 'Warp k·r', '', 0, 20, 0.001, 5, 'Curvature times brane separation.'),
       P('k', 'Curvature k', 'M_Pl', 0.01, 1, 0.001, 0.1, 'AdS curvature of the bulk, in Planck units.'),
       P('dl', 'Tension offset δ', '', -0.01, 0.01, 0.00001, 0, 'Detuning of the brane tension from the Randall-Sundrum value.')],
      [D('w', 'Warp factor', '', 'exp(-pi*kr)', 'w = e^(−πkr)', 'Suppression of every energy on our brane.'),
       D('Lam', 'Brane scale Λ', 'GeV', '2.435e18*w', 'Λ = M_Pl·e^(−πkr)', 'The effective Planck scale on our brane.'),
       D('lgLam', 'Brane scale', 'log₁₀ GeV', 'log10(Lam)', 'log₁₀ Λ', 'As a power of ten.'),
       D('m1', 'First KK graviton', 'GeV', '3.83*k*Lam', 'm₁ = 3.83·k·Λ', 'Mass of the first Kaluza-Klein graviton.'),
       D('L4', 'Residual vacuum energy', '', 'dl + 0.002*d', 'Λ₄ = δ + drift', 'Leftover four-dimensional cosmological constant.')],
      [C('flat', 'stop', '0.0005 - abs(L4)', 'Brane mistuned: it inflates or crunches', '|Λ₄| < 5×10⁻⁴'),
       overheat(500, 'rift frame')],
      rate='1', rate_formula='rate = 1', rate_desc='The rift is harvested at a fixed pace.', heat='15', idle_heat='3', kappa=40)
WINDOWS['multiverse_rift'] = {
    'alien_matter': ({}, [('lgLam', 3.0, 4.0)]),
    'false_vacuum': ({}, [('lgLam', 9.0, 10.0)]),
    'rift_harvest': ({}, [('lgLam', 5.0, 6.0)]),
}

_md, _mc = magnet('Bs', 'inflaton-trap coils')
model('universe_forge', 'Universe Forge',
      'Seed a universe with a slowly rolling scalar field V = m⁴φᵖ. The slow-roll parameters at the moment the largest scales leave the horizon fix the spectrum of the universe that results: tilt n_s, amplitude A_s, gravitational waves r. Ours: n_s = 0.965, A_s = 2.1×10⁻⁹.',
      'Inflaton coupling noise: the power p seen is p + 0.01·d.',
      [P('p', 'Potential power p', '', 0.25, 4, 0.01, 2, 'Shape of the potential V ∝ φᵖ.'),
       P('Ns', 'e-folds after CMB exit', '', 40, 80, 0.1, 60, 'How many e-folds of inflation remain after today\'s largest scales left the horizon.'),
       P('Nt', 'Total e-folds', '', 0, 200, 0.1, 70, 'Total expansion, as ln(a_end/a_start).'),
       P('lgm', 'Mass scale', 'log₁₀ M_Pl', -8, -1, 0.001, -3, 'Scale m of the potential.'),
       P('Bs', 'Trap field', 'T', 0, 15, 0.05, 2, 'Field confining the seed region.')],
      [D('pe', 'Effective power', '', 'p + 0.01*d', 'p', 'Potential power with coupling noise.', show=False),
       D('phis', 'Field at CMB exit', 'M_Pl', 'sqrt(2*pe*Ns + pe^2/4)', 'φ* = √(2pN + p²/4)', 'Inflaton value when the CMB scales left the horizon.'),
       D('eps', 'Slow-roll ε', '', 'pe^2/(2*phis^2)', 'ε = ½(V′/V)²', 'First slow-roll parameter.'),
       D('eta', 'Slow-roll η', '', 'pe*(pe - 1)/phis^2', 'η = V″/V', 'Second slow-roll parameter.'),
       D('ns', 'Spectral tilt n_s', '', '1 - 6*eps + 2*eta', 'n_s = 1 − 6ε + 2η', 'Tilt of the primordial spectrum. Ours: 0.965 ± 0.004.'),
       D('r', 'Tensor ratio r', '', '16*eps', 'r = 16ε', 'Strength of primordial gravitational waves.'),
       D('lgAs', 'Amplitude A_s', 'log₁₀', '4*lgm + pe*log10(phis) - log10(24*pi^2*eps)', 'A_s = V/(24π²ε)', 'Amplitude of the density perturbations. Ours: 2.1×10⁻⁹ (log −8.68).')] + _md,
      [C('horizon', 'stop', 'Nt - Ns', 'Inflation too short: the horizon problem is not solved', 'N_total ≥ N*')] + _mc,
      rate='1', rate_formula='rate = 1', rate_desc='The forge runs at a fixed pace.', heat='12 + 0.5*Bs', idle_heat='3', quench_energy='40*Bs^2', kappa=40)
WINDOWS['universe_forge'] = {
    'pocket_universe': ({}, [('ns', 0.955, 0.975), ('lgAs', -8.9, -8.5), ('r', 0, 0.1)]),
    'harvest_universe': ({}, [('Nt', 60, 200)]),
    'recycle_remnants': ({}, [('ns', 0.90, 1.0)]),
}

# =================================================================================== 18-22 computation
model('matrioshka_brain', 'Matrioshka Brain',
      'Nested shells around a star, each running on the waste heat of the one inside. Computation is limited by Landauer: erasing a bit costs kT·ln2, so the cold outer shell sets the price - and the coolant sets how cold it can be.',
      'Stellar luminosity variation: the inner shell runs at T·(1 ± 1%·d).',
      [P('r1', 'Inner shell radius', 'AU', 0.01, 10, 0.001, 1, 'Orbit of the innermost shell.'),
       P('Nsh', 'Number of shells', '', 1, 12, 1, 3, 'Each shell runs on the heat of the one inside it.'),
       P('fd', 'Duty', '', 0, 1, 0.01, 1, 'Fraction of the starlight captured and used.')],
      [D('T1', 'Inner shell temperature', 'K', '278.6/sqrt(r1)*(1+0.01*d)', 'T₁ = 278.6 K/√(r/AU)', 'Equilibrium temperature of the innermost shell.'),
       D('Tout', 'Outer shell temperature', 'K', 'max(T, T1*exp(-0.5*Nsh))', 'T_out = max(T_coolant, T₁e^(−N/2))', 'Temperature of the outermost shell; it cannot be colder than the coolant.'),
       D('eta', 'Carnot efficiency', '', '1 - Tout/T1', 'η = 1 − T_out/T₁', 'Fraction of the starlight that can do work.'),
       D('lgops', 'Bit operations', 'log₁₀ /s', 'log10(3.828e26*fd*pos(eta)/(1.381e-23*Tout*0.6931) + 1)', 'N = ηL/(kT·ln2)', 'Irreversible bit operations per second at the Landauer limit.')],
      [C('melt', 'damage', '1500 - T1', 'The inner shell is melting', 'T₁ < 1500 K'), overheat(600, 'core')],
      rate='clamp(fd*eta*1.5, 0, 4)', rate_formula='rate = 1.5·duty·η', rate_desc='Computation rate follows the usable power.',
      heat='5 + 30*fd*(1 - eta)', idle_heat='2', kappa=40)
WINDOWS['matrioshka_brain'] = {
    'quantum_processors': ({}, [('lgops', 46, 60)]),
    'contemplate': ({}, [('lgops', 48, 60)]),
    'superintelligence': ({}, [('lgops', 49.3, 60)]),
}

model('reality_compiler', 'Reality Compiler',
      'A fault-tolerant quantum computer. Each logical qubit is a surface-code patch of 2d² physical qubits; the logical error falls as (p/p_th)^((d+1)/2) - if the physical error p is below 1%. And p depends on temperature: hf/kT must be large, or thermal excitations flip the qubits.',
      'Control-line crosstalk: gate errors grow by 20%·|d|.',
      [P('dist', 'Code distance d', '', 3, 61, 2, 5, 'Size of each surface-code patch.'),
       P('fq', 'Qubit frequency', 'GHz', 1, 20, 0.01, 5, 'Higher frequency, larger hf/kT - but more heat per qubit.'),
       P('clk', 'Gate clock', 'MHz', 0.1, 100, 0.1, 1, 'Speed of the syndrome cycle. Faster gates make more errors.'),
       P('NL', 'Logical qubits', '', 1, 1000, 1, 50, 'How many logical qubits the program uses.')],
      [D('pth', 'Thermal error', '', 'exp(-0.048*fq/T)', 'p_th = e^(−hf/kT)', 'Probability a qubit is thermally excited.'),
       D('pg', 'Gate error', '', '1e-4*(1 + clk/10)*(1 + 0.2*abs(d))', 'p_g', 'Control error per gate.'),
       D('pp', 'Physical error p', '', 'pg + pth', 'p = p_g + p_th', 'Total physical error rate.'),
       D('lgpL', 'Logical error', 'log₁₀', 'log10(0.1) + (dist + 1)/2*log10(pp/0.01)', 'p_L = 0.1(p/p_th)^((d+1)/2)', 'Logical error per cycle, as a power of ten.'),
       D('Nphys', 'Physical qubits', '', '2*dist^2*NL', 'N = 2d²·N_L', 'Physical qubits in the machine.'),
       D('Pq', 'Wiring heat', 'kW', '1e-5*Nphys*sqrt(clk)*(fq/5)', 'P ∝ N·√f_clk·f_q', 'Heat leaking down the control wiring.')],
      [C('threshold', 'stop', '0.01 - pp', 'Above the threshold: error correction makes things worse', 'p < 1%')],
      rate='clamp(clk*NL/(5*dist), 0, 4)', rate_formula='rate = f_clk·N_L/(5d)', rate_desc='Logical operations per second.',
      heat='Pq', idle_heat='0.05', kappa=40, heat_capacity=5)
WINDOWS['reality_compiler'] = {
    'reality_anchor': ({}, [('NL', 50, 1000), ('lgpL', -30, -9)]),
    'source_code_fragment': ({}, [('NL', 100, 1000), ('lgpL', -30, -12)]),
    'omega_point': ({}, [('NL', 200, 1000), ('lgpL', -40, -15)]),
    'eternity_catalyst': ({}, [('NL', 300, 1000), ('lgpL', -40, -15)]),
}

_md, _mc = magnet('Bh', 'holographic-store coils')
model('concept_compressor', 'Concept Compressor',
      'Shannon\'s source coding theorem: a source of entropy H bits per symbol can be compressed to any rate R above H - and to none below. At finite block length n, the error falls as exp(−n(R−H)²/2V). Every concept has its own entropy.',
      'Source drift: the concept\'s entropy fluctuates by ±1%·d.',
      [P('R', 'Code rate R', 'bit/symbol', 0, 1, 0.001, 0.6, 'Bits written per source symbol. Compression ratio is 1/R.'),
       P('n', 'Block length n', 'symbols', 10, 1000000, 10, 1000, 'Symbols coded together. Longer blocks, fewer errors, slower.'),
       P('Bh', 'Store field', 'T', 0, 150, 0.1, 10, 'Field of the holographic store (needs 5 T per bit of rate).')],
      [D('H', 'Entropy H', 'bit/symbol', '-(qb*ln(qb) + (1-qb)*ln(1-qb))/0.693147*(1+0.01*d)', 'H = −q log q − (1−q) log(1−q)', 'Information content of the concept per symbol.'),
       D('Vd', 'Dispersion V', 'bit²', 'qb*(1-qb)*(ln((1-qb)/qb)/0.693147)^2', 'V = q(1−q)·log²((1−q)/q)', 'Variance of the information per symbol.'),
       D('gap', 'Rate margin R − H', 'bit', 'R - H', 'R − H', 'How far above the entropy the code runs.'),
       D('lgPe', 'Error probability', 'log₁₀', '-n*pos(gap)^2/(2*Vd)/2.302585', 'P_e ≈ exp(−n(R−H)²/2V)', 'Chance a block is decoded wrongly.'),
       D('ratio', 'Compression ratio', '', '1/R', '1/R', 'How much smaller the concept becomes.')] + _md,
      [C('shannon', 'stop', 'gap', 'Rate below the entropy: information is destroyed', 'R > H'),
       C('store', 'stop', 'Bh - 5*R*10', 'Store field too weak for that rate', 'B ≥ 50 T·R')] + _mc,
      rate='clamp(3*(1000/n)^0.3, 0, 4)', rate_formula='rate = 3·(1000/n)^0.3', rate_desc='Shorter blocks compress faster.',
      heat='10 + 0.1*Bh', idle_heat='3', quench_energy='5*Bh^2',
      rvars=[V('qb', 'Concept bias q', 0.1)], kappa=40)
_cq = {'quantum_singularity': 0.11, 'spacetime_singularity': 0.05, 'standard_model_singularity': 0.08, 'dimensional_singularity': 0.15,
       'stellar_singularity': 0.2, 'temporal_singularity': 0.03, 'exotic_matter_singularity': 0.25, 'mathematics_singularity': 0.02,
       'cognitive_singularity': 0.3, 'theory_of_everything': 0.01, 'omnium_ingot': 0.04, 'omnium_ingot_transfinite': 0.04}
WINDOWS['concept_compressor'] = {}
for _r, _q in _cq.items():
    _H = -(_q * math.log2(_q) + (1 - _q) * math.log2(1 - _q))
    WINDOWS['concept_compressor'][_r] = ({'qb': _q}, [('ratio', round(1 / (_H * 1.25), 2), 1000), ('lgPe', -1000, -9)])

model('omnilex_engine', 'Omnilex Engine',
      'Laws as dynamical systems: the logistic map x → rx(1−x) doubles its period at r = 3, 3.449, 3.544... converging (Feigenbaum, δ = 4.669) on chaos at r∞ = 3.5699. A lattice of coupled laws synchronises only if the coupling beats the chaos: ε > 1 − e^(−λ).',
      'Law drift: r is r + 0.0002·d.',
      [P('r', 'Growth parameter r', '', 2.5, 4, 0.00001, 3.2, 'The logistic map\'s parameter.'),
       P('eps', 'Lattice coupling ε', '', 0, 1, 0.001, 0.1, 'How strongly neighbouring laws are coupled.')],
      [D('re', 'Effective r', '', 'r + 0.0002*d', 'r', 'r with drift.', show=False),
       D('nb', 'Bifurcation index', '', 'step(3.5699456 - re)*(ln(pos(0.5699456/(3.5699456 - re)))/1.540996 + 1) + step(re - 3.5699456)*20',
         'n ≈ ln((r∞ − 3)/(r∞ − r))/ln δ + 1', 'Approximate period-doubling stage: period 2ⁿ (20 = chaotic).'),
       D('lam', 'Lyapunov exponent λ', '', 'step(re - 3.5699456)*0.9*pos(re - 3.5699456)^0.4498 - step(3.5699456 - re)*0.1', 'λ ≈ 0.9(r − r∞)^0.45',
         'Rate at which nearby histories diverge. Positive means chaos.'),
       D('esync', 'Synchronisation threshold', '', '1 - exp(-pos(lam))', 'ε_c = 1 − e^(−λ)', 'Coupling needed to synchronise the lattice.')],
      [C('sync', 'stop', 'eps - esync', 'Laws not synchronised: the lattice tears into incoherent regions', 'ε > 1 − e^(−λ)'),
       C('range', 'stop', '4 - re', 'r above 4: the map leaves the unit interval', 'r ≤ 4')],
      rate='1', rate_formula='rate = 1', rate_desc='Simulations run at a fixed pace.', heat='20', idle_heat='4', kappa=40)
WINDOWS['omnilex_engine'] = {
    'tree_simulacrum': ({}, [('nb', 3, 4)]),
    'ocean_simulacrum': ({}, [('lam', 0.3, 1.0)]),
    'grow_bubble_universes': ({}, [('nb', 2, 3)]),
    'rewrite_laws': ({}, [('nb', 4, 5)]),
    'simulate_everything': ({}, [('lam', 0.5, 1.0)]),
    'omnilex': ({}, [('nb', 5, 6)]),
    'the_impossible': ({}, []),
}

_md, _mc = magnet('Bb', 'barrier-shaping coils')
model('axiomatic_breach_engine', 'Axiomatic Breach Engine',
      'A breach is a bubble of different vacuum (Coleman-De Luccia). A wall of tension σ around a region whose energy is lower by Δε has critical radius R = 3σ/Δε, and the nucleation rate goes as exp(−B), B = 27π²σ⁴/(2Δε³). Too fast, and the bubble swallows everything.',
      'Vacuum energy noise: Δε·(1 ± 0.2%·d).',
      [P('sig', 'Wall tension σ', 'TeV³', 0.001, 10, 0.00001, 1, 'Surface tension of the bubble wall.'),
       P('de', 'Energy difference Δε', 'TeV⁴', 0.001, 10, 0.0001, 1, 'How much lower the breached vacuum lies.'),
       P('Bb', 'Barrier field', 'T', 0, 15, 0.05, 2, 'Shapes the barrier between the vacua (needs 2 T per TeV³ of tension).')],
      [D('dee', 'Effective Δε', 'TeV⁴', 'de*(1+0.002*d)', 'Δε', 'With noise.', show=False),
       D('Rc', 'Critical radius', 'm', '3*sig/dee*1.973e-19', 'R_c = 3σ/Δε', 'Smallest bubble that grows rather than collapses.'),
       D('Bact', 'Bounce action B', '', '27*pi^2*sig^4/(2*dee^3)', 'B = 27π²σ⁴/(2Δε³)', 'Tunnelling exponent.'),
       D('lgN', 'Bubbles per second', 'log₁₀', 'log10(dee) - Bact/2.302585 + 83.3', 'N = Δε·e^(−B)·V·t', 'Expected bubbles nucleated per second in the 1 m³ chamber.')] + _md,
      [C('runaway', 'quench', '3 - lgN', 'Bubbles everywhere: the breach spreads beyond the chamber', 'log N < 3'),
       C('barrier', 'stop', 'Bb - 2*sig', 'Barrier not shaped: the wall cannot form', 'B ≥ 2 T·σ')] + _mc,
      rate='1', rate_formula='rate = 1', rate_desc='The breach runs at a fixed pace.', heat='20 + 0.5*Bb', idle_heat='4', quench_energy='60*Bb^2', kappa=40)
WINDOWS['axiomatic_breach_engine'] = {
    'open_breach': ({}, [('lgN', -1, 1)]),
    'axiom_key': ({}, [('lgN', -3, -1), ('Rc', 1e-18, 1)]),
}

model('research_nexus', 'Research Nexus',
      'A discovery is a statistical claim. With s expected signal events over b background, the Asimov significance is Z = √(2((s+b)ln(1+s/b) − s)). Search K hypotheses at once and the look-elsewhere effect eats into it. Tight cuts keep the signal and kill the background - up to a point.',
      'Systematic drift: the signal estimate wanders by ±5%·d.',
      [P('Lint', 'Exposure', 'units', 0.1, 10000, 0.1, 10, 'How much data is taken. More data, more significance - and more time.'),
       P('cut', 'Selection cut', '', 0, 0.99, 0.01, 0.5, 'How hard events are selected. Signal survives as 1 − c³, background as (1 − c)².'),
       P('Kh', 'Hypotheses tried', '', 1, 1000, 1, 10, 'Searches run in parallel. Faster, but every extra look dilutes the significance.')],
      [D('s', 'Signal events', '', 'ss*Lint*(1 - cut^3)*(1+0.05*d)', 's = σ_s·L·ε_s', 'Expected signal after cuts.'),
       D('b', 'Background events', '', 'sb*Lint*(1 - cut)^2 + 0.01', 'b = σ_b·L·ε_b', 'Expected background after cuts.'),
       D('Z', 'Local significance', 'σ', 'sqrt(pos(2*((s + b)*ln(1 + s/b) - s)))', 'Z = √(2((s+b)ln(1+s/b) − s))', 'Asimov significance of the signal.'),
       D('Zg', 'Global significance', 'σ', 'sqrt(pos(Z^2 - 2*ln(Kh)))', 'Z_g² ≈ Z² − 2 ln K', 'Significance after the look-elsewhere effect.')],
      [overheat(400, 'data centre')],
      rate='clamp(100*sqrt(Kh)/Lint, 0, 4)', rate_formula='rate = 100·√K/L', rate_desc='Less data and more parallel searches finish sooner.',
      heat='5 + 0.001*Lint', idle_heat='1', rvars=[V('ss', 'Signal rate', 1.0), V('sb', 'Background rate', 10.0)], kappa=40)
WINDOWS['research_nexus'] = {
    'nexus_empirical': ({'ss': 2.0, 'sb': 10.0}, [('Zg', 3, 100)]),
    'nexus_theoretical': ({'ss': 1.0, 'sb': 10.0}, [('Zg', 4, 100)]),
    'nexus_unified': ({'ss': 0.5, 'sb': 10.0}, [('Zg', 5, 100)]),
    'nexus_absolute': ({'ss': 0.2, 'sb': 10.0}, [('Zg', 6, 100)]),
}

# =================================================================================== 23-28 matter, chemistry, life
_md, _mc = magnet('Bt', 'magnetic-tweezer coils')
model('femtoscale_assembly_line', 'Femtoscale Assembly Line',
      'Parts are held in optical and magnetic traps of stiffness κ. Equipartition sets the jitter: σ = √(kT/κ). To place parts to a picometre you need a stiff trap and a cold one - but light heats the trap and ultraviolet damages the parts.',
      'Laser pointing noise: stiffness varies by ±3%·d.',
      [P('Pl', 'Trap laser power', 'W', 0, 100, 0.01, 1, 'Power in the optical tweezers.'),
       P('lam', 'Wavelength', 'nm', 200, 2000, 1, 1064, 'Shorter wavelength traps harder (κ ∝ λ⁻³) but damages more.'),
       P('Bt', 'Tweezer field', 'T', 0, 15, 0.05, 1, 'Magnetic tweezers add stiffness ∝ B².')],
      [D('kap', 'Trap stiffness κ', 'pN/nm', '(50*Pl*(1064/lam)^3 + 2*Bt^2)*(1+0.03*d)', 'κ ∝ P/λ³ + B²', 'Restoring force per displacement.'),
       D('Tt', 'Trap temperature', 'K', 'T + 0.05*Pl', 'T_trap = T + absorbed light', 'Temperature of the trapped part.'),
       D('sig', 'Placement jitter σ', 'pm', 'sqrt(1.381e-23*Tt/(kap*1e-3))*1e12', 'σ = √(kT/κ)', 'Thermal position noise of a trapped part.'),
       D('dmg', 'Photodamage index', '', 'Pl*(400/lam)^3/5', '∝ P/λ³', 'Above 1, ultraviolet light breaks bonds in the parts.')] + _md,
      [C('photo', 'wear', '1 - dmg', 'Photodamage: the light is breaking the parts', 'damage < 1')] + _mc,
      rate='clamp(kap/200, 0, 4)', rate_formula='rate = κ/200', rate_desc='A stiffer trap settles faster.',
      heat='2 + 0.3*Pl + 0.2*Bt', idle_heat='2', quench_energy='40*Bt^2', kappa=40)
WINDOWS['femtoscale_assembly_line'] = {}   # filled from recipe energies in finish()

model('fractionation_spire', 'Fractionation Spire',
      'A 40-tray distillation column. Fenske gives the fewest trays at total reflux, Underwood the least reflux at infinite trays, and Gilliland what lies between. The condenser must be colder than the light fraction boils.',
      'Feed composition drift: x_F·(1 ± 5%·d).',
      [P('Rr', 'Reflux ratio R', '', 0, 50, 0.01, 3, 'Liquid returned to the column per unit of product.'),
       P('Qr', 'Reboiler duty', 'kW', 0, 2000, 1, 50, 'Heat driving the column. All of it leaves through the condenser into the coolant.'),
       P('xD', 'Product purity', '', 0.5, 0.999999, 0.000001, 0.95, 'Target mole fraction of the light component in the product.')],
      [D('xFe', 'Feed fraction', '', 'clamp(xF*(1+0.05*d), 0.001, 0.999)', 'x_F', 'Light-component fraction in the feed.', show=False),
       D('Nmin', 'Minimum stages (Fenske)', '', 'ln((xD/(1-xD))*((1-0.05)/0.05))/ln(al)', 'N_min = ln[(x_D/(1−x_D))((1−x_B)/x_B)] / ln α', 'Trays needed at total reflux.'),
       D('Rmin', 'Minimum reflux (Underwood)', '', '(xD/xFe - al*(1-xD)/(1-xFe))/(al - 1)', 'R_min = (x_D/x_F − α(1−x_D)/(1−x_F))/(α − 1)', 'Reflux needed with infinitely many trays.'),
       D('Xg', 'Gilliland X', '', 'pos(Rr - Rmin)/(Rr + 1)', 'X = (R − R_min)/(R + 1)', 'Excess reflux.', show=False),
       D('Nreq', 'Stages needed (Gilliland)', '', '(Nmin + 0.75*(1 - Xg^0.5668))/(1 - 0.75*(1 - Xg^0.5668))', 'Y = 0.75(1 − X^0.5668),  N = (N_min + Y)/(1 − Y)', 'Trays needed at this reflux (Eduljee\'s fit).'),
       D('Dr', 'Product rate', 'mol/s', 'Qr/((Rr + 1)*dH)', 'D = Q/((R+1)ΔH_vap)', 'Product drawn off per second.')],
      [C('pinch', 'stop', 'Rr - Rmin', 'Reflux below the Underwood minimum: the column pinches', 'R > R_min'),
       C('stages', 'stop', '40 - Nreq', 'Not enough trays for that purity at this reflux', 'N_req ≤ 40'),
       C('condense', 'stop', 'Tb - T', 'Condenser too warm: the light fraction will not condense', 'T < T_boil')],
      rate='clamp(Dr/0.5, 0, 4)', rate_formula='rate = D / 0.5 mol/s', rate_desc='Product drawn off per second.',
      heat='Qr', idle_heat='0.5', rvars=[V('al', 'Relative volatility α', 2.0), V('xF', 'Feed fraction x_F', 0.3), V('Tb', 'Light boiling point (K)', 400),
                                          V('dH', 'Heat of vaporisation (kJ/mol)', 20)], kappa=60)
WINDOWS['fractionation_spire'] = {
    'warm_helium_fraction': ({'al': 3.0, 'xF': 0.15, 'Tb': 4.2, 'dH': 0.08}, [('xD', 0.99, 1)]),
    'dimensional_residue': ({'al': 1.5, 'xF': 0.3, 'Tb': 500, 'dH': 30}, [('xD', 0.999, 1)]),
}

model('abiogenesis_crucible', 'Abiogenesis Crucible',
      'Life\'s first molecules polymerise (Arrhenius, Eₐ = 60 kJ/mol) while water hydrolyses them (80 kJ/mol). Copying is imperfect: Eigen\'s error threshold says a genome of length L survives only if L < ln σ / µ. Sparks drive chemistry but also mutate.',
      'Temperature swings: the error rate varies by ±10%·d.',
      [P('Tr', 'Reactor temperature', 'K', 250, 700, 0.1, 350, 'Temperature of the primordial broth. Cannot be colder than the coolant.'),
       P('c', 'Monomer concentration', 'mol/L', 0, 10, 0.01, 1, 'Concentration of building blocks (wet-dry cycling concentrates them).'),
       P('Fuv', 'Spark & UV flux', 'W/m²', 0, 1000, 1, 50, 'Energy driving the chemistry. Also mutates.')],
      [D('kp', 'Polymerisation rate', '1/s', '1e8*exp(-60000/(8.314*Tr))*(1 + Fuv/100)', 'k = A·e^(−Eₐ/RT)', 'Bonds formed per second (Arrhenius).'),
       D('kh', 'Hydrolysis rate', '1/s', '1e10*exp(-80000/(8.314*Tr))', 'k_h = A_h·e^(−E_h/RT)', 'Bonds broken by water per second.'),
       D('Y', 'Polymer yield', '', 'kp*c/(kp*c + kh)', 'Y = k_p c/(k_p c + k_h)', 'Fraction of chains that keep growing.'),
       D('mu', 'Copy error µ', '/nt', '(1e-3 + 2e-5*Fuv + 1e-4*exp((Tr - 400)/40))*(1+0.1*d)', 'µ', 'Errors per nucleotide copied.'),
       D('Lmax', 'Error threshold', 'nt', 'ln(sg)/mu', 'L_max = ln σ / µ', 'Longest genome that can persist (Eigen).')],
      [C('eigen', 'stop', 'Lmax - L', 'Error catastrophe: the genome cannot persist', 'L < ln σ/µ'),
       C('cool', 'stop', 'Tr - T', 'The crucible cannot be held colder than its coolant', 'T_r ≥ T_coolant')],
      rate='clamp(4*Y*min(1, Fuv/50), 0, 4)', rate_formula='rate = 4·Y·min(1, F/50)', rate_desc='Synthesis rate follows the yield and the energy supply.',
      heat='2 + 0.01*Fuv + 0.05*pos(Tr - 300)', idle_heat='1', rvars=[V('L', 'Genome length', 100), V('sg', 'Superiority σ', 10)], kappa=40)
WINDOWS['abiogenesis_crucible'] = {
    'spark_of_life': ({'L': 100, 'sg': 10}, [('Y', 0.5, 1)]),
    'panspermia_seed': ({'L': 1000, 'sg': 20}, [('Y', 0.7, 1)]),
}

model('evolution_engine', 'Evolution Engine',
      'Population genetics, run fast. A beneficial mutation fixes with Kimura\'s probability (1 − e^(−2s))/(1 − e^(−4Ns)). Too small a population and drift beats selection; too high a mutation rate and the genome melts (error catastrophe).',
      'Mutagen drift: µ·(1 ± 20%·d).',
      [P('lgN', 'Population size', 'log₁₀', 1, 9, 0.01, 4, 'Number of organisms, as a power of ten.'),
       P('s', 'Selection coefficient', '', 0.0001, 0.5, 0.0001, 0.01, 'Fitness advantage of the selected trait.'),
       P('lgmu', 'Mutation rate', 'log₁₀ /site', -10, -1, 0.01, -6, 'Mutations per site per generation.')],
      [D('N', 'Population', '', '10^lgN', 'N', 'Organisms.', show=False),
       D('U', 'Genomic mutation rate', '/gen', '10^lgmu*(1+0.2*d)*Lg', 'U = µL', 'Mutations per genome per generation.'),
       D('Pfix', 'Fixation probability', '', '(1 - exp(-2*s))/(1 - exp(-4*N*s))', 'P = (1−e^(−2s))/(1−e^(−4Ns))', 'Chance a new beneficial mutation takes over (Kimura).'),
       D('v', 'Adaptation rate', '/gen', '2*N*U*0.01*Pfix', 'v = 2NU·f_b·P_fix', 'Beneficial substitutions per generation.'),
       D('Ns', 'Selection strength Ns', '', 'N*s', 'N·s', 'Above 1, selection beats drift.')],
      [C('drift', 'stop', 'Ns - 1', 'Drift dominates: selection is blind at this population size', 'Ns > 1'),
       C('catastrophe', 'stop', 'ln(1 + 10*s) - U', 'Error catastrophe: mutations outpace selection', 'U < ln(1 + 10s)'),
       overheat(320, 'culture')],
      rate='clamp(v/vmin, 0, 4)', rate_formula='rate = v / v_needed', rate_desc='Evolution proceeds as fast as adaptations fix.',
      heat='2 + 1e-7*N', idle_heat='1', rvars=[V('Lg', 'Genome length', 1e6), V('vmin', 'Adaptation needed', 1e-3)], kappa=40)
WINDOWS['evolution_engine'] = {
    'evolved_xenobots': ({'Lg': 1e6, 'vmin': 1e-3}, [('v', 1e-3, 1e9)]),
    'telomerase_complex': ({'Lg': 3e6, 'vmin': 1e-2}, [('v', 1e-2, 1e9)]),
    'omnicellular_genome': ({'Lg': 1e8, 'vmin': 0.1}, [('v', 0.1, 1e9)]),
}

_md, _mc = magnet('B', 'Penning-trap solenoid')
model('mirror_foundry', 'Mirror Matter Foundry',
      'Penning traps: a solenoid field holds antiparticles radially, an electrostatic well axially. Stable only while ω_c² > 2ω_z². Antihydrogen forms by three-body recombination, ∝ n_e²·T^(−9/2) - cold positrons, dense ones; but a plasma cannot be denser than the Brillouin limit ε₀B²/2m_e.',
      'Positron heating: the plasma runs at T·(1 + 10%·|d|).',
      [P('B', 'Solenoid field', 'T', 0, 15, 0.01, 3, 'Axial magnetic field of the trap.'),
       P('Vt', 'Trap voltage', 'V', 0, 1000, 0.1, 50, 'Depth of the electrostatic well.'),
       P('lgne', 'Positron density', 'log₁₀ m⁻³', 10, 16, 0.01, 13, 'Density of the positron plasma.'),
       P('Prf', 'RF melt power', 'kW', 0, 100, 0.1, 0, 'Induction power melting the levitated anti-ore.')],
      [D('wc', 'Cyclotron frequency', 'rad/s', '9.58e7*B', 'ω_c = qB/m', 'Antiproton cyclotron frequency.'),
       D('wz', 'Axial frequency', 'rad/s', 'sqrt(9.58e7*Vt/(0.005^2))', 'ω_z = √(qV/(md²))', 'Axial bounce frequency (d = 5 mm).'),
       D('stab', 'Trap stability', '', '1 - 2*wz^2/(wc^2 + 1e-9)', '1 − 2ω_z²/ω_c²', 'Positive while the trap confines.'),
       D('Te', 'Positron temperature', 'K', 'T*(1 + 0.1*abs(d))', 'T_e', 'Temperature of the positron plasma.'),
       D('lgBr', 'Brillouin limit', 'log₁₀ m⁻³', 'log10(4.86e12*B^2 + 1)', 'n_B = ε₀B²/(2m_e)', 'Highest density the field can hold.'),
       D('lgrec', 'Recombination rate', 'log₁₀ /s', '-11.22 - 4.5*log10(Te/4.2) + 2*(lgne - 6)', 'Γ ∝ n_e²·T^(−9/2)', 'Three-body antihydrogen formation rate per antiproton.'),
       D('Tpool', 'Melt temperature', 'K', 'T + 40*Prf', 'T_pool', 'Temperature reached by the levitated anti-ore.')] + _md,
      [C('trap', 'stop', 'stab', 'Penning trap unstable (ω_c² < 2ω_z²)', 'ω_c² > 2ω_z²'),
       C('brillouin', 'stop', 'lgBr - lgne', 'Positron plasma above the Brillouin limit', 'n_e < ε₀B²/2m_e')] + _mc,
      rate='clamp((lgrec - 2)/1.5, 0, 4)', rate_formula='rate = (log Γ − 2)/1.5', rate_desc='Production follows the recombination rate.',
      heat='6 + 0.4*Prf + 0.3*B', idle_heat='2', quench_energy='60*B^2', rvars=[V('Tm', 'Melting point (K)', 0)], kappa=40, heat_capacity=40)
WINDOWS['mirror_foundry'] = {
    'antiiron_ingot': ({'Tm': 1811}, [('Tpool', 1811, 3000)]),
    'antigold_ingot': ({'Tm': 1337}, [('Tpool', 1337, 3000)]),
    'liquid_antihelium': ({}, [('Te', 0, 4.2), ('lgrec', 3, 100)]),
    'liquid_antihelium_slow': ({}, [('Te', 0, 4.2)]),
    'mirror_alloy_ingot': ({}, [('lgrec', 4, 100), ('Tpool', 1811, 4000)]),
    'mirror_processor': ({}, [('lgrec', 4, 100)]),
    'baryogenesis_seed': ({}, [('lgrec', 5, 100)]),
}

_md, _mc = magnet('Bsh', 'veto-shield magnet')
model('dark_matter_collector', 'Dark Matter Collector',
      'Dark matter streams through at ~230 km/s. A WIMP of mass m_χ hitting a nucleus of mass m_N can give it at most 2µ²v²/m_N of recoil - so match the target to the WIMP. Bolometers resolve ΔE ∝ T^(5/2): light dark matter needs millikelvin.',
      'Neutron background varies by ±30%·d.',
      [P('A', 'Target mass number', '', 1, 240, 1, 131, 'Nucleus used as target (O = 16, Ar = 40, Ge = 73, Xe = 131).'),
       P('Eth', 'Recoil threshold', 'keV', 0.01, 100, 0.01, 5, 'Lowest recoil energy counted.'),
       P('Md', 'Detector mass', 't', 0.001, 100, 0.001, 1, 'Mass of the target.'),
       P('Bsh', 'Shield field', 'T', 0, 15, 0.05, 1, 'Magnetic veto against charged backgrounds.')],
      [D('mN', 'Nucleus mass', 'GeV', '0.9315*A', 'm_N = 0.9315·A', 'Mass of a target nucleus.'),
       D('mu', 'Reduced mass', 'GeV', 'mx*mN/(mx + mN)', 'µ = m_χm_N/(m_χ + m_N)', 'Sets how much energy can be transferred.'),
       D('Emax', 'Maximum recoil', 'keV', '2*mu^2*(2.6e-3)^2/mN*1e6', 'E_max = 2µ²v_max²/m_N', 'Largest recoil a WIMP can give.'),
       D('E0', 'Typical recoil', 'keV', '2*mu^2*(7.74e-4)^2/mN*1e6', 'E₀ = 2µ²v₀²/m_N', 'Scale of the exponential recoil spectrum.'),
       D('S', 'Signal events', '/day', '0.02*Md*A^2*(mu/0.9)^2/mx*exp(-Eth/E0)', 'S ∝ M·A²µ²/m_χ·e^(−E/E₀)', 'Expected WIMP events (coherent spin-independent scattering).'),
       D('Bk', 'Background', '/day', '50*Md*exp(-Eth/3)*(1 + 0.3*d)/(1 + Bsh)', 'B ∝ M·e^(−E/3 keV)', 'Radioactive and neutron background.'),
       D('dE', 'Energy resolution', 'keV', '0.5*T^2.5', 'ΔE ∝ T^(5/2)', 'Thermal noise of the bolometers.'),
       D('snr', 'Significance', 'σ', 'S/sqrt(Bk + 1)', 'S/√B', 'Signal over background noise.')] + _md,
      [C('kinematic', 'stop', 'Emax - Eth', 'Threshold above the maximum recoil: nothing can be seen', 'E_th < E_max'),
       C('noise', 'stop', 'Eth - 5*dE', 'Threshold inside the thermal noise', 'E_th > 5ΔE')] + _mc,
      rate='clamp(snr/3, 0, 4)', rate_formula='rate = significance / 3σ', rate_desc='Collection follows the detection significance.',
      heat='3 + 0.2*Md', idle_heat='1', quench_energy='20*Bsh^2', rvars=[V('mx', 'WIMP mass (GeV)', 100)], kappa=40)
WINDOWS['dark_matter_collector'] = {
    'dark_matter': ({'mx': 100}, [('snr', 3, 1e9)]),
    'exotic_matter_crystal': ({'mx': 8}, [('snr', 3, 1e9)]),
    'nebular_gas': ({'mx': 1000}, []),
}

_md, _mc = magnet('B', 'magnetic nozzle')
model('annihilation_reactor', 'Annihilation Reactor',
      'Proton-antiproton annihilation turns mass entirely into pions: about half the energy in charged pions (p ≈ 0.35 GeV/c), a third in gamma rays, the rest in neutrinos. A magnetic nozzle can catch the charged pions if their gyroradius p/(0.3B) fits the 3 m chamber; the gammas must be stopped by shielding.',
      'Annihilation-point jitter: capture varies by ±5%·d.',
      [P('B', 'Nozzle field', 'T', 0, 15, 0.01, 2, 'Field of the magnetic nozzle.'),
       P('mdot', 'Antimatter feed', 'µg/s', 0, 100, 0.01, 1, 'Antimatter annihilated per second (with as much matter).'),
       P('sh', 'Gamma shield', 'cm', 0, 300, 1, 50, 'Tungsten shielding. Each 20 cm stops 63% of the gamma rays.')],
      [D('Ptot', 'Annihilation power', 'MW', '179.8*mdot', 'P = 2ṁc²', 'Total power released.'),
       D('rg', 'Pion gyroradius', 'm', '0.35/(0.2998*B + 1e-9)', 'r_g = p/(0.3B)', 'Radius charged pions circle on.'),
       D('fcap', 'Capture fraction', '', 'clamp(1 - rg/3, 0, 1)*(1 + 0.05*d)', 'f = 1 − r_g/R', 'Fraction of charged pions caught by the nozzle.'),
       D('Pel', 'Electric power', 'MW', '0.5*Ptot*fcap', 'P_e = ½P·f', 'Power converted from the captured pions.'),
       D('Pgam', 'Gamma power', 'MW', '0.33*Ptot', 'P_γ = ⅓P', 'Power in gamma rays.'),
       D('Pesc', 'Escaping gammas', 'MW', 'Pgam*exp(-sh/20)', 'P_γ·e^(−x/20 cm)', 'Gamma power getting through the shield.')] + _md,
      [C('radiation', 'damage', '5 - Pesc', 'Gamma flux through the shield is melting the structure', 'P_esc < 5 MW')] + _mc,
      rate='Pel/50', rate_formula='output = P_e / 50 MW', rate_desc='Generator output follows the captured power.',
      heat='4 + 0.3*Pgam*(1 - exp(-sh/20))', idle_heat='2', quench_energy='60*B^2', kappa=40)
WINDOWS['annihilation_reactor'] = {'annihilation_core': ({}, []), 'antimatter_pellet': ({}, [])}

model('dyson_array', 'Dyson Array',
      'Collectors around a star reach the temperature where they radiate what they absorb: 394 K/√(r/AU) for a flat absorber. They run heat engines into your coolant, so Carnot sets the efficiency: closer is hotter and better, until the collectors melt.',
      'Stellar flare activity: collector temperature varies by ±1%·d.',
      [P('r', 'Orbit radius', 'AU', 0.005, 5, 0.001, 1, 'Distance of the collector swarm from the star.'),
       P('cov', 'Pointing duty', '', 0, 1, 0.01, 1, 'Fraction of collectors facing the star.')],
      [D('Teq', 'Collector temperature', 'K', '394.1/sqrt(r)*(1+0.01*d)', 'T = (L/(4πr²σ))^(1/4)', 'Equilibrium temperature of a collector at this distance.'),
       D('flux', 'Stellar flux', 'W/m²', '1361/r^2', 'F = L/(4πr²)', 'Starlight intensity at this distance.'),
       D('eta', 'Carnot efficiency', '', 'pos(1 - T/Teq)', 'η = 1 − T_cold/T_hot', 'Efficiency of the heat engines between the collectors and your coolant.'),
       D('Pu', 'Useful flux', 'W/m²', 'flux*cov*eta', 'F·duty·η', 'Electric power per square metre of swarm.')],
      [C('melt', 'damage', '2000 - Teq', 'Collectors melting', 'T < 2000 K'), C('carnot', 'stop', 'eta - 0.01', 'Heat sink as hot as the collectors: no engine can run', 'T_cold < T_hot')],
      rate='Pu/(0.3*1361)', rate_formula='output = useful flux / (30% of 1 AU)', rate_desc='Generator output follows the useful flux.',
      heat='10 + 15*cov*(1 - eta)*min(flux/1361, 20)', idle_heat='2', kappa=200, heat_capacity=200)
WINDOWS['dyson_array'] = {'stellar_core': ({}, [])}


# =================================================================================== femtoscale windows (from energy)
def femto_windows():
    out = {}
    for f in sorted(glob.glob(os.path.join(RECIPE_DIR, 'femto_assembly', '*.json'))):
        r = json.load(open(f))
        e = max(65536, r.get('energy', 65536))
        sreq = 60 * 0.78 ** (math.log2(e) - 16)
        out[os.path.basename(f)[:-5]] = ({}, [('sig', 0, round(sreq, 2))])
    return out


# =================================================================================== JSON + lang + checks
LANG = {}


def model_json(mid):
    m = MODELS[mid]
    strip = lambda d: {k: v for k, v in d.items() if not k.startswith('_')}
    j = dict(params=[strip(p) for p in m['params']], recipe_vars={v[0]: v[2] for v in m['rvars']}, consts=m['consts'],
             states=[strip(s) for s in m['states']], derived=[strip(d) for d in m['derived']],
             constraints=[strip(c) for c in m['constraints']], rate=m['rate'], rate_formula=m['rate_formula'],
             heat=m['heat'], idle_heat=m['idle_heat'], quench_energy=m['quench_energy'], kappa=m['kappa'],
             heat_capacity=m['heat_capacity'], timescale=m['timescale'], rate_max=m['rate_max'])
    return j


def build_lang():
    pre = f'physics.{NS}'
    for mid, m in MODELS.items():
        b = f'{pre}.{mid}'
        LANG[f'{b}.title'] = m['title']
        LANG[f'{b}.disturb'] = m['disturb']
        LANG[f'{b}.rate.desc'] = m['rate_desc']
        for p in m['params']:
            LANG[f'{b}.p.{p["id"]}'] = p['_name']
            LANG[f'{b}.p.{p["id"]}.desc'] = p['_desc']
        for d in m['derived']:
            LANG[f'{b}.d.{d["id"]}'] = d['_name']
            LANG[f'{b}.d.{d["id"]}.desc'] = d['_desc']
        for s in m['states']:
            LANG[f'{b}.s.{s["id"]}'] = s['_name']
            LANG[f'{b}.s.{s["id"]}.desc'] = s['_desc']
        for c in m['constraints']:
            LANG[f'{b}.c.{c["id"]}'] = c['_name']
        for v in m['rvars']:
            LANG[f'{b}.v.{v[0]}'] = v[1]
    LANG.update({
        'status.theoreticalsciences.quenched': 'Quenched',
        'status.theoreticalsciences.unstable': 'Out of Spec',
        'status.theoreticalsciences.overheating': 'Overheating',
        'status.theoreticalsciences.out_of_window': 'Out of Window',
        'message.theoreticalsciences.quench': '%s quenched: %s',
        'message.theoreticalsciences.calibrated_physics': 'Recalibrated: field errors trimmed (disturbance was %s), containment restored.',
        'message.theoreticalsciences.calibrated_fault_physics': 'Containment rebuilt and field errors trimmed (disturbance was %s). The machine can be started again.',
        'gui.theoreticalsciences.phys.title': 'Operation',
        'gui.theoreticalsciences.phys.tab.controls': 'Controls',
        'gui.theoreticalsciences.phys.tab.readout': 'Readout',
        'gui.theoreticalsciences.phys.tab.plant': 'Plant',
        'gui.theoreticalsciences.phys.no_model': 'No physics model loaded.',
        'gui.theoreticalsciences.phys.step1': 'One step (Shift: ten)',
        'gui.theoreticalsciences.phys.step10': 'Ten steps (Shift: a hundred)',
        'gui.theoreticalsciences.phys.range': 'Range %s to %s %s, step %s',
        'gui.theoreticalsciences.phys.edit_hint': 'Click to type a value (1.5e3 works). Scroll to step; Shift ×10, Ctrl ×100.',
        'gui.theoreticalsciences.phys.derived': 'Quantities',
        'gui.theoreticalsciences.phys.limits': 'Limits',
        'gui.theoreticalsciences.phys.window_head': 'Recipe window',
        'gui.theoreticalsciences.phys.rate': 'Rate: ×%s',
        'gui.theoreticalsciences.phys.margin': 'Margin: %s',
        'gui.theoreticalsciences.phys.kind.stop': 'If broken: the machine will not run.',
        'gui.theoreticalsciences.phys.kind.wear': 'If broken: containment wears four times as fast.',
        'gui.theoreticalsciences.phys.kind.quench': 'If broken: a quench - stored energy dumps as heat, containment takes a hit, 8 s to recover.',
        'gui.theoreticalsciences.phys.kind.damage': 'If broken: containment loses integrity every second.',
        'gui.theoreticalsciences.phys.quenched': 'Quenched - recovering (%s s)',
        'gui.theoreticalsciences.phys.stop': 'Stopped: %s',
        'gui.theoreticalsciences.phys.state_range': 'Out of range: %s',
        'gui.theoreticalsciences.phys.quench_risk': 'Quench risk: %s',
        'gui.theoreticalsciences.phys.damage': 'Damage: %s',
        'gui.theoreticalsciences.phys.window': 'Outside the recipe window: %s',
        'gui.theoreticalsciences.phys.wear': 'Straining: %s',
        'gui.theoreticalsciences.phys.temperature': 'Cold mass: %s K',
        'gui.theoreticalsciences.phys.no_coolant': 'No coolant - shedding heat to air only',
        'gui.theoreticalsciences.phys.coolant_at': '%s at %s K',
        'gui.theoreticalsciences.phys.coil_tc': 'Coil tier %s: Tc %s K, Bc2(0) %s T',
        'gui.theoreticalsciences.phys.heat': 'Heat %s kW  Cooling %s kW',
        'gui.theoreticalsciences.phys.flow': 'Coolant use: %s mB/s',
        'gui.theoreticalsciences.phys.tanks': 'Coolant / spent',
        'gui.theoreticalsciences.phys.coolant_tank': 'Coolant',
        'gui.theoreticalsciences.phys.coolant_tank.hint': 'Pipe in any coolant, or click with a filled container',
        'gui.theoreticalsciences.phys.spent_tank': 'Spent coolant',
        'gui.theoreticalsciences.phys.spent_tank.hint': 'Drain it (e.g. warm helium back to the condenser) or the loop stops',
        'gui.theoreticalsciences.phys.coolant_props': 'Holds %s K, carries %s kJ/mB',
        'gui.theoreticalsciences.phys.disturbance': 'Disturbance d',
        'gui.theoreticalsciences.phys.integrity': 'Integrity %s%%  quenches: %s',
        'jei.theoreticalsciences.physics_title': 'Operating window',
        'jei.theoreticalsciences.physics_hint': 'Set the machine\'s parameters (Controls tab) so these hold.',
    })


def check_all(verbose=False):
    """Every recipe must have a working point at its coil tier with a coolant available by then."""
    import sys
    sys.argv = ['x']
    import chaincheck as cc
    W, FW = cc.WAVE, cc.FWAVE
    mdefs, sizes = {}, {}
    for f in glob.glob(os.path.join(ROOT, 'src/main/resources/assets/theoreticalsciences/multiblocks', '*.json')):
        d = json.load(open(f))
        used = set(''.join(''.join(r) for r in d['layers']))
        base = 0
        for ch, t in d.get('tiers', {}).items():
            if ch in used and t['family'] == 'coil': base = max(base, t['base'])
        mdefs[os.path.basename(f)[:-5]] = base
        if 'repeat' in d: sizes[os.path.basename(f)[:-5]] = 1 + d['repeat']['max']   # judged at its tallest build
    machines = {m['id']: m for m in json.load(open(os.path.join(HERE, 'machines.json')))}
    report, fails = [], []
    for mid, wins in WINDOWS.items():
        m = ps.Model(mid, model_json(mid))
        m.size = sizes.get(mid, 1)
        mw = W.get(f'{NS}:{mid}', 99)
        rtype = machines[mid]['recipe']
        for rid, (vars_, ws) in sorted(wins.items()):
            f = os.path.join(RECIPE_DIR, rtype, rid + '.json')
            if not os.path.exists(f):
                fails.append(f'{mid}/{rid}: recipe file missing')
                continue
            r = json.load(open(f))
            tier = max(r.get('min_tier', 0), mdefs.get(mid, 0))
            # wave at which the recipe can first run: the machine, and all its inputs
            wave = mw
            for x in r.get('item_inputs', []):
                if 'item' in x: wave = max(wave, W.get(x['item'], 0))
            for x in r.get('fluid_inputs', []):
                fl = x.get('fluid')
                if fl: wave = max(wave, FW.get(fl, 0))
            cools = [c for c in COOLANTS if c.startswith('minecraft') or FW.get(c, 99) <= wave]
            seen_t = set()
            cools = [c for c in sorted(cools, key=lambda c: -COOLANTS[c]['capacity']) if not (COOLANTS[c]['temperature'] in seen_t or seen_t.add(COOLANTS[c]['temperature']))]
            wl = [dict(q=q, min=lo, max=hi) for q, lo, hi in ws]
            ok = None
            for c in sorted(cools, key=lambda c: -COOLANTS[c]['temperature']):
                Tc = COOLANTS[c]['temperature']
                good = True
                sols = []
                prev = None
                for d in (0.0, 1.0, -1.0):
                    sol, s, e, T, rate = ps.solve(m, vars_, wl, Tc, tier, d=d, iters=700 if m.states else 3000, start=prev)
                    prev = sol
                    if s > 0:
                        good = False
                        break
                    sols.append((sol, T, rate))
                if good:
                    ok = (c, sols[0])
                    break
            if ok:
                report.append(f'{mid}/{rid}: OK T{tier} with {ok[0].split(":")[1]} (T={ok[1][1]:.3g} K, rate {ok[1][2]:.2f}) ' +
                              (' '.join(f'{k}={v:.4g}' for k, v in ok[1][0].items()) if verbose else ''))
            else:
                fails.append(f'{mid}/{rid}: NO working point at tier {tier} (wave {wave}, coolants {[c.split(":")[1] for c in cools]})')
    return report, fails


def apply(g):
    WINDOWS['femtoscale_assembly_line'] = femto_windows()
    build_lang()
    g['LANG'].update(LANG)


def finish(g):
    """Write the models, the coolant table, the recipe windows, and the Codex chapter."""
    w, q = g['w'], g['q']
    D = g['D']
    WINDOWS['femtoscale_assembly_line'] = femto_windows()
    for mid in MODELS:
        ps.Model(mid, model_json(mid))          # validates names and ordering
        w(os.path.join(D, 'ts_physics', mid + '.json'), model_json(mid))
    w(os.path.join(D, 'ts_physics', 'coolants.json'), {k: {kk: vv for kk, vv in v.items() if kk != 'name'} for k, v in COOLANTS.items()})
    machines = {m['id']: m for m in json.load(open(os.path.join(HERE, 'machines.json')))}
    n = 0
    for mid, wins in WINDOWS.items():
        rtype = machines[mid]['recipe']
        for rid, (vars_, ws) in wins.items():
            f = os.path.join(D, 'recipe', rtype, rid + '.json')
            if not os.path.exists(f): continue
            r = json.load(open(f))
            phys = {}
            if vars_: phys['vars'] = {k: float(v) for k, v in vars_.items()}
            if ws: phys['windows'] = [dict(q=qq, min=float(lo), max=float(hi)) for qq, lo, hi in ws]
            if phys:
                r['physics'] = phys
                w(f, r)
                n += 1
    print('physics windows written:', n, 'models:', len(MODELS))
    codex(g)


def codex(g):
    te = g['text_entry']
    te('operation', 'principles', 'Running a Multiblock', 'field_calibrator', [
        'Every multiblock is a real machine now. Its $(thing)Operation$() console (right of its screen) has three pages.$(br2)$(thing)Controls$(): the parameters you set - fields, voltages, currents, gaps. Click a value to type it, scroll to step it.',
        '$(thing)Readout$(): everything that follows from your settings, worked out with real equations. Hover any line for its formula. Below them, the $(thing)limits$() the machine must respect and the $(thing)recipe window$() it must be run inside.',
        'Each recipe states a window - a beam energy, a plasma temperature, a density, a significance. JEI shows it under the recipe. If the readout is outside it, the machine waits: $(thing)Out of Window$().',
        'Limits come in four kinds. Some simply stop the machine. Some make it $(thing)strain$() - containment wears four times as fast. Some $(thing)damage$() it every second. And some $(thing)quench$(): the stored energy dumps as heat in an instant, the containment takes a hit, and the machine needs eight seconds to recover. Enough of those and containment fails.',
        'The $(thing)disturbance$() d (Plant page) is what the machine fights: field errors, drifting phases, impurities, outgassing. It moves while the machine runs, and every model says what it does. A Field Calibrator trims it back to zero; an Autonomous Calibrator damps it.'],
        sort=0, spot=['field_calibrator'])
    te('operation', 'cooling', 'Coolant & Superconductors', 'superfluid_helium_bucket' if False else 'superconducting_coil_block', [
        'Every multiblock has a $(thing)coolant loop$(): pipe a coolant into it (or click its tank with a filled container). The coolant holds the cold mass near its own temperature; whatever heat the machine makes raises it by heat/κ.',
        'Coolants: Water and Ionic Liquid (300 K), Superfluid Helium and Liquid Antihelium (2 K), Neutrino Flux (10 K), Metallic Hydrogen and Antihydrogen (20 K), Dark Matter and Liquid Spacetime (3 K), and a Bose-Einstein Condensate at 10 mK that carries almost no heat.',
        'Superfluid Helium comes back out as $(fluid)Warm Helium$() in the spent tank: send it to a Condensing Chamber and back. If the spent tank fills, the loop stops - and without coolant a machine only sheds heat slowly to the air.',
        'Coils are superconductors with a critical temperature and field: Nb-Ti (tier 1) 9.2 K and 14.5 T, Nb₃Sn 18.3 K and 30 T, REBCO 92 K and 120 T, neutronium hydride 290 K and 400 T, transfinite lattice 4000 K. The usable field falls as B_c2(0)·(1 − (T/T_c)²): the colder, the stronger.',
        'Run a magnet above its critical field and it $(thing)quenches$(). Above 95% of T_c the interlock will not even let it start. So heat is not just a cost: every kilowatt raises the temperature, and with it lowers the field you can use.'],
        sort=1, spot=['superconducting_coil_block', 'chronosteel_coil_block', 'spacetime_alloy_coil_block'])
    for i, (mid, m) in enumerate(sorted(MODELS.items(), key=lambda kv: kv[1]['title'])):
        params = ', '.join(p['_name'] for p in m['params'])
        lim = '; '.join(c['_name'] for c in m['constraints'][:6])
        pages = [m['intro'],
                 f'$(thing)Set$(): {params}.$(br2)$(thing)Disturbance$(): {m["disturb"]}',
                 f'$(thing)Limits$(): {lim}.$(br2)$(thing)Rate$(): {m["rate_desc"]}']
        eqs = [f'{d["_name"]}: {d["formula"]}' for d in m['derived'] if d.get('show', True) and d['formula']][:7]
        if eqs: pages.append('$(thing)Equations$()$(br)' + '$(br)'.join(eqs))
        te('operation', mid, m['title'], mid, pages, sort=10 + i, spot=None)


CATEGORY = ('operation', 'Machine Operation', 'The physics of every multiblock: controls, limits, recipe windows and coolant.', 'field_calibrator', 18)

if __name__ == '__main__':
    import sys
    WINDOWS['femtoscale_assembly_line'] = femto_windows()
    only = sys.argv[1:] or None
    if only:
        keep = {k: v for k, v in WINDOWS.items() if k in only}
        WINDOWS.clear()
        WINDOWS.update(keep)
    rep, fails = check_all(verbose=True)
    print('\n'.join(rep))
    print('\nFAILS:' if fails else '\nall recipes have working points')
    print('\n'.join(fails))
