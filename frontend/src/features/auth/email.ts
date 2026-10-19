// Client-side email check only blocks obvious typos; the server checks again (DR-21, FL-01 E1).

const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
const MAX_LENGTH = 254;

/** Trimmed and lower-cased, as the server stores it. */
export function normalizeEmail(value: string): string {
  return value.trim().toLowerCase();
}

export function isValidEmail(normalized: string): boolean {
  return normalized.length <= MAX_LENGTH && EMAIL.test(normalized);
}

const WEBMAIL: Record<string, string> = {
  "gmail.com": "https://mail.google.com",
  "outlook.com": "https://outlook.live.com/mail",
  "hotmail.com": "https://outlook.live.com/mail",
  "live.com": "https://outlook.live.com/mail",
  "yahoo.com": "https://mail.yahoo.com",
  "icloud.com": "https://www.icloud.com/mail",
};

/** "Open inbox" target (DOC-44 §6.1, DR-138): Mailpit in dev, known webmail, otherwise none. */
export function webmailUrl(email: string, mailpitUrl: string | null): string | null {
  if (mailpitUrl) return mailpitUrl;
  const domain = email.slice(email.lastIndexOf("@") + 1);
  return WEBMAIL[domain] ?? null;
}
