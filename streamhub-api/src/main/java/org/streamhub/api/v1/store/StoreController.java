package org.streamhub.api.v1.store;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.store.dto.StoreDto;

/**
 * Offline-store management endpoints (SYSTEM or CHURCH_MANAGER). The public distance-
 * sorted store finder lives in {@code PublicController}.
 */
@Tag(name = "Store", description = "오프라인 직영매장 관리")
@RestController
@RequestMapping("/v1/store")
@PreAuthorize("hasAuthority('store:read')") // class default = read; mutations require store:write
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @Operation(summary = "매장 목록", description = "관리자용 전체 매장 목록(최신순).")
    @PostMapping("/list")
    public ResultDTO<List<StoreDto>> list() {
        return ResultDTO.ok(storeService.listAll());
    }

    @Operation(summary = "매장 등록")
    @PreAuthorize("hasAuthority('store:write')")
    @PostMapping
    public ResultDTO<StoreDto> create(@Valid @RequestBody StoreDto request) {
        return ResultDTO.ok(storeService.create(request));
    }

    @Operation(summary = "매장 수정")
    @PreAuthorize("hasAuthority('store:write')")
    @PutMapping("/{id}")
    public ResultDTO<StoreDto> update(@PathVariable Long id, @Valid @RequestBody StoreDto request) {
        return ResultDTO.ok(storeService.update(id, request));
    }

    @Operation(summary = "매장 삭제")
    @PreAuthorize("hasAuthority('store:write')")
    @DeleteMapping("/{id}")
    public ResultDTO<Void> delete(@PathVariable Long id) {
        storeService.delete(id);
        return ResultDTO.ok();
    }
}
