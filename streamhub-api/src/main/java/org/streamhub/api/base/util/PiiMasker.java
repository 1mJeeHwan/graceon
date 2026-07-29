package org.streamhub.api.base.util;

/**
 * Masks personal data for list/monitoring views.
 *
 * <p>Exists because the console has a read-only {@code VIEWER} role that the README publishes as a
 * public demo account. Read-only is not the same as harmless: a browse-everything role that returns
 * raw member emails, phone numbers, and the login ids seen in failed sign-in attempts is a
 * confidentiality leak even though it can mutate nothing. Masking the response is the right lever —
 * narrowing the role would break the demo the account exists for.
 *
 * <p>Kept deliberately dumb: format-preserving, null-safe, and lossy in one direction only. Detail
 * screens that a privileged operator genuinely needs (a single member's record) keep full values;
 * only the wide list surfaces are masked.
 */
public final class PiiMasker {

    private PiiMasker() {
    }

    /**
     * {@code hong@example.com} → {@code ho**@example.com}. Keeps the domain (operators sort and
     * triage by it) and enough of the local part to recognize a row you already know, while making
     * the address useless for harvesting.
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return maskTail(email, 2);
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        return maskTail(local, 2) + domain;
    }

    /** {@code 010-1234-5678} → {@code 010-****-5678}; digits-only input is masked the same way. */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 7) {
            return maskTail(phone, 3);
        }
        String head = digits.substring(0, 3);
        String tail = digits.substring(digits.length() - 4);
        return head + "-****-" + tail;
    }

    /**
     * {@code hong@example.com} / {@code admin} → first two characters plus asterisks. Used for the
     * attempted login id on security events, which is an arbitrary caller-supplied string (often a
     * real member's email) rather than a known-format field.
     */
    public static String maskLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return loginId;
        }
        int at = loginId.indexOf('@');
        return at > 0 ? maskEmail(loginId) : maskTail(loginId, 2);
    }

    /** {@code 211.34.56.78} → {@code 211.34.*.*}; IPv6 keeps its first two hextets. */
    public static String maskIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return ip;
        }
        String value = ip.trim();
        if (value.contains(":")) {
            String[] hextets = value.split(":");
            return hextets.length < 2 ? "*:*" : hextets[0] + ":" + hextets[1] + ":*:*";
        }
        String[] octets = value.split("\\.");
        return octets.length < 4 ? "*.*" : octets[0] + "." + octets[1] + ".*.*";
    }

    /** Keeps the first {@code keep} characters and replaces the rest with asterisks (min two). */
    private static String maskTail(String value, int keep) {
        if (value.length() <= keep) {
            return "*".repeat(Math.max(2, value.length()));
        }
        return value.substring(0, keep) + "*".repeat(value.length() - keep);
    }
}
