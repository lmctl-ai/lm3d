import assert from 'node:assert/strict';
import { readFile } from 'node:fs';
import http from 'node:http';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from 'playwright';

const repository = fileURLToPath(new URL('../', import.meta.url));
const root = path.join(repository, 'target/demo');
const types = { '.css': 'text/css', '.html': 'text/html', '.js': 'text/javascript', '.svg': 'image/svg+xml', '.gltf': 'model/gltf+json', '.bin': 'application/octet-stream' };
const server = http.createServer((request, response) => {
  const pathname = decodeURIComponent(new URL(request.url, 'http://localhost').pathname);
  const file = path.resolve(root, '.' + (pathname === '/' ? '/demo.html' : pathname));
  if (!file.startsWith(root + path.sep)) {
    response.writeHead(403).end();
    return;
  }
  readFile(file, (error, content) => {
    if (error) {
      response.writeHead(404).end();
      return;
    }
    response.writeHead(200, { 'Content-Type': types[path.extname(file)] || 'application/octet-stream' });
    response.end(content);
  });
});
await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
const address = server.address();
const base = process.env.DEMO_URL ?? `http://127.0.0.1:${address.port}/demo.html`;
const browser = await chromium.launch({
  headless: true,
  executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE || undefined,
  args: ['--enable-webgl', '--use-angle=swiftshader']
});
const page = await browser.newPage({ viewport: { width: 1440, height: 1100 } });
const errors = [];
const requests = [];
page.on('pageerror', error => errors.push(`pageerror: ${error.message}`));
page.on('console', message => {
  if (message.type() === 'error') errors.push(`console: ${message.text()}`);
});
page.on('response', response => {
  if (response.status() >= 400) errors.push(`HTTP ${response.status()}: ${response.url()}`);
});
page.on('request', request => requests.push(request.url()));

async function frame() {
  return page.locator('#scene-canvas').evaluate(canvas => canvas.toDataURL());
}

async function pixels() {
  return page.locator('#scene-canvas').evaluate(canvas => {
    const gl = canvas.getContext('webgl');
    const pixels = new Uint8Array(canvas.width * canvas.height * 4);
    gl.readPixels(0, 0, canvas.width, canvas.height, gl.RGBA, gl.UNSIGNED_BYTE, pixels);
    const background = [pixels[0], pixels[1], pixels[2]];
    let colored = 0;
    for (let i = 0; i < pixels.length; i += 4) {
      if (pixels[i] !== background[0] || pixels[i + 1] !== background[1] || pixels[i + 2] !== background[2]) colored++;
    }
    return { colored, error: gl.getError(), version: gl.getParameter(gl.VERSION) };
  });
}

await page.goto(base);
await page.waitForFunction(() => document.querySelector('#status')?.textContent.startsWith('Ready: Box'));
assert.equal(await page.locator('#geometry option').count(), 10);
assert.match(await page.locator('#render-stats').innerText(), /24 hero vertices · 454 triangles · 3 draw calls/);
assert.deepEqual(await page.locator('script[src]').evaluateAll(scripts => scripts.map(script => script.getAttribute('src'))), ['teavm.js', 'boot.js']);

const animated1 = await frame();
await page.waitForTimeout(180);
const animated2 = await frame();
assert.notEqual(animated1, animated2, 'requestAnimationFrame updates the WebGL scene');
await page.locator('#toggle-animation').click();
assert.equal(await page.locator('#toggle-animation').innerText(), 'Resume animation');
assert.equal(await page.locator('#toggle-animation').getAttribute('aria-pressed'), 'true');
await page.waitForTimeout(80);
const paused1 = await frame();
await page.waitForTimeout(160);
assert.equal(await frame(), paused1, 'pausing freezes object animation');

const webgl = await pixels();
assert.ok(webgl.colored > 50000, `WebGL non-background pixels: ${webgl.colored}`);
assert.equal(webgl.error, 0);
assert.match(webgl.version, /WebGL/);

await page.locator('#geometry').selectOption('sphere');
await page.waitForFunction(() => document.querySelector('#status')?.textContent.startsWith('Ready: Sphere'));
assert.match(await page.locator('#render-stats').innerText(), /825 hero vertices · 1914 triangles/);
const sphere = await frame();
assert.notEqual(sphere, paused1, 'sphere geometry changes rendered pixels');

