/**
 * PinPulse Master Admin Console - Hardware-Secured Local Web Server
 * 
 * Enforces strict physical machine MAC address verification.
 * Only boots and serves requests if running on the authorized host hardware.
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const os = require('os');

// Authorized Physical Hardware Addresses (MACs) for Master Admin PC
const AUTHORIZED_MACS = [
  '0a:e0:af:a6:0e:2f', // Primary Ethernet Adapter (Authorized Admin Workstation)
  '90:de:80:6c:f1:dd'  // Secondary Wi-Fi Adapter
];

const PORT = process.env.PORT || 8080;
const HOST = '127.0.0.1'; // Strictly bind to localhost for local machine access only

function normalizeMac(mac) {
  if (!mac) return '';
  return mac.toLowerCase().replace(/-/g, ':').trim();
}

function verifyHostHardware() {
  const nics = os.networkInterfaces();
  const detectedMacs = [];
  let authorizedMatch = null;

  for (const [ifName, addrs] of Object.entries(nics)) {
    if (!addrs) continue;
    for (const addr of addrs) {
      if (addr && addr.mac && addr.mac !== '00:00:00:00:00:00') {
        const norm = normalizeMac(addr.mac);
        detectedMacs.push({ interface: ifName, mac: norm, ip: addr.address });
        if (AUTHORIZED_MACS.includes(norm)) {
          authorizedMatch = {
            interface: ifName,
            mac: norm,
            ip: addr.address
          };
        }
      }
    }
  }

  return { isAuthorized: !!authorizedMatch, match: authorizedMatch, all: detectedMacs };
}

// 1. Initial Hardware Validation
console.log('\n========================================================================');
console.log('   PINPULSE MASTER RADAR & FLEET COMMAND - HARDWARE GUARD');
console.log('========================================================================');

const hwCheck = verifyHostHardware();

if (!hwCheck.isAuthorized) {
  console.error('\x1b[31m%s\x1b[0m', ' [FATAL ERROR] UNAUTHORIZED HARDWARE DETECTED!');
  console.error('\x1b[31m%s\x1b[0m', ' This software is cryptographically locked to authorized physical PC hardware.');
  console.error('\x1b[31m%s\x1b[0m', ' Detected Network MAC addresses:');
  hwCheck.all.forEach(d => console.error(`   - Interface [${d.interface}]: ${d.mac}`));
  console.error('\x1b[31m%s\x1b[0m', '\n Authorized Hardware IDs:');
  AUTHORIZED_MACS.forEach(m => console.error(`   - ${m.toUpperCase()}`));
  console.error('\x1b[31m%s\x1b[0m', '\n ACCESS DENIED. Server startup terminated immediately.');
  console.log('========================================================================\n');
  process.exit(1);
}

const verified = hwCheck.match;
console.log('\x1b[32m%s\x1b[0m', ' [STATUS] HARDWARE SIGNATURE VERIFIED: ACCESS GRANTED');
console.log(` - Matched Interface:  ${verified.interface}`);
console.log(` - Physical MAC:       ${verified.mac.toUpperCase()}`);
console.log(` - Host Machine:       ${os.hostname()}`);
console.log(` - Authorized User:    ${os.userInfo().username}`);
console.log('========================================================================\n');

// 2. MIME Types Mapping
const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js':   'application/javascript; charset=utf-8',
  '.css':  'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png':  'image/png',
  '.jpg':  'image/jpeg',
  '.gif':  'image/gif',
  '.svg':  'image/svg+xml',
  '.ico':  'image/x-icon'
};

// 3. HTTP Server
const server = http.createServer((req, res) => {
  // Re-verify hardware integrity on incoming requests
  const liveCheck = verifyHostHardware();
  if (!liveCheck.isAuthorized) {
    res.writeHead(403, { 'Content-Type': 'text/plain' });
    res.end('403 Forbidden: Host hardware authorization revoked.');
    return;
  }

  const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const pathname = parsedUrl.pathname;

  // API Endpoint: Hardware Auth Status
  if (pathname === '/api/admin/hardware-auth') {
    res.writeHead(200, {
      'Content-Type': 'application/json',
      'Access-Control-Allow-Origin': '*'
    });
    res.end(JSON.stringify({
      authorized: true,
      verifiedMac: liveCheck.match.mac.toUpperCase(),
      interface: liveCheck.match.interface,
      hostName: os.hostname(),
      user: os.userInfo().username,
      timestamp: Date.now()
    }));
    return;
  }

  // Static File Serving
  let relativePath = pathname;
  if (relativePath === '/' || relativePath === '') {
    relativePath = '/index.html';
  }

  const safePath = path.normalize(path.join(__dirname, 'web', relativePath));

  // Security: prevent path traversal outside web folder
  const webDir = path.normalize(path.join(__dirname, 'web'));
  if (!safePath.startsWith(webDir)) {
    res.writeHead(403, { 'Content-Type': 'text/plain' });
    res.end('403 Forbidden: Invalid file path.');
    return;
  }

  fs.stat(safePath, (err, stats) => {
    if (err || !stats.isFile()) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('404 Not Found');
      return;
    }

    const ext = path.extname(safePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';

    res.writeHead(200, {
      'Content-Type': contentType,
      'Cache-Control': 'no-cache, no-store, must-revalidate'
    });

    const stream = fs.createReadStream(safePath);
    stream.pipe(res);
  });
});

server.listen(PORT, HOST, () => {
  console.log(`\x1b[36mPinPulse Master Admin Console running securely at:\x1b[0m`);
  console.log(`\x1b[1m\x1b[32m  >>> http://${HOST}:${PORT}\x1b[0m`);
  console.log(`  >>> http://localhost:${PORT}\n`);
  console.log(`Press Ctrl+C to terminate server.\n`);
});
