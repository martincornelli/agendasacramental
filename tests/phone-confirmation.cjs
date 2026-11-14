const fs=require('node:fs'),path=require('node:path'),{execFileSync}=require('node:child_process');
const adb='C:/Users/marti/AppData/Local/Android/Sdk/platform-tools/adb.exe';
const serial='bd36348a', app='com.martincornelli.agendasacramental.design.qa';
const out='output/confirmation-phone';fs.mkdirSync(out,{recursive:true});
const run=(...args)=>execFileSync(adb,['-s',serial,...args],{timeout:30000,maxBuffer:12*1024*1024});
const delay=ms=>new Promise(r=>setTimeout(r,ms));
const decode=s=>s.replace(/&quot;/g,'"').replace(/&amp;/g,'&').replace(/&lt;/g,'<').replace(/&gt;/g,'>');
function nodes(){run('shell','uiautomator','dump','/sdcard/confirmation-qa.xml');const xml=run('shell','cat','/sdcard/confirmation-qa.xml').toString();fs.writeFileSync(out+'/latest.xml',xml);return [...xml.matchAll(/<node\s+([^>]+)/g)].map(m=>Object.fromEntries([...m[1].matchAll(/([\w-]+)="([^"]*)"/g)].map(v=>[v[1],decode(v[2])])));}
function screenshot(name){if(!/^[\w-]+$/.test(name))throw Error('bad filename');fs.writeFileSync(out+'/'+name+'.png',run('exec-out','screencap','-p'));}
function tap(n){const b=n.bounds.match(/\d+/g).map(Number);run('shell','input','tap',String(Math.round((b[0]+b[2])/2)),String(Math.round((b[1]+b[3])/2)));}
const endpoint='http://127.0.0.1:8191/v1/projects/demo-agenda-phone/databases/(default)/documents/';
function encode(v){if(v===null)return {nullValue:null};if(v instanceof Date)return {timestampValue:v.toISOString()};if(Array.isArray(v))return {arrayValue:{values:v.map(encode)}};if(typeof v==='object')return {mapValue:{fields:Object.fromEntries(Object.entries(v).map(([k,v])=>[k,encode(v)]))}};return typeof v==='string'?{stringValue:v}:typeof v==='boolean'?{booleanValue:v}:Number.isInteger(v)?{integerValue:String(v)}:{doubleValue:v};}
function unpack(v){if('timestampValue'in v)return new Date(v.timestampValue);if('mapValue'in v)return Object.fromEntries(Object.entries(v.mapValue.fields||{}).map(([k,v])=>[k,unpack(v)]));if('arrayValue'in v)return (v.arrayValue.values||[]).map(unpack);if('integerValue'in v)return Number(v.integerValue);return v.stringValue??v.booleanValue??v.doubleValue??null;}
async function read(){const res=await fetch(endpoint+'agendas/phone-agenda');if(!res.ok)throw Error(await res.text());return unpack({mapValue:await res.json()});}
async function put(doc,data){const res=await fetch(endpoint+doc,{method:'PATCH',headers:{'Content-Type':'application/json'},body:JSON.stringify({fields:encode(data).mapValue.fields})});if(!res.ok)throw Error(await res.text());}
async function main(){const [cmd,...args]=process.argv.slice(2);
 if(cmd==='install'){run('install','-r',path.resolve('app/build/outputs/apk/qa/app-qa.apk'));run('reverse','tcp:8191','tcp:8191');console.log('QA APK installed; emulator forwarded');}
 else if(cmd==='seed'){
  const agenda={numeroUnidad:'phone-qa',fecha:new Date('2026-11-15T15:00:00Z'),estado:'BORRADOR',confirmacionVersion:1,revision:0,aceptaciones:{},preside:'Presidente QA',dirige:'Consejero QA',primeraOracion:'Ana QA',oracionFinal:'Luis QA',pianista:'Maria QA',directorMusica:'Pedro QA',primerHimnoNumero:1,primerHimnoNombre:'Himno QA 1',himnoSacramentalNumero:2,himnoSacramentalNombre:'Himno QA 2',himnoFinalNumero:3,himnoFinalNombre:'Himno QA 3',mensajesEvangelio:[{id:'talk-one',tipo:'DISCURSO',nombre:'Juan QA',tema:'Fe'},{id:'talk-two',tipo:'DISCURSO',nombre:'Sofia QA',tema:'Amor'}],creadoPor:'qa@example.test',creadoEn:new Date(),ultimaEdicionPor:'qa@example.test',ultimaEdicionEn:new Date()};
  await put('agendas/phone-agenda',agenda);
  for(const [id,nombre,organizacion]of[['qa-juan','Juan QA','ELDERS_QUORUM'],['qa-sofia','Sofia QA','YOUNG_WOMEN']])await put('hermanos/'+id,{numeroUnidad:'phone-qa',nombre,organizacion,agregadoManualmente:true});
  console.log('Seeded demo emulator only');
 }
 else if(cmd==='open'){run('shell','am','force-stop',app);console.log(run('shell','am','start','-W','-n',app+'/com.example.agendasacramental.ConfirmationPhoneFixture','--es','screen',args[0]||'editor','--es','language',args[1]||'es','--es','scenario',args[2]||'normal','--es','appearance',args[3]||'light').toString());await delay(2200);}
 else if(cmd==='screen'){const ns=nodes();console.log(ns.filter(n=>n.text||n['content-desc']||n.checkable==='true').map(n=>JSON.stringify({text:n.text,desc:n['content-desc'],bounds:n.bounds,checked:n.checked,enabled:n.enabled,cls:n.class})).join('\n'));if(args[0])screenshot(args[0]);}
 else if(cmd==='tap'){const ns=nodes(),matches=ns.filter(n=>n.text===args[0]||n['content-desc']===args[0]);if(!matches.length)throw Error('No observed element: '+args[0]);tap(matches[Number(args[1]||0)]);await delay(500);}
 else if(cmd==='replace'){const n=nodes().find(n=>n.text===args[0]&&n.class==='android.widget.EditText');if(!n)throw Error('No observed input');tap(n);run('shell','input','keycombination','113','29');run('shell','input','keyevent','67');if(args[1])run('shell','input','text',args[1].replaceAll(' ','%s'));run('shell','input','keyevent','4');await delay(500);}
 else if(cmd==='assert'){let a;for(let i=0;i<30;i++){a=await read();if(a.estado===args[0]&&Object.keys(a.aceptaciones||{}).length===Number(args[1])){console.log('Persisted',a.estado,Object.keys(a.aceptaciones||{}).length,'acceptances, revision',a.revision);return;}await delay(500);}throw Error(JSON.stringify(a));}
 else if(cmd==='xy'){run('shell','input','tap',args[0],args[1]);await delay(500);}
 else if(cmd==='scroll'){const b=nodes()[0].bounds.match(/\d+/g).map(Number);const x=String(Math.round(b[2]/2)),top=String(Math.round(b[3]*.28)),bottom=String(Math.round(b[3]*.8));run('shell','input','swipe',x,args[0]==='up'?top:bottom,x,args[0]==='up'?bottom:top,'450');await delay(450);}
 else if(cmd==='back'){run('shell','input','keyevent','4');}
 else if(cmd==='read'){console.log(JSON.stringify(await read(),null,2));}
 else if(cmd==='variant'){const a=await read();a.revision++;a.aceptaciones={};a.estado='BORRADOR';a.presentacionPrimaria=args[0]==='primary';a.reunionTestimonios=args[0]==='testimony';a.responsablePrimaria=a.presentacionPrimaria?'Elena QA':'';if(args[0]==='intermediate')a.mensajesEvangelio.push({id:'hymn-mid',tipo:'HIMNO_INTERMEDIO',himnoNumero:25,himnoNombre:'Intermedio QA',nombre:'',tema:''});await put('agendas/phone-agenda',a);console.log('QA variant',args[0]);}
 else if(cmd==='remote'){const a=await read();a.primeraOracion='Remoto QA';a.revision++;a.ultimaEdicionEn=new Date();await put('agendas/phone-agenda',a);console.log('Emulator remote edit written');}
 else if(cmd==='accept-visible'){const ns=nodes();let clicked=0;for(const n of ns.filter(n=>n.checkable==='true'&&n.checked==='false'&&n.enabled==='true')){const b=n.bounds.match(/\d+/g).map(Number);const completed=ns.find(m=>/Reunión realizada|Meeting completed/.test(m.text)&&Math.abs(Number(m.bounds.match(/\d+/g)[1])-b[1])<70);if(!completed&&b[1]>200&&b[3]<2300){tap(n);clicked++;}}console.log('checked',clicked);}
 else if(cmd==='logs'){const pid=run('shell','pidof',app).toString().trim();const logs=run('logcat','-d','--pid='+pid).toString();fs.writeFileSync(out+'/app.log',logs);console.log(logs.split('\n').filter(l=>/FATAL EXCEPTION|PERMISSION_DENIED|Exception|FirebaseFirestore|Firestore/.test(l)).slice(-25).join('\n'));}
 else throw Error('unknown mode');
}
main().catch(e=>{console.error(e);process.exitCode=1});
