const {createAvatar}=require("@dicebear/core"); const {avataaars}=require("@dicebear/collection"); const fs=require("fs");
// slot, top, skin, hairColor, clothing, clothesColor, glasses?
const A=[
 [11,"theCaesar","614335","2c1b18","shirtCrewNeck","65c9ff",0],
 [12,"froBand","614335","2c1b18","shirtScoopNeck","ff488e",0],
 [13,"shortCurly","ae5d29","2c1b18","hoodie","25557c",0],
 [14,"bob","ae5d29","2c1b18","shirtCrewNeck","ffdeb5",0],
 [15,"shortFlat","d08b5b","2c1b18","shirtCrewNeck","ff5c5c",0],
 [21,"curly","614335","2c1b18","overall","5199e4",0],
 [22,"theCaesar","ae5d29","2c1b18","shirtVNeck","a7ffc4",1],
 [23,"bun","d08b5b","4a312c","shirtCrewNeck","ffafb9",0],
 [24,"shortRound","edb98a","724133","hoodie","929598",0],
 [25,"fro","ae5d29","2c1b18","shirtScoopNeck","ffffb1",0],
 [31,"shortCurly","614335","2c1b18","shirtCrewNeck","3c4f5c",0],
 [32,"straight01","ffdbb4","b58143","shirtCrewNeck","b1e2ff",0],
 [33,"curvy","d08b5b","2c1b18","overall","65c9ff",0],
 [34,"bigHair","614335","2c1b18","hoodie","e6e6e6",1],
 [35,"theCaesar","614335","2c1b18","shirtCrewNeck","2e9e5b",0],
];
for (const [n,t,s,hc,cl,cc,gl] of A){
  const o={size:480,seed:"eraka"+n,top:[t],skinColor:[s],hairColor:[hc],clothing:[cl],clothesColor:[cc],eyes:["happy"],mouth:["smile"],eyebrows:["defaultNatural"],facialHairProbability:0,topProbability:100,backgroundColor:["f0f0f0"],style:["circle"]};
  if(gl){o.accessories=["prescription02"]; o.accessoriesProbability=100; o.accessoriesColor=["262e33"];} else o.accessoriesProbability=0;
  fs.writeFileSync(`slot_${n}.svg`, createAvatar(avataaars,o).toString());
}
console.log("ok");