await page.locator('#geometry').selectOption('torus');
await page.waitForFunction(() => document.querySelector('#status')?.textContent.startsWith('Ready: Torus'));
assert.match(await page.locator('#render-stats').innerText(), /1225 hero vertices · 2746 triangles/);
const torus = await frame();
assert.notEqual(torus, sphere, 'torus geometry changes rendered pixels');
await page.screenshot({ path: path.join(repository, 'target/lm3d-torus-demo.png'), fullPage: true });

await page.locator('#geometry').selectOption('gltf-cube');
await page.waitForFunction(() => document.querySelector('#status')?.textContent.startsWith('Ready: glTF embedded cube'));
assert.match(await page.locator('#render-stats').innerText(), /1 glTF meshes · 24 vertices · 454 triangles · 3 draw calls/);
const gltfCube = await frame();
assert.notEqual(gltfCube, torus, 'embedded glTF buffer renders a different scene');
assert.ok(requests.some(url => url.endsWith('/models/cube-embedded.gltf')));

await page.locator('#geometry').selectOption('gltf-hierarchy');
await page.waitForFunction(() => document.querySelector('#status')?.textContent.startsWith('Ready: glTF external-buffer hierarchy'));
assert.match(await page.locator('#render-stats').innerText(), /2 glTF meshes · 48 vertices · 466 triangles · 4 draw calls/);
assert.equal(await page.locator('#scene-mesh-count').innerText(), '4 meshes');
const gltfHierarchy = await frame();
assert.notEqual(gltfHierarchy, gltfCube, 'external glTF hierarchy renders two transformed meshes');
assert.ok(requests.some(url => url.endsWith('/models/hierarchy.gltf')));
assert.ok(requests.some(url => url.endsWith('/models/hierarchy.bin')));
await page.screenshot({ path: path.join(repository, 'target/lm3d-gltf-hierarchy.png'), fullPage: true });

await page.locator('#geometry').selectOption('gltf-torus');
await page.waitForFunction(() => document.querySelector('#status')?.textContent.startsWith('Ready: glTF interleaved torus'));
assert.match(await page.locator('#render-stats').innerText(), /1 glTF meshes · 561 vertices · 1466 triangles · 3 draw calls/);
const gltfTorus = await frame();
assert.notEqual(gltfTorus, gltfHierarchy, 'interleaved glTF accessors render the torus');
assert.ok(requests.some(url => url.endsWith('/models/torus-interleaved.gltf')));
assert.ok(requests.some(url => url.endsWith('/models/torus.bin')));
assert.equal((await pixels()).error, 0);

await page.locator('#geometry').selectOption('lmcode-cube');
await page.waitForFunction(() => document.querySelector('#status')?.textContent.startsWith('Ready: LMCode @cube'));
assert.match(await page.locator('#render-stats').innerText(), /1 LMCode meshes · 24 vertices · 454 triangles · 3 draw calls/);
const lmcodeCube = await frame();
assert.notEqual(lmcodeCube, gltfTorus, 'LMCode @cube builds and renders through Java hosts');

await page.locator('#geometry').selectOption('lmcode-hierarchy');
await page.waitForFunction(() => document.querySelector('#status')?.textContent.startsWith('Ready: LMCode @hierarchy'));
assert.match(await page.locator('#render-stats').innerText(), /3 LMCode meshes · 1010 vertices · 2198 triangles · 5 draw calls/);
const lmcodeHierarchy = await frame();
assert.notEqual(lmcodeHierarchy, lmcodeCube, 'LMCode @hierarchy composes three transformed meshes');
await page.screenshot({ path: path.join(repository, 'target/lm3d-lmcode-hierarchy.png'), fullPage: true });

await page.locator('#geometry').selectOption('lmcode-materials');
await page.waitForFunction(() => document.querySelector('#status')?.textContent.startsWith('Ready: LMCode @materials'));
assert.match(await page.locator('#render-stats').innerText(), /2 LMCode meshes · 2050 vertices · 4218 triangles · 4 draw calls/);
const lmcodeMaterials = await frame();
assert.notEqual(lmcodeMaterials, lmcodeHierarchy, 'LMCode material host functions alter the scene');
assert.equal((await pixels()).error, 0);

