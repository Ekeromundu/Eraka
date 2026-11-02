const {createAvatar}=require("@dicebear/core"); const {avataaars}=require("@dicebear/collection"); const fs=require("fs");
// login-screen children (24 Oct 2026; 27 Oct: a light-skinned girl (32) instead of 25): same designs as the avatars, without the grey circle
const A={23:["bun","d08b5b","4a312c","shirtCrewNeck","ffafb9"],35:["theCaesar","614335","2c1b18","shirtCrewNeck","2e9e5b"],
 12:["froBand","614335","2c1b18","shirtScoopNeck","ff488e"],24:["shortRound","edb98a","724133","hoodie","929598"],32:["straight01","ffdbb4","b58143","shirtCrewNeck","b1e2ff"]};
for (const [n,[t,s,hc,cl,cc]] of Object.entries(A)){
 const o={size:400,seed:"eraka"+n,top:[t],skinColor:[s],hairColor:[hc],clothing:[cl],clothesColor:[cc],eyes:["happy"],mouth:["smile"],eyebrows:["defaultNatural"],facialHairProbability:0,topProbability:100,accessoriesProbability:0};
 fs.writeFileSync(`row_${n}.svg`,createAvatar(avataaars,o).toString());
 fs.writeFileSync(`row_${n}.html`,`<html><body style='margin:0;background:transparent'><img src='row_${n}.svg' width=400 height=400></body></html>`);
}
