package org.streamhub.api.v1.payment.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siot.IamportRestClient.response.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.iamport.IamportPaymentService;
import org.streamhub.api.base.iamport.IamportProperty;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.order.entity.PayStatus;

@ExtendWith(MockitoExtension.class)
class PortonePaymentProviderTest {

    @Mock
    private IamportPaymentService iamport;

    private IamportProperty property;
    private PortonePaymentProvider provider;

    private final PaymentRequest request = new PaymentRequest("20260623-000001", 10000L, "PORTONE");

    @BeforeEach
    void setUp() {
        property = new IamportProperty();
        property.setApikey("k");
        property.setSecret("s");
        provider = new PortonePaymentProvider(iamport, property);
    }

    private static final ObjectMapper MAPPER =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private Payment payment(String status, long amount) throws Exception {
        String json = "{\"status\":\"" + status + "\",\"amount\":" + amount + ",\"pay_method\":\"card\"}";
        return MAPPER.readValue(json, Payment.class);
    }

    @Test
    void approve_paidMatchingAmount_returnsApproved() throws Exception {
        when(iamport.findByImpUid("imp_1")).thenReturn(Optional.of(payment("paid", 10000L)));

        PaymentResult result = provider.approve(request, null, "imp_1", null);

        assertThat(result.status()).isEqualTo(PayStatus.APPROVED);
        assertThat(result.txnId()).isEqualTo("imp_1");
        assertThat(result.amount()).isEqualTo(10000L);
    }

    @Test
    void approve_amountMismatch_throws() throws Exception {
        when(iamport.findByImpUid("imp_1")).thenReturn(Optional.of(payment("paid", 9999L)));

        assertThatThrownBy(() -> provider.approve(request, null, "imp_1", null))
                .isInstanceOf(ApiException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.INVALID_PARAMETER);
    }

    @Test
    void approve_notPaid_throws() throws Exception {
        when(iamport.findByImpUid("imp_1")).thenReturn(Optional.of(payment("ready", 10000L)));

        assertThatThrownBy(() -> provider.approve(request, null, "imp_1", null))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void approve_noKeys_throws() {
        property.setApikey("");
        assertThatThrownBy(() -> provider.approve(request, null, "imp_1", null))
                .isInstanceOf(ApiException.class)
                .extracting("resultCode")
                .isEqualTo(ResultCode.INVALID_PARAMETER);
    }
}
