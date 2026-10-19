import { readItem, writeItem } from "../../lib/storage";

// A failed chunk import usually means a new deploy renamed hashed files: reload once per
// browser session, then show E5 (DR-140).

const FLAG = "tb.chunkReload";

export function isChunkLoadError(error: unknown): boolean {
  if (!(error instanceof Error)) return false;
  return (
    error.name === "ChunkLoadError" ||
    /dynamically imported module|Importing a module script failed|error loading dynamically imported module/i.test(
      error.message,
    )
  );
}

/** Returns true when a reload was started. */
export function reloadOnceForChunkError(): boolean {
  if (readItem(FLAG, "session")) return false;
  writeItem(FLAG, "1", "session");
  window.location.reload();
  return true;
}
