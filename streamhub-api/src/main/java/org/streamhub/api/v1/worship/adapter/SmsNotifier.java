package org.streamhub.api.v1.worship.adapter;

/**
 * SMS notification seam. The default {@link NoopSmsNotifier} never sends anything (demo/test
 * mode). A real implementation (e.g. Aligo/SOLAPI) can be injected later via
 * {@code app.worship.sms.provider} without changing {@code WorshipService}. The service
 * depends only on this interface, so swapping providers is a bean replacement, not a version
 * branch.
 */
public interface SmsNotifier {

    /** Notifies the applicant that their registration was received. */
    void notifyRegistrationReceived(String phone, String regNo);

    /** Notifies the applicant that an admin has made contact (status → CONTACTED). */
    void notifyContacted(String phone, String regNo);
}
