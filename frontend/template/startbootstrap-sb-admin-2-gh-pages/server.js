"use strict";

const http = require("http");
const fs = require("fs");
const path = require("path");

const host = "127.0.0.1";
const port = Number(process.env.PORT || 4444);
const rootDir = __dirname;

const mimeTypes = {
  ".css": "text/css; charset=utf-8",
  ".gif": "image/gif",
  ".html": "text/html; charset=utf-8",
  ".ico": "image/x-icon",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".js": "application/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".map": "application/json; charset=utf-8",
  ".png": "image/png",
  ".svg": "image/svg+xml",
  ".txt": "text/plain; charset=utf-8",
  ".woff": "font/woff",
  ".woff2": "font/woff2"
};

function send(res, statusCode, body, contentType) {
  res.writeHead(statusCode, { "Content-Type": contentType });
  res.end(body);
}

function resolvePath(urlPath) {
  const safePath = path.normalize(decodeURIComponent(urlPath)).replace(/^(\.\.[/\\])+/, "");
  const requestedPath = path.join(rootDir, safePath);

  if (!requestedPath.startsWith(rootDir)) {
    return null;
  }

  return requestedPath;
}

const server = http.createServer((req, res) => {
  const reqUrl = new URL(req.url, `http://${req.headers.host || `${host}:${port}`}`);
  const pathname = reqUrl.pathname === "/" ? "/index.html" : reqUrl.pathname;
  const filePath = resolvePath(pathname);

  if (!filePath) {
    send(res, 403, "Forbidden", "text/plain; charset=utf-8");
    return;
  }

  fs.stat(filePath, (statErr, stats) => {
    if (statErr) {
      send(res, 404, "Not Found", "text/plain; charset=utf-8");
      return;
    }

    const targetPath = stats.isDirectory() ? path.join(filePath, "index.html") : filePath;

    fs.readFile(targetPath, (readErr, data) => {
      if (readErr) {
        send(res, 404, "Not Found", "text/plain; charset=utf-8");
        return;
      }

      const ext = path.extname(targetPath).toLowerCase();
      send(res, 200, data, mimeTypes[ext] || "application/octet-stream");
    });
  });
});

server.listen(port, host, () => {
  console.log(`SB Admin 2 disponible en http://${host}:${port}`);
});

server.on("error", (error) => {
  if (error.code === "EADDRINUSE") {
    console.error(`El puerto ${port} ya esta en uso en ${host}. Cierra el proceso anterior o usa otro puerto, por ejemplo: PORT=4445 npm start`);
    process.exit(1);
  }

  console.error(error);
  process.exit(1);
});