await page.locator('#geometry').selectOption('lmcode-failure');
await page.waitForFunction(() => document.querySelector('#status')?.textContent.startsWith('LMCode @failure failed:'));
assert.match(await page.locator('#status').innerText(), /previous scene is still visible/);
assert.equal(await frame(), lmcodeMaterials, 'failure after lm3d_use does not replace the previous scene');
assert.equal(await page.locator('#geometry').isDisabled(), false);

await page.locator('#geometry').selectOption('box');
await page.waitForFunction(() => document.querySelector('#status')?.textContent.startsWith('Ready: Box'));
const canvas = page.locator('#scene-canvas');
const bounds = await canvas.boundingBox();
const startX = bounds.x + bounds.width * 0.5;
const startY = bounds.y + bounds.height * 0.5;
const beforeDrag = await frame();
await page.mouse.move(startX, startY);
await page.mouse.down();
await page.mouse.move(startX + 120, startY + 45, { steps: 8 });
await page.mouse.up();
await page.waitForTimeout(100);
assert.notEqual(await frame(), beforeDrag, 'pointer drag changes the camera');

const beforeZoom = await frame();
await page.mouse.wheel(0, -240);
await page.waitForTimeout(100);
assert.notEqual(await frame(), beforeZoom, 'wheel zoom changes the camera');

await page.locator('#reset-scene').click();
assert.match(await page.locator('#status').innerText(), /Scene reset/);
await page.locator('#toggle-animation').press('Space');
assert.equal(await page.locator('#toggle-animation').innerText(), 'Pause animation');
assert.match(await page.locator('#status').innerText(), /Animation resumed/);
const resumed1 = await frame();
await page.waitForTimeout(180);
assert.notEqual(await frame(), resumed1, 'Space resumes animation');

await page.locator('#toggle-animation').click();
await page.locator('#reset-scene').click();
await page.setViewportSize({ width: 390, height: 844 });
assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), 'mobile layout has no horizontal overflow');
await page.screenshot({ path: path.join(repository, 'target/lm3d-mobile-demo.png'), fullPage: true });
await page.setViewportSize({ width: 1440, height: 1100 });
await page.screenshot({ path: path.join(repository, 'target/lm3d-demo.png'), fullPage: true });

const callbackPage = await browser.newPage();
const callbackErrors = [];
callbackPage.on('pageerror', error => callbackErrors.push(error.message));
await callbackPage.goto(base);
await callbackPage.waitForFunction(() => document.querySelector('#status')?.textContent.startsWith('Ready: Box'));
await callbackPage.locator('#scene-mesh-count').evaluate(element => element.remove());
const callbackException = callbackPage.waitForEvent('pageerror');
await callbackPage.locator('#geometry').selectOption('gltf-cube');
await callbackException;
assert.equal(callbackErrors.length, 1, 'glTF success-callback exceptions propagate to the browser');
assert.equal(await callbackPage.locator('#geometry').isDisabled(), false, 'callback failure does not leave the selector disabled');
await callbackPage.close();

assert.deepEqual(errors, []);
const allowedOrigins = new Set([new URL(base).origin, new URL(page.url()).origin]);
const externalRequests = requests.filter(url => !allowedOrigins.has(new URL(url).origin));
assert.deepEqual(externalRequests, [], 'demo loads no third-party runtime resources');
console.log(JSON.stringify({ geometries: ['Box', 'Sphere', 'Torus'], gltf: ['embedded cube', 'external-buffer hierarchy', 'interleaved torus with default scene/material'], lmcode: ['@cube', '@hierarchy', '@materials', '@failure preserves previous scene'], callbackExceptions: 'propagated', webgl, animation: 'frames, pause, Space verified', controls: 'pointer drag, wheel, reset verified', mobile: 'no horizontal overflow', scripts: ['teavm.js', 'boot.js'], errors }, null, 2));
await browser.close();
server.close();
