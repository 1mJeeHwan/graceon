package org.streamhub.api.v1.goods.dto;

/**
 * Result of a goods file upload.
 *
 * @param key storage object key (store this on the goods item/image)
 * @param url public URL for previewing the uploaded object
 */
public record UploadResponse(String key, String url) {
}
