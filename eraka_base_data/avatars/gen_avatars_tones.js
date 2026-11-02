const {createAvatar}=require("@dicebear/core"); const {avataaars}=require("@dicebear/collection"); const fs=require("fs");
// Same 15 designs as gen3.js, each in three skin tones (22 Oct 2026): the avatar screen shows the
// tone chosen in the child profile. Mixed = the gen3.js set (ic_avatarNN).
const A=[
 [11,"theCaesar","2c1b18","shirtCrewNeck","65c9ff",0],[12,"froBand","2c1b18","shirtScoopNeck","ff488e",0],
 [13,"shortCurly","2c1b18","hoodie","25557c",0],[14,"bob","2c1b18","shirtCrewNeck","ffdeb5",0],
 [15,"shortFlat","2c1b18","shirtCrewNeck","ff5c5c",0],[21,"curly","2c1b18","overall","5199e4",0],
 [22,"theCaesar","2c1b18","shirtVNeck","a7ffc4",1],[23,"bun","4a312c","shirtCrewNeck","ffafb9",0],
 [24,"shortRound","724133","hoodie","929598",0],[25,"fro","2c1b18","shirtScoopNeck","ffffb1",0],
 [31,"shortCurly","2c1b18","shirtCrewNeck","3c4f5c",0],[32,"straight01","2c1b18","shirtCrewNeck","b1e2ff",0],
 [33,"curvy","2c1b18","overall","65c9ff",0],[34,"bigHair","2c1b18","hoodie","e6e6e6",1],
 [35,"theCaesar","2c1b18","shirtCrewNeck","2e9e5b",0],
];
const TONES={light:{skin:["ffdbb4","edb98a"],hair:["724133","b58143","2c1b18","d6b370","4a312c"]},
             medium:{skin:["d08b5b"],hair:null},
             dark:{skin:["614335","ae5d29"],hair:null}};
const out=[];
A.forEach(([n,t,hc,cl,cc,gl],i)=>{
  for (const [tone,T] of Object.entries(TONES)){
    const o={size:480,seed:"eraka"+n,top:[t],skinColor:[T.skin[i%T.skin.length]],hairColor:[T.hair?T.hair[i%T.hair.length]:hc],clothing:[cl],clothesColor:[cc],eyes:["happy"],mouth:["smile"],eyebrows:["defaultNatural"],facialHairProbability:0,topProbability:100,backgroundColor:["f0f0f0"],style:["circle"]};
    if(gl){o.accessories=["prescription02"]; o.accessoriesProbability=100; o.accessoriesColor=["262e33"];} else o.accessoriesProbability=0;
    const f=`v_${n}_${tone}`; fs.writeFileSync(f+".svg", createAvatar(avataaars,o).toString());
    fs.writeFileSync(f+".html",`<html><body style='margin:0;background:transparent'><img src='${f}.svg' width=480 height=480></body></html>`);
    out.push(f);
  }
});
fs.writeFileSync("v_list.txt",out.join("\n")); console.log(out.length);
