import numpy as np, math, sys
LOGO=len(sys.argv)>1 and sys.argv[1]=='logo'
from PIL import Image, ImageFilter, ImageDraw
N=2048; C=N/2
import os; T=os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src/main/resources/assets/theoreticalsciences/textures') + '/'
base=Image.open('base.png').convert('RGB')
def frame0(path):
    im=Image.open(path).convert('RGBA'); w=im.size[0]; return im.crop((0,0,w,w))
# items spanning the tiers, early -> transcendence, spread around the orbit
order=['qubit','cherenkov_quartz','entangled_photon_pair','neutronium_ingot','antimatter_pellet','stellar_core','micro_singularity','tesseract_core','riemann_zeta_crystal','proof_of_everything','omnilex','omnipotent_ingot','transcendence_crystal']
# progression spirals inward: early tiers on the outer orbit, transcendence nearest the hole
if LOGO:
    order=['qubit','neutronium_ingot','antimatter_pellet','stellar_core','tesseract_core','proof_of_everything','omnipotent_ingot','transcendence_crystal']
items=[]
for n,k in enumerate(order):
    r=(0.71-0.008*n) if LOGO else (0.81-0.030*n); deg=(200+n*(-45 if LOGO else -31))%360
    items.append((k,r,deg))
TILT=0.62 if LOGO else 0.40; ROT=math.radians(-14)
def pos(r,deg):
    t=math.radians(deg); x=r*math.cos(t); y=r*math.sin(t)*TILT
    xr=x*math.cos(ROT)-y*math.sin(ROT); yr=x*math.sin(ROT)+y*math.cos(ROT)
    return C+xr*C, C+yr*C, math.sin(t)
glow=Image.new('RGB',(N,N)); gd=ImageDraw.Draw(glow); core=Image.new('RGB',(N,N)); cd=ImageDraw.Draw(core)
sprites=[]
for k,r,deg in items:
    sp=frame0(T+'item/'+k+'.png')
    a=np.asarray(sp,np.float32); m=a[...,3:]/255
    col=tuple(int(c) for c in np.clip((a[...,:3]*m).sum((0,1))/max(m.sum(),1)*1.25,40,255))
    # motion trail: arc behind the item (orbit runs counter-clockwise on screen)
    for i in range(110):
        d=deg+ i*0.42
        x,y,_=pos(r,d); f=(1-i/110)**1.3
        rad=(26 if LOGO else 16)*f+3
        gd.ellipse([x-rad,y-rad,x+rad,y+rad],fill=tuple(int(min(255,c*f*1.3)) for c in col))
        cr=rad*0.3
        cd.ellipse([x-cr,y-cr,x+cr,y+cr],fill=tuple(int(min(255,(c*0.4+150)*f)) for c in col))
    x,y,depth=pos(r,deg)
    sz=int((270 if LOGO else 176)*(1+0.28*depth))
    gd.ellipse([x-sz*0.55,y-sz*0.55,x+sz*0.55,y+sz*0.55],fill=tuple(int(c*0.35) for c in col))
    sprites.append((depth,x,y,sz,sp))
glow=glow.filter(ImageFilter.GaussianBlur(9))
out=np.clip(np.asarray(base,np.float32)+np.asarray(glow,np.float32)*1.2+np.asarray(core.filter(ImageFilter.GaussianBlur(2)),np.float32)*0.9,0,255)
out=Image.fromarray(out.astype(np.uint8)).convert('RGBA')
for depth,x,y,sz,sp in sorted(sprites,key=lambda s:s[0]):
    s=sp.resize((sz,sz),Image.NEAREST)
    # far-side items sit slightly dimmer
    if depth<0:
        arr=np.asarray(s,np.float32); arr[...,:3]*=0.75+0.25*(1+depth); s=Image.fromarray(arr.astype(np.uint8))
    out.alpha_composite(s,(int(x-sz/2),int(y-sz/2)))
# --- easter egg: a tiny isometric Omnipotence Core, barely lit, in the shadow
tex=np.asarray(frame0(T+'block/omnipotence_core.png').convert('RGB'),np.float32)
s=(64 if LOGO else 26); cx,cy=C+6,C+(4 if LOGO else 10)
o=np.asarray(out,np.float32)
yy,xx=np.mgrid[int(cy-s-2):int(cy+s+2),int(cx-s-2):int(cx+s+2)]
c30=0.866
faces=[((cx,cy-s),(s*c30,s*.5),(-s*c30,s*.5),1.0),
       ((cx-s*c30,cy-s*.5),(s*c30,s*.5),(0,s),0.62),
       ((cx,cy),(s*c30,-s*.5),(0,s),0.45)]
for (ox,oy),e1,e2,shade in faces:
    M=np.array([[e1[0],e2[0]],[e1[1],e2[1]]]); inv=np.linalg.inv(M)
    px=xx+0.5-ox; py=yy+0.5-oy
    i=inv[0,0]*px+inv[0,1]*py; j=inv[1,0]*px+inv[1,1]*py
    ok=(i>=0)&(i<1)&(j>=0)&(j<1)
    ti=np.clip((i*16).astype(int),0,15); tj=np.clip((j*16).astype(int),0,15)
    c=tex[tj,ti]*shade*(0.30 if LOGO else 0.22)
    sub=o[yy,xx,:3]; sub[ok]=c[ok]; o[yy,xx,:3]=sub
out=Image.fromarray(o.astype(np.uint8)).convert('RGB')
if LOGO:
    out.resize((256,256),Image.LANCZOS).save('theoreticalsciences_logo.png')
    out.resize((512,512),Image.LANCZOS).save('logo_preview_512.png')
else:
    out.resize((1024,1024),Image.LANCZOS).save('theoreticalsciences_thumbnail_1024.png')
    out.resize((400,400),Image.LANCZOS).save('theoreticalsciences_thumbnail_400.png')
out.crop((C-120,C-120,C+120,C+120)).resize((480,480),Image.NEAREST).save('egg_zoom.png')
print('ok')
