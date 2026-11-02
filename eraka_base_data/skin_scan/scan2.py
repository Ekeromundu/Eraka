import os,json,numpy as np
from PIL import Image
from rule2 import masks
R=os.path.expanduser("~/mnt/My Projects--Eraka/app/src/main/res/drawable-xxhdpi")
out={}
for f in sorted(os.listdir(R)):
    if not f.endswith(('.webp','.png','.jpg')): continue
    name=f.rsplit('.',1)[0]
    if name.startswith('eraka_') and not name.startswith('eraka_d_') and name not in('eraka_hurt','eraka_laundry'): continue
    if name.startswith('ic_avatar'): continue
    try:
        im=Image.open(os.path.join(R,f)).convert("RGBA"); w,h=im.size; im=im.resize((w//2,h//2),Image.BILINEAR)
        core,change,_,_,_=masks(np.asarray(im).astype(float))
    except Exception: continue
    W,H=im.size
    if core.sum()>=max(8,W*H//8000): out[name]=[int(core.sum()),round(100*core.sum()/(W*H),2)]
json.dump(out,open("hits2.json","w")); print(len(out))
