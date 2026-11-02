# Namibian landscape strip (360x80): sun, dunes, savanna ground, quiver tree, camelthorn tree.
C={'green':'#4E9F5F','green_dark':'#357A45','green_soft':'#A8D8AE','mustard':'#E9A92A','mustard_dark':'#B97F10','mustard_light':'#FFE39A'}
R={'green':'@color/eraka_green','green_dark':'@color/eraka_green_dark','green_soft':'@color/eraka_green_soft','mustard':'@color/eraka_mustard','mustard_dark':'@color/eraka_mustard_dark','mustard_light':'@color/eraka_mustard_light'}
els=[]
def fill(c,d,a=1.0,cm=''): els.append(('f',c,d,a,cm))
def stroke(c,d,w,cm=''): els.append(('s',c,d,w,cm))
fill('mustard','M286,22 a13,13 0 1,0 26,0 a13,13 0 1,0 -26,0 Z',0.9,'sun')
fill('mustard_light','M0,80 L0,50 C30,40 60,34 100,46 C140,30 180,26 220,42 C255,30 300,26 360,40 L360,80 Z',1.0,'far dunes')
fill('mustard','M0,80 L0,58 C40,48 80,46 120,56 C170,42 210,44 250,54 C290,46 330,46 360,52 L360,80 Z',0.85,'near dunes')
fill('green_soft','M0,80 L0,66 C60,58 120,70 180,64 C240,58 300,70 360,62 L360,80 Z',1.0,'savanna grass')
fill('green','M0,80 L0,72 C70,66 130,76 200,70 C260,65 310,74 360,69 L360,80 Z',1.0,'front grass')
# quiver tree (kokerboom)
fill('green_dark','M64,68 L67,44 L71,44 L74,68 Z',1.0,'quiver tree trunk')
for d in ['M69,45 L60,35','M69,45 L78,35','M60,35 L55,28','M60,35 L64,27','M78,35 L74,27','M78,35 L83,28']:
    stroke('green_dark',d,2.4)
for x,y in [(55,27),(64,26),(74,26),(83,27)]:
    fill('green_dark',f'M{x-5},{y} a5,3.5 0 1,0 10,0 a5,3.5 0 1,0 -10,0 Z',1.0,'')
# camelthorn tree (umbrella acacia)
fill('green_dark','M247,70 L249,48 L252,48 L254,70 Z',1.0,'camelthorn trunk')
for d in ['M250,49 L240,40','M251,49 L262,40','M250,52 L246,44']: stroke('green_dark',d,2.0)
fill('green','M224,40 C226,31 250,28 262,30 C274,30 282,34 280,39 C270,43 232,44 224,40 Z',1.0,'camelthorn crown')
# small bush
fill('green_dark','M150,66 a6,4 0 1,0 12,0 a6,4 0 1,0 -12,0 Z',1.0,'bush')
fill('green_dark','M320,66 a5,3 0 1,0 10,0 a5,3 0 1,0 -10,0 Z',1.0,'')
# SVG preview
svg=['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 360 80" width="1080" height="240" style="background:#FFF1C2">']
xml=['<?xml version="1.0" encoding="utf-8"?>','<!-- Bottom strip of the onboarding screens (24 Oct 2026): a Namibian landscape - sun, dunes,','     savanna grass, a quiver tree (kokerboom) and a camelthorn tree. Was waves with bubbles.','     File name kept as bg_eraka_grass so every screen picks it up. Drawn at 360x80. -->',
 '<vector xmlns:android="http://schemas.android.com/apk/res/android"','    android:width="360dp"','    android:height="80dp"','    android:viewportWidth="360"','    android:viewportHeight="80">']
for e in els:
    if e[0]=='f':
        _,c,d,a,cm=e
        svg.append(f'<path fill="{C[c]}" fill-opacity="{a}" d="{d}"/>')
        if cm: xml.append(f'    <!-- {cm} -->')
        xml.append(f'    <path android:fillColor="{R[c]}" android:fillAlpha="{a}"\n        android:pathData="{d}"/>')
    else:
        _,c,d,w,cm=e
        svg.append(f'<path fill="none" stroke="{C[c]}" stroke-width="{w}" stroke-linecap="round" d="{d}"/>')
        xml.append(f'    <path android:strokeColor="{R[c]}" android:strokeWidth="{w}" android:strokeLineCap="round"\n        android:pathData="{d}"/>')
svg.append('</svg>'); xml.append('</vector>')
open('strip.svg','w').write('\n'.join(svg)); open('bg_eraka_grass.xml','w').write('\n'.join(xml)+'\n')
