package io.ticket.arch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Finds SQL inside Java string literals and text blocks. ArchUnit cannot see literals, so the
 * table-ownership and read-only-invariant rules (ARC-05, ARC-07) read the sources instead.
 */
final class SqlSourceScanner {

  /** Owner of each table (DOC-07 §4.1). */
  static final Map<String, String> TABLE_OWNER =
      Map.ofEntries(
          Map.entry("app_user", "auth"),
          Map.entry("organizer", "auth"),
          Map.entry("login_token", "auth"),
          Map.entry("session", "auth"),
          Map.entry("media", "media"),
          Map.entry("event", "event"),
          Map.entry("ticket_type", "event"),
          Map.entry("seat_map", "map"),
          Map.entry("seat_map_version", "map"),
          Map.entry("inventory_pool", "inventory"),
          Map.entry("inventory_unit", "inventory"),
          Map.entry("inventory_pool_counter", "inventory"),
          Map.entry("reservation", "reservation"),
          Map.entry("reservation_item", "reservation"),
          Map.entry("idempotency_key", "reservation"),
          Map.entry("orders", "order"),
          Map.entry("ticket", "ticket"),
          Map.entry("stripe_event", "payment"),
          Map.entry("fake_payment_intent", "payment"),
          Map.entry("outbox", "notification"));

  private static final Pattern TEXT_BLOCK = Pattern.compile("\"\"\"(.*?)\"\"\"", Pattern.DOTALL);
  private static final Pattern STRING = Pattern.compile("\"((?:[^\"\\\\\\n]|\\\\.)*)\"");
  private static final Pattern TABLE_REF =
      Pattern.compile("(?i)\\b(?:FROM|JOIN|INTO|UPDATE)\\s+([a-z_][a-z0-9_]*)");
  private static final Pattern SQL_VERB =
      Pattern.compile(
          "(?i)\\b(SELECT\\b.*\\bFROM|INSERT\\s+INTO|UPDATE\\s+\\w+\\s+SET|DELETE\\s+FROM)",
          Pattern.DOTALL);
  private static final Pattern WRITE_VERB =
      Pattern.compile("(?i)\\b(INSERT\\s+INTO|UPDATE\\s+\\w+\\s+SET|DELETE\\s+FROM)");

  private SqlSourceScanner() {}

  /** {@code path → source} for every Java file under a source root. */
  static Map<String, String> read(Path root) throws IOException {
    try (Stream<Path> files = Files.walk(root)) {
      Map<String, String> out = new java.util.TreeMap<>();
      for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
        out.put(root.relativize(p).toString().replace('\\', '/'), Files.readString(p));
      }
      return out;
    }
  }

  static List<String> literals(String source) {
    String noComments = source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    List<String> out = new ArrayList<>();
    Matcher block = TEXT_BLOCK.matcher(noComments);
    StringBuilder rest = new StringBuilder();
    int last = 0;
    while (block.find()) {
      out.add(block.group(1));
      rest.append(noComments, last, block.start());
      last = block.end();
    }
    rest.append(noComments.substring(last));
    Matcher str = STRING.matcher(rest);
    while (str.find()) {
      out.add(str.group(1));
    }
    return out;
  }

  /** Module name of {@code io/ticket/<module>/…}, or {@code null} for files outside a module. */
  static String moduleOf(String relativePath) {
    String[] parts = relativePath.replace('\\', '/').split("/");
    return parts.length > 3 && parts[0].equals("io") && parts[1].equals("ticket") ? parts[2] : null;
  }

  /**
   * ARC-05: SQL in module X names only tables X owns. {@code invariant} may read everything (DOC-12
   * §2.2 rule 7).
   */
  static List<String> ownershipViolations(Map<String, String> sources) {
    List<String> violations = new ArrayList<>();
    sources.forEach(
        (path, source) -> {
          String module = moduleOf(path);
          if (module == null || module.equals("invariant") || module.equals("common")) {
            return;
          }
          for (String literal : literals(source)) {
            if (!SQL_VERB.matcher(literal).find()) {
              continue;
            }
            Matcher m = TABLE_REF.matcher(literal);
            while (m.find()) {
              String table = m.group(1).toLowerCase();
              String owner = TABLE_OWNER.get(table);
              if (owner != null && !owner.equals(module)) {
                violations.add(
                    path + ": module " + module + " touches " + table + " owned by " + owner);
              }
            }
          }
        });
    return violations;
  }

  /** ARC-07: the invariant checker never writes. */
  static List<String> invariantWriteViolations(Map<String, String> sources) {
    List<String> violations = new ArrayList<>();
    sources.forEach(
        (path, source) -> {
          if (!"invariant".equals(moduleOf(path))) {
            return;
          }
          for (String literal : literals(source)) {
            if (WRITE_VERB.matcher(literal).find()) {
              violations.add(path + ": write statement in invariant module");
            }
          }
        });
    return violations;
  }
}
