const {execFileSync}=require('node:child_process');
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const adb=path.join(process.env.USERPROFILE,'AppData/Local/Android/Sdk/platform-tools/adb.exe');
const serial=process.argv[2]; assert.ok(serial,'Supply the test device serial');
const app='com.martincornelli.agendasacramental.design.qa';
const run=(...args)=>execFileSync(adb,['-s',serial,...args],{encoding:'utf8',timeout:30000});
const delay=ms=>new Promise(resolve=>setTimeout(resolve,ms));
const dump=()=>{run('shell','uiautomator','dump','/sdcard/hymn-topics-qa.xml');return run('shell','cat','/sdcard/hymn-topics-qa.xml');};
function bounds(xml,text){const node=xml.match(new RegExp('<node[^>]*text="'+text+'"[^>]*>'))?.[0];assert.ok(node,'Missing '+text);return node.match(/bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/).slice(1).map(Number);}
function tap(xml,text){const [x,y,r,b]=bounds(xml,text);run('shell','input','tap',String((x+r)>>1),String((y+b)>>1));}
(async()=>{
 fs.mkdirSync('output/hymn-topics-phone',{recursive:true});
 run('install','-r','app/build/outputs/apk/qa/app-qa.apk');
 for(const language of ['es','en']) for(const large of [false,true]) {
  run('shell','am','force-stop',app);
  run('shell','am','start','-W','-n',app+'/com.example.agendasacramental.HymnTopicsFixture','--es','language',language,'--ez','dark',String(large),'--ez','large',String(large),'--ez','landscape',String(large));
  await delay(1200);
  let xml=dump();assert.ok(xml.includes('Refresh count: 0'));
  // Pull down from the first item of the actual LazyColumn.
  const [x,y,r,b]=bounds(xml,'Refresh count: 0');
  run('shell','input','swipe',String((x+r)>>1),String(b+25),String((x+r)>>1),String(b+480),'500');
  await delay(2100);xml=dump();assert.ok(xml.includes('Refresh count: 1'),'Pull-to-refresh did not complete');
  tap(xml,language==='en'?'Refresh':'Actualizar');
  await delay(2100);xml=dump();assert.ok(xml.includes('Refresh count: 2'),'Accessible button did not refresh');
  tap(xml,language==='en'?'Edit topics':'Editar temas');
  await delay(500);xml=dump();assert.ok(xml.includes(language==='en'?'Topics (one per line)':'Temas (uno por línea)'));
  fs.writeFileSync('output/hymn-topics-phone/'+language+'-'+large+'.xml',xml);
  const png=execFileSync(adb,['-s',serial,'exec-out','screencap','-p'],{timeout:30000});
  fs.writeFileSync('output/hymn-topics-phone/'+language+'-'+large+'.png',png);
  tap(xml,language==='en'?'Cancel':'Cancelar');
  assert.ok(dump().includes('Refresh count: 2'));
 }
 console.log('Passed actual Android pull gesture, accessible refresh, editor/cancel, both languages, dark/large landscape.');
})().catch(e=>{console.error(e);process.exitCode=1;});
