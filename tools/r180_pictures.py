"""
1.8.0: the pictures of the Empyrean Assembly Table's recipes. Each is at most 15 by 15; a letter is a material's place in
that recipe (a is its first material, b its second, and so on), a space is an empty slot. Every recipe draws the thing
it makes.

Run:  python3 tools/r180_pictures.py <png>      (a sheet of all of them, to look at)
"""
PIC = {}


def pic(name, art):
    rows = art.strip('\n').split('\n')
    assert len(rows) <= 15, (name, len(rows))
    for r in rows:
        assert len(r.rstrip()) <= 15, (name, r, len(r.rstrip()))
    rows = [r.rstrip().ljust(15) for r in rows]
    top = (15 - len(rows)) // 2
    rows = [' ' * 15] * top + rows + [' ' * 15] * (15 - len(rows) - top)
    PIC[name] = rows


# ---------------------------------------------------------------- the elements
pic('remembered_soil', '''
       c
      ccc
     c c c
       c
       c
     aaaaa
   aabbbbbaa
  abbbbbbbbba
 abbdbbbbdbbba
 abbbbbdbbbbba
abbbbdbbbbdbbba
addddddddddddda
abbbbbbbbbbbbba
aeeeeeeeeeeeeea
aaaaaaaaaaaaaaa
''')
pic('living_water', '''
       a
       a
      aba
      aba
     abbba
     abcba
    abbcbba
    abcbbba
   abbcbbbba
   abcbbbbba
   abbbbbbba
   abbbbbdba
    abbbdba
     aabaa
      aaa
''')
pic('first_breath', '''
    aaaaaaa
  aa       aa
 a   bbbbb   a
a  bb     bb  a
a b   ccc   b a
a b  c   c  b a
a b c  e  c b
a b c ee  c b
a b  c   c  b
a  b  ccc  b
 a  bb    bb
  a   bbbb   d
   aa       dd
     aaaaaadd
           d
''')
pic('first_light', '''
a      a      a
 a     a     a
  b    a    b
   b       b
    b ccc b
     cdddc
    cddeddc
aaa cdeeedc aaa
    cddeddc
     cdddc
    b ccc b
   b       b
  b    a    b
 a     a     a
a      a      a
''')
pic('hearthfire', '''
       a
      aa
      aba
     abba  a
  a  abbba a
  aa abcbbaa
  abaabccbba
  abbbbccbba
 abbbcccccbba
 abbccdddccba
 abbcddeddcba
  abccdedccba
   abccdccba
 fffffffffffff
ff fff fff fff
''')
pic('seraphic_alloy', '''
c             c
cc           cc
ccc         ccc
 ccc       ccc
c ccc     ccc c
cc ccc   ccc cc
 cc cc   cc cc
  cc aaaaa cc
    abbbbba
   abbdddbba
  abbdeeedbba
  abbdddddbba
  abbbbbbbbba
  aaaaaaaaaaa
''')
pic('choir_crystal', '''
       a
      aba
     abcba
    abcccba
   abccdccba
   abcdddcba
   abcdedcba
   abcdddcba
   abcdddcba
   abccdccba
    abcccba
 a  abcccba  a
aba  abcba  aba
aba   aba   aba
 a     a     a
''')
pic('woven_memory', '''
ababababababa
bcbcbcbcbcbcb
acadacadacada
bcbcbcbcbcbcb
abababababababa
bcbdbcbebcbdbcb
acadacadacadaca
bcbebcbdbcbebcb
abababababababa
bcbdbcbebcbdbcb
acadacadacada
bcbcbcbcbcbcb
ababababababa
 c c c c c c
 c c c c c c
''')
pic('still_stone', '''
       e
      aaa
     abbba
     abbba
      aaa
    ccccccc
   cdddddddc
   cdddddddc
    ccccccc
  aaaaaaaaaaa
 abbbbbbbbbbba
abbbbbbbbbbbbba
abbbbbbbbbbbbba
 abbbbbbbbbbba
  aaaaaaaaaaa
''')
pic('morning_dew', '''
            a
      c    aa
     cdc  aba
     cdc aba
      c  aba
        abba
  c     aba
 cdc   abba
 cdc   aba
  c   abba  c
      aba  cdc
     abba  cdc
     aba    c
    abba
    aaa
''')
pic('evening_ember', '''
       a
      aba
     abcba
    abcdcba
   abcdddcba
  abcddeddcba
 abcddeeeddcba
abcddeeeeeddcba
 abcddeeeddcba
  abcddeddcba
   abcdddcba
    abcdcba
     abcba
      aba
       a
''')
pic('quiet_snow', '''
   b   a   b
    b  a  b
 b   b a b   b
  b   bab   b
   b   a   b
 bbbb  a  bbbb
     c a c
aaaaaaaeaaaaaaa
     c a c
 bbbb  a  bbbb
   b   a   b
  b   bab   b
 b   b a b   b
    b  a  b
   b   a   b
''')
pic('deep_root', '''
      aaa
      aba
      aba
      aba
     aabaa
    aa b aa
   aa  b  aa
  aa  aba  aa
 aa  aa a   aa
 a  aa  aa   a
a  aa    aa  a
c  a      a  c
  aa      aa
  c        c

''')
pic('bright_seed', '''
          cc
         cdc
        ccc
       c
      c
     aaa
    abbba
   abbcbba
   abcccba
   abcecba
   abcccba
   abbcbba
    abbba
     aaa

''')
pic('kind_shadow', '''
     aaaa
   aabbb
  abbba
 abbba       c
 abba       cdc
abbba        c
abba
abba     c
abbba
 abba
 abbba      a
  abbba   aab
   aabbbaabba
     aabbbaa
       aaa
''')
pic('warm_rain', '''
     aaaa
   aabbbba aa
  abbbbbbabbba
 abbbbbbbbbbbba
abbbbbbbbbbbbba
abbbbbbbbbbbbba
 aaaaaaaaaaaaa

  c   d   c   d
 c   d   c   d
c   d   c   d

  d   c   d   c
 d   c   d   c
d   c   d   c

''')
pic('salt_of_tears', '''
    aaaaaaaaa
   abbbbbbbaa
  abbbbbbbaca
 aaaaaaaaacca
 acccccccaccca
 acdcccdcaccca
 accccccccacca
 accceccccacca
 acccccccaccca
 acdcccdcacca
 acccccccaca
 aaaaaaaaaa
      d
      d
     ddd
''')
pic('honey_of_days', '''
    aaa aaa
   abbbabbba
   abcbabcba
 aaabbbabbbaaa
abbbaaaaaaabbba
abcbaddddabcba
abbbadeeedabbba
 aaaaddeddaaaa
abbbadeeedabbba
abcbaddddabcba
abbbaaaaaaabbba
 aaabbbabbbaaa
   abcbabcba
   abbbabbba
    aaa aaa
''')
pic('thread_of_fate', '''
   aaaaaaaaa
   abbbbbbba
    aaaaaaa
     c d c
    c  d  c
   c   d   c
    c  d  c
     c d c
      cdc
     c d c
    c  d  c
   c   d   c
    aaaaaaa
   abbbbbbba
   aaaaaaaaa
''')
pic('dust_of_stars', '''
c      a      d
       a
   d  aba   c
      aba
aaaaaabbbaaaaaa
 abbbbbcbbbbba
  abbbcccbbba
   abbcecbba
   abbcccbba
  abbbbabbbba
  abbba abbba
 abba  c  abba
 aba       aba
aa   d       aa
c          d
''')
pic('ash_of_worlds', '''
     aaaaa
   aabbbbbaa
  abbbbbbbbba
 abbbcbbbbbbba   
 abbbbbbbdbbba
abbbbbbbbbbbbba
abbcbbbbbbbbbba
eeeeeeeeeeeeeee
abbbbbbbbcbbbba
abbbbdbbbbbbbba
 abbbbbbbbbbba
 abbbbbbbbcbba
  abbbbbbbbba
   aabbbbbaa
     aaaaa
'''.replace('   \n', '\n'))
pic('word_unspoken', '''
  aaaaaaaaaaa
 abbbbbbbbbbba
abbbbbbbbbbbbba
abbbbbbbbbbbbba
abbccbbccbbccba
abbccbbccbbccba
abbbbbbbbbbbbba
abbbbbbbbbbbbba
 abbbbbbbbbbba
  aaaabbaaaaa
     abba
    abba
   abba
   aba
   aa
''')
pic('name_forgotten', '''
      aaa
      a a
  aaaaaaaaaaa
  abbbbbbbbba
  abcccccccba
  abbbbbbbbba
  abccccccbba
  abbbbbbbbba
  abdddddddba
  abdeeeeedba
  abdddddddba
  abbbbbbbbba
  abcccccbbba
  abbbbbbbbba
  aaaaaaaaaaa
''')
pic('song_unsung', '''
     aaaaaaaaa
     aaaaaaaaa
     a       a
     a       a
     acccccccc
     acccccccc
     a       a
     a       a
     a       a
  bbba    bbba
 bdddb   bdddb
bddedb  bddedb
bdddb   bdddb
 bbb     bbb

''')
# ---------------------------------------------------------------- the works
pic('firmament_plate', '''
     aaaaa
   aabbbbbaa
  abbbcbbbbba
 abbbbbbbdbbba
 abcbbbbbbbbba
abbbbbdbbbbcbba
abbbbbbbbbbbbba
abdbbbbcbbbbbba
abbbbbbbbbbdbba
abbbcbbbbbbbbba
aaaaaaaaaaaaaaa
eeeeeeeeeeeeeee
e  e  e  e  e e
e  e  e  e  e e
eeeeeeeeeeeeeee
''')
pic('foundation_stone', '''

   aaaaaaaaa
   abbbabbba
   aaaaaaaaa
  aabbbabbbaa
  aaaaaaaaaaa
 aabbabbbabbaa
 aaaaaaaaaaaaa
aabbbabbbabbbaa
aaaaaaaaaaaaaaa
acccccdcccccdca
acdcccccdccccca
aaaaaaaaaaaaaaa
eeeeeeeeeeeeeee

''')
pic('garden_bed', '''
  c    d    c
 cec  ded  cec
  c    d    c
  b    b    b
 bb    bb   bb
  b   bb    b
  b    b   bb
  b    b    b
aaaaaaaaaaaaaaa
afffffffffffffa
affbffffbffffba
afffffbffffbffa
afbfffffffbfffa
afffffffffffffa
aaaaaaaaaaaaaaa
''')
pic('wellspring', '''
       c
      c c
   c  c c  c
  c c  c  c c
  c  c c c  c
 c    ccc    c
 c     c     c
       c
  aaaaadaaaaa
 abbbbdddbbbba
abddddddddddbba
abbddddddddbbba
 abbbbbbbbbbba
  aaaaaaaaaaa
   eeeeeeeee
''')
pic('lantern_of_days', '''
      aaa
     a   a
      aaa
    aaaaaaa
   abbbbbbba
   a c   c a
   a  cdc  a
   a cdedc a
   a cdedc a
   a  cdc  a
   a   c   a
   abbbbbbba
    aaaaaaa
     bbbbb
    bbbbbbb
''')
pic('bell_of_hours', '''
      aaa
      aba
     aaaaa
    abbbbba
   abbcbbbba
   abcbbbbba
   abcbbbbba
  abbcbbbbbba
  abcbbbbbbba
  abbbbbbbbba
 abbbbbbbbbbba
aaaaaaaaaaaaaaa
      ddd
      ded
      ddd
''')
pic('loom_of_years', '''
aaaaaaaaaaaaaaa
ab c c c c c ba
ab c c c c c ba
abdddddddddddba
ab c c c c c ba
abeeeeeeeeeeeba
ab c c c c c ba
abdddddddddddba
ab c c c c c ba
abeeeeeeeeeeeba
ab c c c c c ba
ab c c c c c ba
aaaaaaaaaaaaaaa
aa           aa
aa           aa
''')
pic('gate_lintel', '''
aaaaaaaaaaaaaaa
abbbbbcdcbbbbba
aaaaaaaaaaaaaaa
 eee       eee
 aba       aba
 aba       aba
 aba       aba
 aba       aba
 aba       aba
 aba       aba
 aba       aba
 aba       aba
 eee       eee
eeeee     eeeee
eeeee     eeeee
''')
pic('threshold_step', '''
            ccc
            aaa
           aabb
         cccabb
         aaaabb
        aabbbbb
      cccabbbbb
      aaaabbbbb
     aabbbbbbbb
   cccabbbbbbbb
   aaaabbbbbbbb
  aabbbbbbbbbbb
cccabbbbbbbbbbb
aaaabbbbbbbbbbb
eeeeeeeeeeeeeee
''')
pic('roof_of_heaven', '''
       d
      ded
       a
      aba
     abcba
    abcccba
   abccdccba
  abccdddccba
 abccddeddccba
abccccccccccba
aaaaaaaaaaaaaaa
 f    f f    f
 f    f f    f
 f    f f    f
fff  fffff  fff
''')
pic('table_of_welcome', '''

     c   ddd
    ccc  ded
  d ccc  ddd  c
 ddd c      ccc
aaaaaaaaaaaaaaa
abbbbbbbbbbbbba
aaaaaaaaaaaaaaa
 aa         aa
 aa         aa
 aa         aa
 aa         aa
 aa         aa
 ee         ee

''')
pic('bed_of_rest', '''
aa
aba
aba
aba   ccccc
aba  cdddddc
aba   ccccc
abaeeeeeeeeeeee
abaebbbbbbbbbba
abaebbbbbbbbbba
abaaaaaaaaaaaaa
abbbbbbbbbbbbba
aaaaaaaaaaaaaaa
aa           aa
aa           aa

''')
# ---------------------------------------------------------------- the pillars
SHAFT = '''
    aaaaaaa
     abcba
     abcba
     abdba
     abcba
     abcba
    aaaaaaa
   eeeeeeeee
'''
pic('pillar_of_knowing', '''
    fffffff
  ff  ddd  ff
 f   deeed   f
  ff  ddd  ff
    fffffff

   aaaaaaaaa''' + SHAFT)
