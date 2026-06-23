package org.streamhub.api.base.iamport;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Iamport(포트원) payment lookup/cancel — the same REST integration ng-api uses
 * ({@code IamportService.checkPayInfo} / {@code cancel}). The browser SDK runs
 * {@code IMP.request_pay()} and yields an {@code imp_uid}; the server resolves that to the actual
 * payment record here and verifies it. The {@link IamportClient} is constructed even when the keys
 * are empty (no network at init), so the bean is safe to load while PortOne is dormant; the REST
 * calls only run when a PortOne payment is actually approved/cancelled.
 */
@Slf4j
@Service
public class IamportPaymentService {

    private final IamportProperty property;
    private IamportClient client;

    public IamportPaymentService(IamportProperty property) {
        this.property = property;
    }

    @PostConstruct
    void init() {
        client = new IamportClient(property.getApikey(), property.getSecret());
    }

    /** Resolves an {@code imp_uid} to its payment record, or empty if the lookup fails. */
    public Optional<Payment> findByImpUid(String impUid) {
        try {
            IamportResponse<Payment> result = client.paymentByImpUid(impUid);
            return Optional.ofNullable(result.getResponse());
        } catch (IamportResponseException | IOException e) {
            log.error("Iamport payment lookup failed for {}: {}", impUid, e.getMessage());
            return Optional.empty();
        }
    }

    /** Cancels (full refund) the payment for {@code impUid}, or empty if the cancel fails. */
    public Optional<Payment> cancel(String impUid, String reason) {
        CancelData data = new CancelData(impUid, true);
        if (reason != null && !reason.isBlank()) {
            data.setReason(reason);
        }
        try {
            IamportResponse<Payment> result = client.cancelPaymentByImpUid(data);
            return Optional.ofNullable(result.getResponse());
        } catch (IamportResponseException | IOException e) {
            log.error("Iamport cancel failed for {}: {}", impUid, e.getMessage());
            return Optional.empty();
        }
    }
}
