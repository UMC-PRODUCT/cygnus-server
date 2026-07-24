export function isVerificationExpired(expiresAt: string, now = Date.now()): boolean {
  return Date.parse(expiresAt) <= now;
}
