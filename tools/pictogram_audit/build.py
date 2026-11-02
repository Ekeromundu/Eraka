import json, os, io, sys
sys.path.insert(0, os.path.expanduser('~/audit'))
from flags import SAFETY, RENAME
from PIL import Image
from openpyxl import Workbook
from openpyxl.drawing.image import Image as XImg
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.worksheet.datavalidation import DataValidation
from collections import defaultdict
ROOT=os.path.expanduser('~/mnt/OTTAAProject - Copy - Copy')
A=ROOT+'/app/src/main/assets/eraka_seed/Archivos_Paises'
RES=ROOT+'/app/src/main/res/drawable-xxhdpi'
pictos=json.load(open(A+'/pictos/pictos_en.txt',encoding='utf-8-sig'))
es={p['id']:p['texto'].get('es','') for p in json.load(open(A+'/pictos/pictos_es.txt',encoding='utf-8-sig'))}
groups=json.load(open(A+'/grupos/grupos_en.txt',encoding='utf-8-sig'))
grp=defaultdict(list)
for g in groups:
    for r in g.get('relacion',[]):
        n=g['texto'].get('en','')
        if n!='ALL' and n not in grp[r['id']]: grp[r['id']].append(n)
P={p['id']:p for p in pictos if p['id']!=0}
TIPO={1:'person',2:'thing',3:'action',4:'describing',5:'social',6:'other'}
thumbs={}
def thumb(pid):
    if pid in thumbs: return thumbs[pid]
    name=P[pid]['imagen'].get('picto','')
    path=None
    for d in (RES, RES.replace('drawable-xxhdpi','drawable')):
        for ext in ('webp','png','jpg'):
            f=f'{d}/{name}.{ext}'
            if os.path.exists(f) and not path: path=f
    if not path: thumbs[pid]=None; return None
    im=Image.open(path).convert('RGBA'); im.thumbnail((60,60))
    bg=Image.new('RGB',im.size,'white'); bg.paste(im,mask=im.split()[3])
    b=io.BytesIO(); bg.save(b,'PNG'); thumbs[pid]=b.getvalue(); return thumbs[pid]
def addimg(ws,pid,cell):
    t=thumb(pid)
    if t: ws.add_image(XImg(io.BytesIO(t)),cell)
H=Font(bold=True,color='FFFFFF'); HF=PatternFill('solid',fgColor='2E8B6B')
thin=Side(style='thin',color='DDDDDD'); BR=Border(left=thin,right=thin,top=thin,bottom=thin)
WR=Alignment(wrap_text=True,vertical='center')
REC={'Remove':'F8D7DA','Review':'FFF3CD','Rename':'DDEBF7','Keep':'E2F0D9'}
def header(ws,cols,widths):
    ws.append(cols)
    for i,c in enumerate(ws[1]):
        c.font=H; c.fill=HF; c.alignment=WR
        ws.column_dimensions[c.column_letter].width=widths[i]
    ws.freeze_panes='A2'
wb=Workbook()
rd=wb.active; rd.title='Read me'
lines=["Eraka pictogram audit – 13 Oct 2026",
"",
"Why: the exploration slots (step 5a) can show any picture in the vocabulary, so pictures that are not suitable for children must be removed before any child uses the app.",
"Scope: the 585 pictures in Eraka's built-in vocabulary (OTTAA starter set, pictos_en.txt). Pictures parents add themselves (own photos) are not included.",
"",
"How to use:",
"1. 'Safety review': pictures flagged for content (sexual, alcohol, adult places, abuse, stigmatising words, medicine, danger). Choose a decision in column H for each row.",
"2. 'Wording fixes': labels with spelling mistakes or wrong translations from Spanish. These matter because the voice reads the label and the sentence builder uses it. Accept or change the suggestion.",
"3. 'Duplicates': the same English label on several pictures (e.g. two 'help'). Choose which one to keep.",
"4. 'All pictures': the full list with pictures, for a final look (and for the teachers). Change 'Keep' to 'Remove' for anything else you or the teachers want out.",
"",
"Decisions: Remove = hidden everywhere (tiles, suggestions, exploration, gallery). Keep. Rename = keep, change the label. Caregiver only = only in caregiver mode (option for sensitive but useful words, e.g. abuse, medicine).",
"Recommendations are Claude's first suggestion; final decisions are Keneke's (ideally checked with a Nexus Academy teacher). Send the file back when done and the app will be updated.",
]
for l in lines: rd.append([l])
rd['A1'].font=Font(bold=True,size=14); rd.column_dimensions['A'].width=130
for r in rd.iter_rows(min_row=2):
    r[0].alignment=Alignment(wrap_text=True)
