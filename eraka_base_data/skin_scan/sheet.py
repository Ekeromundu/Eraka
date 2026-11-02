import os,sys,numpy as np
from PIL import Image,ImageDraw
R=os.path.expanduser("~/mnt/My Projects--Eraka/app/src/main/res/drawable-xxhdpi")
S=np.array([0xF5,0xE5,0xDE],float); s=S.sum(); T=np.array([0xA6,0x5C,0x17],float)
def rec(im):
    a=np.asarray(im.convert("RGBA")).astype(float); rgb=a[...,:3]; tot=rgb.sum(-1); k=tot/s; tol=7*np.maximum(k,0.5)
    c1=(k>0.2)&(k<=1.03)&np.all(np.abs(rgb-k[...,None]*S)<=tol[...,None],-1)
    u=(tot-s)/(765-s); c2=(u>0)&(u<0.85)&np.all(np.abs(rgb-(S+u[...,None]*(255-S)))<=7,-1)&~c1
    o=rgb.copy(); o[c1]=k[c1][:,None]*T; o[c2]=T+u[c2][:,None]*(255-T)
    a[...,:3]=np.clip(o,0,255); return Image.fromarray(a.astype('uint8'))
names=sys.argv[2].split(','); cell=150; cols=4
sheet=Image.new("RGB",(cols*2*cell,((len(names)+cols-1)//cols)*(cell+16)),"white"); d=ImageDraw.Draw(sheet)
for i,n in enumerate(names):
    f=[x for x in os.listdir(R) if x.rsplit('.',1)[0]==n]
    if not f: continue
    im=Image.open(os.path.join(R,f[0])).convert("RGBA"); bg=Image.new("RGBA",im.size,"white"); im=Image.alpha_composite(bg,im)
    r=rec(im); x=(i%cols)*2*cell; y=(i//cols)*(cell+16)
    sheet.paste(im.convert("RGB").resize((cell,cell)),(x,y)); sheet.paste(r.convert("RGB").resize((cell,cell)),(x+cell,y))
    d.text((x+4,y+cell+2),n,fill="black")
out=os.path.expanduser("~/mnt/My Projects--Eraka/_backup_login_activity_2/"+sys.argv[1]); sheet.save(out); print(out)
