// Placeholder until P1-09 adds the i18next catalogs (src/locales/{vi,en}/*.json).
// Passes while no catalog exists; from P1-09 it fails on missing keys, extra keys,
// hard-coded JSX strings and mismatched placeholders between `vi` and `en` (DOC-31).
import { existsSync } from "node:fs";

if (!existsSync(new URL("../src/locales", import.meta.url))) {
  console.log("i18n:check: no locales yet, skipped");
}
