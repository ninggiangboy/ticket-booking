#!/usr/bin/env bash
# Gzip size of the JS loaded by page `/` must stay <= 200 KB (DR-81, DOC-38).
set -euo pipefail
cd "$(git rev-parse --show-toplevel)/frontend"
LIMIT=$((200 * 1024))
node --input-type=module - "$LIMIT" <<'JS'
import { readFileSync } from "node:fs";
import { gzipSync } from "node:zlib";

const limit = Number(process.argv[2]);
const manifest = JSON.parse(readFileSync("dist/.vite/manifest.json", "utf8"));
const entry = Object.values(manifest).find((c) => c.isEntry);
if (!entry) throw new Error("no entry chunk in manifest");
const seen = new Set();
const visit = (chunk) => {
  if (seen.has(chunk.file)) return;
  seen.add(chunk.file);
  for (const key of chunk.imports ?? []) visit(manifest[key]);
};
visit(entry);
let total = 0;
for (const file of seen) {
  const size = gzipSync(readFileSync(`dist/${file}`)).length;
  total += size;
  console.log(`${String(size).padStart(8)}  ${file}`);
}
console.log(`${String(total).padStart(8)}  total (limit ${limit})`);
if (total > limit) process.exit(1);
JS
