package org.streamhub.api.base.iamport;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Iamport(포트원) credentials. {@code imp} is the public merchant code surfaced to the browser
 * SDK; {@code apikey}/{@code secret} are the REST credentials used server-side. Mirrors ng-api's
 * config (the committed defaults are 포트원's public KG이니시스 test merchant — test cert only, no
 * real charge); override via {@code IAMPORT_*} env in production.
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "iamport")
public class IamportProperty {

    private String imp;
    private String apikey;
    private String secret;
}
