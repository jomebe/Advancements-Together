// Test helper: run a local server with a file-backed console and capture its log.
const {spawn}=require('node:child_process');
const fs=require('node:fs');
const path=require('node:path');
const [java, directory]=process.argv.slice(2);
const cwd=path.resolve(directory);
const commands=path.join(cwd,'commands.txt');
fs.writeFileSync(commands,'');
const log=fs.createWriteStream(path.join(cwd,'console.log'));
const child=spawn(java,['-Xmx1G','-jar','server.jar','nogui'],{cwd,windowsHide:true,stdio:['pipe','pipe','pipe']});
child.stdout.pipe(log);child.stderr.pipe(log);
let consumed=0;
const timer=setInterval(()=>{
  const lines=fs.readFileSync(commands,'utf8').split('\n');
  while(consumed<lines.length-1)child.stdin.write(lines[consumed++].trim()+'\n');
},250);
child.on('exit',code=>{clearInterval(timer);log.end();console.log('Server exit',code);process.exitCode=code;});
child.on('error',error=>{clearInterval(timer);console.error(error);process.exitCode=1;});
