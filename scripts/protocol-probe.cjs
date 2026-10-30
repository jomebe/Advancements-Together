// Local-only integration probe; dependency installed in .tools/bots, never shipped.
const mc = require('../.tools/bots/node_modules/minecraft-protocol');
const fs = require('node:fs');
const out = process.argv[2] || '.tools/probe.json';
const results = {};
function varint(n) { const a=[]; do { let b=n&127; n>>>=7; a.push(b|(n?128:0)); } while(n); return Buffer.from(a); }
function str(s) { const b=Buffer.from(s); return Buffer.concat([varint(b.length),b]); }
function readString(b) { let n=0,shift=0,i=0,v; do { v=b[i++];n|=(v&127)<<shift;shift+=7; } while(v&128); return b.subarray(i,i+n).toString(); }
function save() { fs.writeFileSync(out, JSON.stringify(results,null,2)); }
for (const username of ['SharedTestA','SharedTestB']) {
  const r=results[username]={messages:[],errors:[]};
  const c=mc.createClient({host:'127.0.0.1',port:25575,username,auth:'offline',version:'1.21.1'});
  c.on('state', state => {
    if(state==='configuration') c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from('c:version\0c:register')});
  });
  c.on('custom_payload',p=>{
    if(p.channel==='c:version') c.write('custom_payload',{channel:'c:version',data:Buffer.from([1,1])});
    if(p.channel==='c:register') c.write('custom_payload',{channel:'c:register',data:Buffer.concat([varint(1),str('play'),varint(1),str('shared_advancements:history_v3')])});
  });  c.on('ping', p=>c.write('pong',p));
  c.on('position', p => { if(p.teleportId!==undefined)c.write('teleport_confirm',{teleportId:p.teleportId}); });
  c.on('playerJoin',()=>{r.joined=true;save();console.log(username+' joined');});
  c.on('custom_payload',p=>{if(p.channel==='shared_advancements:history_v3'){r.messages.push(JSON.parse(readString(p.data)));save();}});
  c.on('error',e=>{r.errors.push(String(e));save();});
  c.on('kick_disconnect',p=>{r.errors.push(p);save();});
  c.on('end',reason=>{r.ended=reason;save();});
}
setTimeout(()=>process.exit(0),600000);