dec=DataValidation(type='list',formula1='"Remove,Keep,Rename,Caregiver only,Review"',allow_blank=True)
# Safety
ws=wb.create_sheet('Safety review')
header(ws,['Picture','ID','English label','Spanish (original)','Groups','Concern','Reason','Recommendation','Your decision','Notes'],[10,6,16,18,26,18,44,26,16,30])
ws.add_data_validation(dec)
order={'Remove':0,'Rename':1,'Review':2,'Keep':3}
items=sorted(SAFETY.items(),key=lambda kv:(order.get(kv[1][2].split()[0].rstrip(':'),9),kv[0]))
for pid,(cat,why,rec) in items:
    p=P[pid]; ws.append(['',pid,p['texto'].get('en',''),es.get(pid,''),', '.join(grp[pid]),cat,why,rec,'',''])
    r=ws.max_row; ws.row_dimensions[r].height=48; addimg(ws,pid,f'A{r}')
    key=rec.split()[0].rstrip(':')
    for c in ws[r]:
        c.border=BR; c.alignment=WR
    ws[f'H{r}'].fill=PatternFill('solid',fgColor=REC.get(key,'FFFFFF'))
    dec.add(f'I{r}')
# Wording
ws=wb.create_sheet('Wording fixes')
header(ws,['Picture','ID','Current label','Spanish (original)','Suggested label','Why','Accept? (Y/N)','Your label (if different)'],[10,6,20,20,22,40,12,24])
yn=DataValidation(type='list',formula1='"Y,N"',allow_blank=True); ws.add_data_validation(yn)
for pid,(cur,new,why) in sorted(RENAME.items()):
    ws.append(['',pid,cur,es.get(pid,''),new,why,'',''])
    r=ws.max_row; ws.row_dimensions[r].height=48; addimg(ws,pid,f'A{r}')
    for c in ws[r]: c.border=BR; c.alignment=WR
    yn.add(f'G{r}')
# Duplicates
ws=wb.create_sheet('Duplicates')
header(ws,['Picture','ID','English label','Spanish (original)','Groups','Keep this one? (Y/N)'],[10,6,18,24,40,14])
yn2=DataValidation(type='list',formula1='"Y,N"',allow_blank=True); ws.add_data_validation(yn2)
by=defaultdict(list)
for pid,p in P.items():
    t=p['texto'].get('en','').strip().lower()
    if t: by[t].append(pid)
ndup=0
for t,ids in sorted(by.items()):
    if len(ids)<2: continue
    ndup+=1
    for pid in sorted(ids):
        ws.append(['',pid,P[pid]['texto'].get('en',''),es.get(pid,''),', '.join(grp[pid]),''])
        r=ws.max_row; ws.row_dimensions[r].height=48; addimg(ws,pid,f'A{r}')
        for c in ws[r]: c.border=BR; c.alignment=WR
        if ndup%2: 
            for c in ws[r][1:5]: c.fill=PatternFill('solid',fgColor='F3F3F3')
        yn2.add(f'F{r}')
# All
ws=wb.create_sheet('All pictures')
header(ws,['Picture','ID','English label','Spanish (original)','Word type','Groups','Flag','Decision'],[10,6,20,22,12,40,22,14])
dec2=DataValidation(type='list',formula1='"Keep,Remove,Rename,Caregiver only"',allow_blank=True); ws.add_data_validation(dec2)
dupset={pid for ids in by.values() if len(ids)>1 for pid in ids}
for pid in sorted(P):
    p=P[pid]
    flag='safety: '+SAFETY[pid][2] if pid in SAFETY else ('wording' if pid in RENAME else ('duplicate' if pid in dupset else ''))
    ws.append(['',pid,p['texto'].get('en',''),es.get(pid,''),TIPO.get(p.get('tipo'),''),', '.join(grp[pid]),flag,'Keep'])
    r=ws.max_row; ws.row_dimensions[r].height=48; addimg(ws,pid,f'A{r}')
    for c in ws[r]: c.border=BR; c.alignment=WR
    dec2.add(f'H{r}')
ws.auto_filter.ref=f'A1:H{ws.max_row}'
out=os.path.expanduser('~/mnt/Eraka/pictogram_audit'); os.makedirs(out,exist_ok=True)
wb.save(out+'/Eraka_pictogram_audit.xlsx')
miss=[pid for pid in P if thumb(pid) is None]
print('pictures',len(P),'safety',len(SAFETY),'wording',len(RENAME),'duplicate labels',ndup,'missing images',miss)
