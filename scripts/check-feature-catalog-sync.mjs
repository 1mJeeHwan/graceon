#!/usr/bin/env node
/**
 * Feature-catalog drift guard.
 *
 * Two descriptions of the same feature set must not drift apart:
 *   - streamhub-api/src/main/resources/feature-catalog.json — what the chatbot tells users
 *   - streamhub-web/src/lib/features.catalog.ts — the admin console's catalog cards
 *
 * What is compared depends on the JSON entry's `audience`, because the two files describe
 * different surfaces:
 *
 *   - **status, always.** This is the honesty badge (live / demo / external). If the console says a
 *     feature is a demo while the chatbot calls it live, the product is lying to somebody. This is
 *     the check the guard exists for.
 *   - **title and href, only for `audience: "admin"`.** For `user` and `both` entries the JSON
 *     deliberately describes the *user site* ("굿즈샵" at `/goods`) while the TS describes the
 *     *admin screen* that manages it ("굿즈 관리" at `/goods`). Requiring those to be equal was
 *     comparing two different things, and the guard had been failing on ~30 such pairs — a red
 *     check nobody could act on, which is worse than no check because it trains you to ignore it.
 *   - **every TS card must exist in the JSON.** An admin feature the chatbot has never heard of
 *     means the bot answers "no such feature" about something that ships.
 *   - **JSON-only entries are allowed when `audience: "user"`.** Sign-up, favourites, and watch
 *     history are user-site features with no admin card by definition. A JSON-only `admin`/`both`
 *     entry is still drift.
 *
 * `howTo` (chatbot usage guide) has no TS counterpart — presence is checked, content is not.
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
    map.set(f.id, {
      title: f.title,
      status: f.status,
      href: f.href,
      howTo: f.howTo,
      audience: f.audience,
    });
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
    // The honesty badge must agree on every shared feature, whoever the audience is.
    if (t.status !== j.status) {
      problems.push(
        `불일치 [${id}.status]: TS="${t.status}" vs JSON="${j.status}" — 실동작/데모 표시가 어긋납니다`,
      );
    }
    // Wording and links only describe the same surface for admin-only features.
    if (j.audience === "admin") {
      for (const field of ["title", "href"]) {
        if (t[field] !== j[field]) {
          problems.push(`불일치 [${id}.${field}]: TS="${t[field]}" vs JSON="${j[field]}"`);
        }
      }
    }
    if (!j.howTo || !j.howTo.trim()) {
      problems.push(`howTo 비어있음: "${id}" — 챗봇 사용법 안내 누락`);
    }
  }
  for (const [id, j] of json) {
    // User-site features legitimately have no admin card; anything else is a missing card.
    if (!ts.has(id) && j.audience !== "user") {
      problems.push(
        `TS 누락: "${id}" (JSON audience=${j.audience}) — 관리자 카탈로그 카드 확인`,
      );
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