pic('pillar_of_making', '''
   fffffff
   fffffff
   fffffff
      d
      d
      d
   aaaaaaaaa''' + SHAFT)
pic('pillar_of_keeping', '''
     fffff
    f     f
    f     f
   ddddddddd
   ddddeddd d
   ddddddddd
   aaaaaaaaa''' + SHAFT)
pic('pillar_of_mercy', '''
   ff   ff
  fddf fddf
  fdddfdddf
   fdddddf
    fdddf
     fdf
   aaafaaaaa''' + SHAFT)
pic('pillar_of_return', '''
    fffffff
   f   d   f
   f  dd   f
   f ddddd f
   f  dd   f
   f   d   f
   aaaaaaaaa''' + SHAFT)
pic('keystone_of_return', '''
aaaaaaaaaaaaaaa
abbbbbbbbbbbbba
 abcccccccccba
 abcdddddddcba
 abcdeeeeedcba
  abcdefedcba
  abcdfffdcba
  abcdefedcba
  abcdeeedcba
   abcdedcba
   abcdddcba
   abcccccba
    abbbbba
    aaaaaaa
''')

# ---------------------------------------------------------------- 9 by 9 pictures, for the Extreme Assembly Table
P9 = {}
P9['empyrean_assembly_table'] = [
    'ABABABABA',
    'BCCCCCCCB',
    'ACDDDDDCA',
    'BCDEEEDCB',
    'ACDEFEDCA',
    'BCDEEEDCB',
    'ACDDDDDCA',
    'GG     GG',
    'GG     GG']
