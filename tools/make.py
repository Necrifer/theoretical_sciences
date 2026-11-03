import numpy as np, math
from PIL import Image, ImageFilter
N=2048
y,x=np.mgrid[0:N,0:N].astype(np.float32)
u=(x-N/2)/(N/2); v=(y-N/2)/(N/2)
rng=np.random.default_rng(7)
img=np.zeros((N,N,3),np.float32)
# --- background: deep space with a violet/blue nebula
def noise(scale,seed):
    r=np.random.default_rng(seed); g=r.random((scale,scale)).astype(np.float32)
    im=Image.fromarray((g*255).astype(np.uint8)).resize((N,N),Image.BICUBIC).filter(ImageFilter.GaussianBlur(N/scale/2))
    return np.asarray(im,np.float32)/255
n1=noise(9,1); n2=noise(23,2); n3=noise(60,3)
neb=np.clip((n1*0.6+n2*0.3+n3*0.1-0.45)*2.2,0,1)
img+=neb[...,None]*np.array([0.20,0.07,0.32])*0.9
img+=np.clip((n2-0.5)*2,0,1)[...,None]*np.array([0.02,0.10,0.22])*0.8
img+=np.array([0.010,0.012,0.03])
# stars
for i in range(2600):
    sx,sy=rng.integers(0,N,2); b=rng.random()**6
    rr=1+int(b*3)
    c=np.array([0.8+0.2*rng.random(),0.85,1.0])*(0.3+1.5*b)
    img[max(0,sy-rr):sy+rr+1,max(0,sx-rr):sx+rr+1]+=c*0.5
R=np.sqrt(u*u+v*v); A=np.arctan2(v,u)
# --- accretion disk (tilted), its lensed far side arching over the shadow, photon ring
rs=0.20                       # shadow radius
tilt=0.22
def disk_mask(dy_sign):
    # disk plane: ellipse radii between 0.27 and 0.78, squashed by tilt
    rr=np.sqrt(u*u+((v)/tilt)**2)
    m=np.clip((rr-0.27)/0.05,0,1)*np.clip((0.80-rr)/0.25,0,1)
    return rr,m
rr,m=disk_mask(1)
# temperature: hotter inside; doppler: approaching side (left) brighter
heat=np.clip(1.25-rr*1.3,0,1)
dop=1.0+0.75*(-u/np.maximum(rr,1e-3))*np.clip(1-abs(v)/0.2,0,1)
turb=0.75+0.5*noise(140,9)*np.cos(A*7+rr*40)**2
col_hot=np.array([1.0,0.96,0.86]); col_mid=np.array([1.0,0.62,0.22]); col_out=np.array([0.75,0.20,0.08])
def ramp(h):
    h=h[...,None]
    return np.where(h>0.5, col_mid+(col_hot-col_mid)*(h-0.5)*2, col_out+(col_mid-col_out)*h*2)
disk=ramp(heat)*(m*dop*turb)[...,None]*1.5
fw=np.clip(v/0.012+0.5,0,1)[...,None]      # the near half crosses in front of the shadow (soft split)
# lensed far side: the back of the disk seen bent up over the hole, as a ring
lens_r=np.sqrt(u*u+v*v)
lens=np.clip(1-abs(lens_r-0.30)/0.06,0,1)**1.5*np.clip(-v/0.05+0.6,0,1)
lens+=np.clip(1-abs(lens_r-0.29)/0.03,0,1)**2*0.6*np.clip(v/0.05+0.4,0,1)  # thin underside image
lensc=ramp(np.full_like(u,0.75))*lens[...,None]*(1+0.5*np.clip(-u/0.3,-1,1))[...,None]
photon=np.exp(-((R-rs*1.06)/0.007)**2)[...,None]*np.array([1.0,0.92,0.75])*2.2
glow=np.exp(-((R-0.3)/0.22)**2)[...,None]*np.array([0.55,0.28,0.10])*0.5
img+=glow
img+= disk*(1-fw)
img+=lensc*1.4
# shadow
shadow=np.clip((R-rs)/0.008,0,1)[...,None]
img*=shadow
img+=photon
fa=np.clip(m*1.2,0,1)[...,None]*fw*0.85
img=img*(1-fa)+disk*fw
# --- high-tech HUD: segmented outer ring with ticks and a scanning arc, cyan
cyan=np.array([0.35,0.92,1.0])
def ring(r0,w,a=1.0):
    return np.exp(-((R-r0)/w)**2)*a
seg=(np.cos(A*36)>-0.2).astype(np.float32)
hud=ring(0.88,0.004)*seg*0.9 + ring(0.905,0.0025)*0.5
ticks=((np.cos(A*120)>0.93)&(R>0.92)&(R<0.95)).astype(np.float32)*0.8
major=((np.cos(A*12)>0.995)&(R>0.915)&(R<0.975)).astype(np.float32)
arc=ring(0.86,0.006)*np.clip(np.cos(A-2.35),0,1)**6*1.6
hud+=ticks+major+arc
# four corner brackets of a holographic frame
k=0.93; L=0.18; W=0.006
br=np.zeros_like(u)
for sx in (-1,1):
    for sy in (-1,1):
        br+=((abs(u-sx*k)<W)&(sy*v>k-L)&(sy*v<k+W)).astype(np.float32)
        br+=((abs(v-sy*k)<W)&(sx*u>k-L)&(sx*u<k+W)).astype(np.float32)
hud+=br*0.9
img+=hud[...,None]*cyan
hudim=Image.fromarray((np.clip(hud,0,1)*255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(10))
img+=(np.asarray(hudim,np.float32)/255)[...,None]*cyan*0.6
# vignette + tone map
img*=(1-0.45*np.clip(R-0.6,0,1)**1.5)[...,None]
img=1-np.exp(-img*1.35)
img=np.clip(img,0,1)**(1/1.05)
out=Image.fromarray((img*255).astype(np.uint8))
# bloom
bl=out.filter(ImageFilter.GaussianBlur(18))
out=Image.blend(out,Image.fromarray(np.clip(np.asarray(out,np.float32)+np.asarray(bl,np.float32)*0.35,0,255).astype(np.uint8)),1.0)
out.save('base.png')
print('ok')
