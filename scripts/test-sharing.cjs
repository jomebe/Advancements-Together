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
    if(p.channel==='c:register')c.write('custom_payload',{channel:'c:register',data:Buffer.concat([vi(1),str('play'),vi(1),str('shared_advancements:history_v2')])});
    if(p.channel==='shared_advancements:history_v2')c.records.push(decode(p.data));
  });
  c.on('ping',p=>c.write('pong',p));
  c.on('position',p=>c.write('teleport_confirm',{teleportId:p.teleportId}));
  c.on('error',e=>console.error(name,e));
  await until(()=>c.records.some(r=>r.kind==='mode'), name+' initial snapshot');
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
  command('sharedadvancements mode individual');
  command('advancement grant ModeA only minecraft:story/mine_stone');
  await checkVanilla('ModeA','minecraft:story/mine_stone',true,'INDIVIDUAL_OWNER');
  await checkVanilla('ModeB','minecraft:story/mine_stone',false,'INDIVIDUAL_OTHER_LOCKED');
  command('sharedadvancements mode cooperative');
  await checkVanilla('ModeB','minecraft:story/mine_stone',true,'COOP_HISTORICAL_CATCHUP');
  command('advancement grant ModeA only minecraft:story/smelt_iron');
  await checkVanilla('ModeB','minecraft:story/smelt_iron',true,'COOP_LIVE_UNLOCK');
  b.end();await delay(600);
  command('advancement grant ModeA only minecraft:story/obtain_armor');
  await checkVanilla('ModeA','minecraft:story/obtain_armor',true,'OFFLINE_OWNER');
  b=await join('ModeB');
  await checkVanilla('ModeB','minecraft:story/obtain_armor',true,'OFFLINE_REJOIN_CATCHUP');
  assert(b.records.find(r=>r.kind==='mode').id==='COOPERATIVE');
  command('sharedadvancements mode individual');
  command('advancement grant ModeA only minecraft:story/lava_bucket');
  await checkVanilla('ModeB','minecraft:story/lava_bucket',false,'SWITCH_BACK_STOPS_SHARING');
  await checkVanilla('ModeB','minecraft:story/smelt_iron',true,'SWITCH_BACK_KEEPS_UNLOCKS');
  command('sharedadvancements mode cooperative');
  await checkVanilla('ModeB','minecraft:story/lava_bucket',true,'RE_ENABLE_CATCHUP');
  const history=JSON.parse(fs.readFileSync(path.join(directory,'world/shared-advancements/history.json')));
  const stone=history.completions.filter(c=>c.advancement==='minecraft:story/mine_stone');
  assert.equal(stone.length,2);
  const owner=history.players.find(p=>p.name==='ModeA').id;
  assert.equal(stone[0].player,owner);
  assert(stone[0].time<stone[1].time);
  assert.equal(fs.readFileSync(path.join(directory,'world/shared-advancements/mode.txt'),'utf8'),'COOPERATIVE');
  fs.writeFileSync(path.join(directory,'sharing-test.json'),JSON.stringify({passed:true,checks:11,mode:'COOPERATIVE'},null,2));
  console.log('PASS first achiever retained and cooperative mode persisted');
})().catch(e=>{console.error(e);process.exitCode=1;}).finally(()=>{for(const b of bots)b.end();setTimeout(()=>process.exit(process.exitCode||0),250);});
