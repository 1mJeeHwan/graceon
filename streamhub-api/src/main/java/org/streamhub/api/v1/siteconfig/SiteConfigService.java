package org.streamhub.api.v1.siteconfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.actionlog.ActionLogPublisher;
import org.streamhub.api.v1.siteconfig.dto.SiteConfigData;
import org.streamhub.api.v1.siteconfig.entity.SiteConfig;
import org.streamhub.api.v1.siteconfig.repository.SiteConfigRepository;

/**
 * Reads and writes the singleton {@link SiteConfig}. The config is stored as a JSON blob and
 * (de)serialized here via the shared {@link ObjectMapper}; a missing row yields defaults so
 * the user site always has something to render.
 */
@Service
public class SiteConfigService {

    private final SiteConfigRepository repository;
    private final ObjectMapper objectMapper;
    private final ActionLogPublisher actionLogPublisher;

    public SiteConfigService(SiteConfigRepository repository, ObjectMapper objectMapper,
                             ActionLogPublisher actionLogPublisher) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.actionLogPublisher = actionLogPublisher;
    }

    /** Current config, or defaults when no row exists yet. */
    @Transactional(readOnly = true)
    public SiteConfigData get() {
        return repository.findById(SiteConfig.SINGLETON_ID)
                .map(row -> deserialize(row.getData()))
                .orElseGet(SiteConfigData::defaults);
    }

    /** Validates and persists the config (upserting the single row), returning the saved value. */
    @Transactional
    public SiteConfigData save(SiteConfigData data) {
        if (data == null) {
            throw new ApiException(ResultCode.INVALID_PARAMETER);
        }
        String json = serialize(data);
        SiteConfig row = repository.findById(SiteConfig.SINGLETON_ID).orElse(null);
        if (row == null) {
            repository.save(SiteConfig.builder().data(json).build());
        } else {
            row.update(json);
            repository.saveAndFlush(row);
        }
        actionLogPublisher.publish("SITE_CONFIG_UPDATE", "SITE_CONFIG", "1", data.getDefaultTheme());
        return data;
    }

    private SiteConfigData deserialize(String json) {
        try {
            return objectMapper.readValue(json, SiteConfigData.class);
        } catch (JsonProcessingException e) {
            // Corrupt/legacy blob — fall back to defaults rather than 500 the public site.
            return SiteConfigData.defaults();
        }
    }

    private String serialize(SiteConfigData data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new ApiException(ResultCode.INTERNAL_ERROR);
        }
    }
}
