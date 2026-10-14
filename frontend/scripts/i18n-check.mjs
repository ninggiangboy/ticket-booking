// `pnpm i18n:check` (DOC-31 §9, DOC-40 §6). For each namespace in src/locales/<lng>/<ns>.json:
// same keys in every locale, same ICU placeholders per key, messages that parse, no empty values,
// keys prefixed by their namespace, and every literal t("…") in src pointing at an existing key.
// Unused keys and untranslated Vietnamese in `en` are warnings. Hard-coded JSX text is caught by
// ESLint (i18next/no-literal-string).
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join, relative } from "node:path";
import { fileURLToPath } from "node:url";
import { IntlMessageFormat } from "intl-messageformat";

const VIETNAMESE = /[àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ]/i;
const T_CALL = /\bt\(\s*["'`]([a-z][\w.-]*)["'`]/g;

/** Top-level ICU argument names: `{name}`, `{count, plural, …}`. */
export function placeholders(message) {
  const names = new Set();
  let depth = 0;
  for (let i = 0; i < message.length; i += 1) {
    const char = message[i];
    if (char === "{") {
      if (depth === 0) {
        const match = /^\{\s*([A-Za-z_][\w]*)/.exec(message.slice(i));
        if (match) names.add(match[1]);
      }
      depth += 1;
    } else if (char === "}") {
      depth = Math.max(0, depth - 1);
    }
  }
  return [...names].sort();
}

function listFiles(dir, accept) {
  const out = [];
  for (const name of readdirSync(dir)) {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) out.push(...listFiles(path, accept));
    else if (accept(path)) out.push(path);
  }
  return out;
}

export function checkCatalogs({ localesDir, srcDir }) {
  const errors = [];
  const warnings = [];
  // `vi` is the source locale (DOC-31 §1), so it is the base of every comparison.
  const locales = readdirSync(localesDir)
    .filter((name) => statSync(join(localesDir, name)).isDirectory())
    .sort((a, b) => (a === "vi" ? -1 : b === "vi" ? 1 : a.localeCompare(b)));
  const namespaces = new Set();
  const catalogs = {};
  for (const locale of locales) {
    catalogs[locale] = {};
    for (const file of readdirSync(join(localesDir, locale)).filter((f) => f.endsWith(".json"))) {
      const ns = file.slice(0, -".json".length);
      namespaces.add(ns);
      try {
        catalogs[locale][ns] = JSON.parse(readFileSync(join(localesDir, locale, file), "utf8"));
      } catch (error) {
        errors.push(`${ns}: ${locale}/${file} is not valid JSON (${error.message})`);
      }
    }
  }

  const allKeys = new Set();
  for (const ns of [...namespaces].sort()) {
    const keySets = {};
    for (const locale of locales) {
      const catalog = catalogs[locale][ns];
      if (!catalog) {
        errors.push(`${ns}: missing file ${locale}/${ns}.json`);
        continue;
      }
      keySets[locale] = new Set(Object.keys(catalog));
      for (const [key, value] of Object.entries(catalog)) {
        allKeys.add(key);
        if (!key.startsWith(`${ns}.`))
          errors.push(`${ns}: key outside its namespace in ${locale}: ${key}`);
        if (typeof value !== "string" || value.trim() === "") {
          errors.push(`${ns}: empty value in ${locale}: ${key}`);
          continue;
        }
        try {
          new IntlMessageFormat(value, locale);
        } catch (error) {
          errors.push(`${ns}: invalid ICU message in ${locale}: ${key} (${error.message})`);
        }
      }
    }
    const present = Object.keys(keySets);
    for (const locale of present) {
      for (const other of present) {
        if (locale === other) continue;
        for (const key of keySets[locale]) {
          if (!keySets[other].has(key)) errors.push(`${ns}: missing in ${other}: ${key}`);
        }
      }
    }
    const [base, ...rest] = present;
    for (const key of base ? keySets[base] : []) {
      const expected = placeholders(String(catalogs[base][ns][key] ?? ""));
      for (const locale of rest) {
        const value = catalogs[locale][ns]?.[key];
        if (typeof value !== "string") continue;
        const actual = placeholders(value);
        if (expected.join(",") !== actual.join(",")) {
          const show = (list) => (list.length ? list.map((n) => `{${n}}`).join(" ") : "(none)");
          errors.push(
            `${ns}: placeholder mismatch ${key}: ${base} ${show(expected)}, ${locale} ${show(actual)}`,
          );
        }
      }
      const vi = catalogs.vi?.[ns]?.[key];
      const en = catalogs.en?.[ns]?.[key];
      if (typeof vi === "string" && vi === en && VIETNAMESE.test(en)) {
        warnings.push(`${ns}: en is still Vietnamese: ${key}`);
      }
    }
  }

  const used = new Set();
  const sources = listFiles(
    srcDir,
    (path) => /\.(ts|tsx)$/.test(path) && !path.includes(`${join("src", "locales")}`),
  );
  for (const file of sources) {
    const text = readFileSync(file, "utf8");
    for (const match of text.matchAll(T_CALL)) {
      used.add(match[1]);
      if (!allKeys.has(match[1])) errors.push(`${relative(srcDir, file)}: unknown key ${match[1]}`);
    }
  }
  for (const key of [...allKeys].sort()) {
    if (!used.has(key)) warnings.push(`unused key ${key}`);
  }
  return { errors: [...new Set(errors)], warnings };
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const root = new URL("..", import.meta.url);
  const { errors, warnings } = checkCatalogs({
    localesDir: fileURLToPath(new URL("src/locales", root)),
    srcDir: fileURLToPath(new URL("src", root)),
  });
  for (const warning of warnings) console.warn(`  warning: ${warning}`);
  if (errors.length > 0) {
    console.error("i18n:check FAILED");
    for (const error of errors) console.error(`  ${error}`);
    process.exit(1);
  }
  console.log(`i18n:check OK (${warnings.length} warnings)`);
}
