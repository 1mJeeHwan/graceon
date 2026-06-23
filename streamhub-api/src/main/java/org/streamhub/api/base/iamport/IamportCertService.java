package org.streamhub.api.base.iamport;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.response.Certification;
import com.siot.IamportRestClient.response.IamportResponse;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Iamport(포트원) identity-verification (휴대폰 본인인증) lookup — same integration as ng-api's
 * {@code IamportService.certificate}. The browser SDK runs {@code IMP.certification()} and yields an
 * {@code imp_uid}; this service resolves that to the verified identity (name/phone/CI) via the REST API.
 */
@Slf4j
@Service
public class IamportCertService {

    private final IamportProperty property;
    private IamportClient client;

    public IamportCertService(IamportProperty property) {
        this.property = property;
    }

    @PostConstruct
    void init() {
        client = new IamportClient(property.getApikey(), property.getSecret());
    }

    /** Resolves an {@code imp_uid} to its certified identity, or empty if the lookup fails. */
    public Optional<Certification> certificate(String impUid) {
        try {
            IamportResponse<Certification> result = client.certificationByImpUid(impUid);
            return Optional.ofNullable(result.getResponse());
        } catch (IamportResponseException | IOException e) {
            log.error("Iamport certification lookup failed for {}: {}", impUid, e.getMessage());
            return Optional.empty();
        }
    }
}
