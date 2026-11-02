import numpy as np
S=np.array([0xF5,0xE5,0xDE],float); s=S.sum()
def masks(a):
    rgb=a[...,:3]; al=a[...,3]>0; tot=rgb.sum(-1); k=tot/s; tol=7*np.maximum(k,0.5)
    m1=(k>0.2)&(k<=1.03)&np.all(np.abs(rgb-k[...,None]*S)<=tol[...,None],-1)&al
    u=(tot-s)/(765-s); m2=(u>0)&(u<0.85)&np.all(np.abs(rgb-(S+u[...,None]*(255-S)))<=7,-1)&al&~m1
    core=m1&(k>=0.85)
    near=core.copy()
    for _ in range(2):
        n=near.copy()
        n[1:]|=near[:-1]; n[:-1]|=near[1:]; n[:,1:]|=near[:,:-1]; n[:,:-1]|=near[:,1:]
        near=n
    change=core|((m1|m2)&near)
    return core,change,k,u,m1
