// 构建产物验收：解析 jar 里的 fabric.mod.json、点数 A 类残留、统计资源。
const { execSync } = require('child_process');
const JAR = 'D:/MC-MTR-JS/mtr-headlight/v1.20.1/build/libs/lted-2.0.2.jar';
const sh = (c) => execSync(c, { maxBuffer: 1 << 28 }).toString();

const mod = JSON.parse(sh(`unzip -p "${JAR}" fabric.mod.json`));
console.log('fabric.mod.json: name=' + mod.name + ' id=' + mod.id + ' version=' + mod.version);
console.log('  mixins     :', JSON.stringify(mod.mixins));
console.log('  entrypoints:', JSON.stringify(mod.entrypoints));
console.log('  desc lines :', mod.description.split('\n').length);

const list = sh(`unzip -l "${JAR}"`);
const A = ['BeamGlow', 'TrainLamps', 'LightController', 'LightFlare', 'CollisionSystem',
  'CollisionImpact', 'MobCollision', 'CarBand', 'RailBed', 'Trackbed', 'EndLight',
  'RainSpray', 'RainHush', 'ParticleCull', 'HeadlightConfig', 'MtrHeadlightClient',
  'ScatterPass', 'BeamEditor', 'VehicleLights', 'VehicleLightJson', 'LteServerLights',
  'PidsConnectorItem', 'ProjectorConnectorItem', 'SignalLinkItem', 'PidsLinkStore',
  'PidsScheduleScope', 'DisplayPushTicker', 'BuiltInTrains', 'RegistrySelfCheck',
  'SimpleRegistryFreezeMixin', 'LteChannel', 'ServerAuthority', 'HostProbe',
  'BeamBlockLight', 'ConeVolumeBeam', 'ProtoBeamCone', 'GlowCompat', 'LightBackends',
  'SidingScreen', 'ParameterScreen', 'ConfigWriteTarget', 'ConfigCategory', 'Spawns'];
const hits = A.filter((n) => new RegExp('(^|/)' + n + '(\\.class|\\$)', 'm').test(list));
console.log('A-class leftovers:', hits.length ? hits.join(', ') : 'none');

const count = (re) => (list.match(re) || []).length;
console.log('class count :', count(/\.class$/gm));
console.log('refmap      :', list.split('\n').filter((l) => l.includes('refmap')).map((l) => l.trim()).join(' | ') || 'none');
console.log('shaders     :', count(/shaders\//gm), ' particles:', count(/particles\//gm));
console.log('lang files  :', list.split('\n').filter((l) => l.includes('/lang/')).map((l) => l.trim().split(/\s+/).pop()).join(', '));

const ref = sh(`unzip -p "${JAR}" lte.refmap.json`);
const r = JSON.parse(ref);
console.log('refmap mappings:', Object.keys(r.mappings || {}).join(', '));
