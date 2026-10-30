// Run against a disposable 1.21.1 server started by server-console.cjs on loopback:25575.
const mc = require('../.tools/bots/node_modules/minecraft-protocol');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const directory = path.resolve(process.argv[2]);
const bots = [];
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
async function until(check, message) {
  for (let i = 0; i < 200; i++) { if (check()) return; await delay(100); }
  throw Error('Timeout: ' + message);
}
function command(text) { fs.appendFileSync(path.join(directory, 'commands.txt'), text + '\n'); }
function vi(n) { const a=[]; do { let b=n&127;n>>>=7;a.push(b|(n?128:0)); } while(n);return Buffer.from(a); }
function str(s) { const b=Buffer.from(s);return Buffer.concat([vi(b.length),b]); }
function decode(b) { let n=0,s=0,i=0,v;do{v=b[i++];n|=(v&127)<<s;s+=7;}while(v&128);return JSON.parse(b.subarray(i,i+n)); }
async function join(name) {
  const c=mc.createClient({host:'127.0.0.1',port:25575,username:name,auth:'offline',version:'1.21.1'});
  c.records=[];bots.push(c);
  c.on('state',state=>{if(state==='configuration')c.write('custom_payload',{channel:'minecraft:register',data:Buffer.from('c:version\0c:register')});});
  c.on('custom_payload',p=>{
    if(p.channel==='c:version')c.write('custom_payload',{channel:'c:version',data:Buffer.from([1,1])});
    if(p.channel==='c:register')c.write('custom_payload',{channel:'c:register',data:Buffer.concat([vi(1),str('play'),vi(1),str('shared_advancements:history_v3')])});
    if(p.channel==='shared_advancements:history_v3')c.records.push(decode(p.data));
  });
  c.on('ping',p=>c.write('pong',p));
  c.on('position',p=>c.write('teleport_confirm',{teleportId:p.teleportId}));
  c.on('error',e=>console.error(name,e));
  await until(()=>c.records.some(r=>r.kind==='player'&&r.player.name===name), name+' initial snapshot');
  return c;
}
async function checkVanilla(name,id,done,label) {
  command(`execute ${done?'if':'unless'} entity @a[name=${name},advancements={${id}=true}] run say SA_TEST_${label}`);
  await until(()=>fs.readFileSync(path.join(directory,'console.log'),'utf8').includes('SA_TEST_'+label),label);
  console.log('PASS '+label);
}
(async()=>{
  await until(()=>fs.existsSync(path.join(directory,'console.log')) && /Done \([\d.]+s\)!/.test(fs.readFileSync(path.join(directory,'console.log'),'utf8')),'server ready');
  const a=await join('ModeA'); let b=await join('ModeB');
  command('advancement grant ModeA only minecraft:story/mine_stone');
  await checkVanilla('ModeA','minecraft:story/mine_stone',true,'OWNER');
  await checkVanilla('ModeB','minecraft:story/mine_stone',true,'AUTOMATIC_SHARING');
  b.end();await delay(600);
  command('advancement grant ModeA only minecraft:story/obtain_armor');
  await checkVanilla('ModeA','minecraft:story/obtain_armor',true,'OFFLINE_OWNER');
  b=await join('ModeB');
  await checkVanilla('ModeB','minecraft:story/obtain_armor',true,'OFFLINE_CATCHUP');
  command('give ModeB minecraft:diamond');
  await checkVanilla('ModeB','minecraft:story/mine_diamond',true,'INVENTORY_TRIGGER');
  await checkVanilla('ModeA','minecraft:story/mine_diamond',true,'REVERSE_SHARING');
  b.end();await delay(600);b=await join('ModeB');
  await checkVanilla('ModeB','minecraft:story/mine_stone',true,'REJOIN');
  const history=JSON.parse(fs.readFileSync(path.join(directory,'world/shared-advancements/history.json')));
  for(const [id,name] of [['mine_stone','ModeA'],['obtain_armor','ModeA'],['mine_diamond','ModeB']]){
    const records=history.completions.filter(c=>c.advancement==='minecraft:story/'+id);
    assert.equal(records.length,1,'Shared recipients must never become achievers: '+id);
    assert.equal(records[0].player,history.players.find(p=>p.name===name).id);
    assert.equal(records[0].verified,true);
  }
  assert.equal(history.schema,2);
  assert(!fs.existsSync(path.join(directory,'world/shared-advancements/mode.txt')));
  fs.writeFileSync(path.join(directory,'sharing-test.json'),JSON.stringify({passed:true,checks:12,alwaysShared:true,credits:'actual achievers only'},null,2));
  console.log('PASS source attribution, no duplicate imported recipients, schema 2, no mode setting');
})().catch(e=>{console.error(e);process.exitCode=1;}).finally(()=>{for(const b of bots)b.end();setTimeout(()=>process.exit(process.exitCode||0),250);});
