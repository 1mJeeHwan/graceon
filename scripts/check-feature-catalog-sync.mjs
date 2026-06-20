#!/usr/bin/env node
/**
 * Feature-catalog drift guard.
 *
 * The chatbot's knowledge source — streamhub-api/src/main/resources/feature-catalog.json — is a
 * hand-maintained mirror of the admin catalog cards in
 * streamhub-web/src/lib/features.catalog.ts. This script compares the two so they never silently
 * diverge: it checks that the set of feature ids matches and that each shared id has the same
 * title / status / href. The JSON additionally carries a `howTo` field (chatbot usage guide) that
 * has no TS counterpart — its presence/non-emptiness is checked but not compared.
 *
 * Usage:  node scripts/check-feature-catalog-sync.mjs
 * Exit:   0 = in sync, 1 = drift found (prints a report). No deps, no network.
 */
import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const JSON_PATH = resolve(root, "streamhub-api/src/main/resources/feature-catalog.json");
const TS_PATH = resolve(root, "streamhub-web/src/lib/features.catalog.ts");

/** Parses the backend JSON into a Map<id, {title,status,href,howTo}>. */
function readJson() {
  const data = JSON.parse(readFileSync(JSON_PATH, "utf8"));
  const map = new Map();
  for (const f of data.features ?? []) {
    map.set(f.id, { title: f.title, status: f.status, href: f.href, howTo: f.howTo });
  }
  return map;
}

/**
 * Extracts each card from the TS FEATURES array. Fields appear in a fixed order
 * (id → title → status → href), so a non-greedy regex captures one card per id.
 */
function readTs() {
  const src = readFileSync(TS_PATH, "utf8");
  const re =
    /id:\s*"([^"]+)"[\s\S]*?title:\s*"([^"]+)"[\s\S]*?status:\s*"([^"]+)"[\s\S]*?href:\s*"([^"]+)"/g;
  const map = new Map();
  let m;
  while ((m = re.exec(src)) !== null) {
    map.set(m[1], { title: m[2], status: m[3], href: m[4] });
  }
  return map;
}

function main() {
  const json = readJson();
  const ts = readTs();
  const problems = [];

  for (const [id, t] of ts) {
    const j = json.get(id);
    if (!j) {
      problems.push(`JSON 누락: "${id}" (TS에는 있음) — feature-catalog.json에 추가 필요`);
      continue;
    }
    for (const field of ["title", "status", "href"]) {
      if (t[field] !== j[field]) {
        problems.push(`불일치 [${id}.${field}]: TS="${t[field]}" vs JSON="${j[field]}"`);
      }
    }
    if (!j.howTo || !j.howTo.trim()) {
      problems.push(`howTo 비어있음: "${id}" — 챗봇 사용법 안내 누락`);
    }
  }
  for (const id of json.keys()) {
    if (!ts.has(id)) {
      problems.push(`TS 누락: "${id}" (JSON에는 있음) — 카탈로그 카드 확인`);
    }
  }

  console.log(`TS 카드 ${ts.size}개 · JSON 기능 ${json.size}개`);
  if (problems.length === 0) {
    console.log("✅ 기능 카탈로그 동기화 OK");
    process.exit(0);
  }
  console.error(`❌ 드리프트 ${problems.length}건:`);
  for (const p of problems) console.error("  - " + p);
  process.exit(1);
}

main();
