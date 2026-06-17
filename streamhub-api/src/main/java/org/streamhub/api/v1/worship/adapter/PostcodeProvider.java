package org.streamhub.api.v1.worship.adapter;

/**
 * Postcode/address seam. The default {@link MockPostcodeProvider} only normalises the
 * caller-supplied values (the public form's Daum/Kakao postcode widget already fills
 * {@code zipcode}/{@code addr1}). A real implementation can later enrich the address via an
 * external API, registered via {@code app.worship.postcode.provider}. No external call here.
 */
public interface PostcodeProvider {

    /**
     * Normalises/validates a postcode + base address.
     *
     * @param zipcode caller-supplied postcode (may be null/blank)
     * @param addr1   caller-supplied base address (may be null/blank)
     * @return normalised result
     */
    PostcodeResult resolve(String zipcode, String addr1);

    /**
     * Normalised postcode/address pair.
     *
     * @param zipcode normalised postcode
     * @param addr1   normalised base address
     */
    record PostcodeResult(String zipcode, String addr1) {
    }
}