P9['offering_altar'] = [
    '    E    ',
    '   EFE   ',
    '    E    ',
    ' AAAAAAA ',
    ' ABBBBBA ',
    '  ACCCA  ',
    '  ACDCA  ',
    ' AACCCAA ',
    'AAAAAAAAA']

if __name__ == '__main__':
    import sys
    from PIL import Image
    COL = {'a': (240, 232, 216), 'b': (255, 216, 112), 'c': (128, 200, 255), 'd': (255, 144, 176), 'e': (160, 255, 176), 'f': (200, 160, 255)}
    names = list(PIC)
    cols, S = 8, 4
    W = Image.new('RGB', (cols * (15 * S + 8), ((len(names) + cols - 1) // cols) * (15 * S + 8)), (20, 16, 8))
    for i, n in enumerate(names):
        ox, oy = (i % cols) * (15 * S + 8) + 4, (i // cols) * (15 * S + 8) + 4
        for y, row in enumerate(PIC[n]):
            for x, ch in enumerate(row):
                c = COL.get(ch, (40, 32, 20))
                for dy in range(S - 1):
                    for dx in range(S - 1):
                        W.putpixel((ox + x * S + dx, oy + y * S + dy), c)
    W = W.resize((W.width * 2, W.height * 2), Image.NEAREST)
    W.save(sys.argv[1])
    print(len(names), 'pictures')
