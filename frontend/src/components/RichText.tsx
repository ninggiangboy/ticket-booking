import { Fragment, type ReactNode } from "react";

/**
 * Puts React nodes into a translated sentence without splitting it into pieces: the message is
 * formatted with markers in place of the values, then split around them (DOC-31 §8 rule 3).
 */
export function interpolateNodes(
  format: (values: Record<string, string>) => string,
  nodes: Record<string, ReactNode>,
): ReactNode[] {
  const names = Object.keys(nodes);
  const markers = Object.fromEntries(names.map((name) => [name, `\u0000${name}\u0000`]));
  const text = format(markers);
  return text
    .split("\u0000")
    .map((part, index) =>
      index % 2 === 1 && part in nodes ? (
        <Fragment key={index}>{nodes[part]}</Fragment>
      ) : (
        <Fragment key={index}>{part}</Fragment>
      ),
    );
}
