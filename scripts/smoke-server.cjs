const {spawn}=require('node:child_process');
const fs=require('node:fs');
const path=require('node:path');
const [java,directory]=process.argv.slice(2);
const cwd=path.resolve(directory);
const child=spawn(java,['-Xmx1G','-jar','server.jar','nogui'],{cwd,windowsHide:true});
let output='',ready=false;
const timeout=setTimeout(()=>{console.error('Server readiness timeout');child.kill();},180000);
function receive(data){
  output+=data;
  if(!ready && /Done \([\d.]+s\)!/.test(output)){
    ready=true;
    child.stdin.write('save-all\nstop\n');
  }
}
child.stdout.on('data',receive);child.stderr.on('data',receive);
child.on('error',e=>{console.error(e);clearTimeout(timeout);process.exitCode=1;});
child.on('close',code=>{
  clearTimeout(timeout);
  fs.writeFileSync(path.join(cwd,'smoke.log'),output);
  if(code!==0 || !ready || !output.includes('shared_advancements')){console.error(output.slice(-6000));process.exitCode=1;}
  else console.log('PASS: packaged mod loaded, dedicated server ready, clean save and stop');
});
