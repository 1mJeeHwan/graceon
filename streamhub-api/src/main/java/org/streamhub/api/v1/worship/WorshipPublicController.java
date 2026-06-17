package org.streamhub.api.v1.worship;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.worship.dto.ChurchOptionDto;
import org.streamhub.api.v1.worship.dto.WorshipRegisterRequest;
import org.streamhub.api.v1.worship.dto.WorshipRegisterResponse;

/**
 * Public, unauthenticated worship/new-family registration endpoints consumed by the user
 * site ({@code streamhub-user-web}). Mapped under {@code /pub/**}, which is permitAll in
 * {@link org.streamhub.api.base.security.SecurityConfig} (no SecurityConfig change needed).
 * Demo/test mode: every submission is marked {@code test_mode='Y'} and no real notification
 * is dispatched.
 */
@Tag(name = "WorshipPublic", description = "예배·새가족 공개 신청 (인증 불필요, 데모/테스트 모드)")
@RestController
@RequestMapping("/pub/v1/worship")
public class WorshipPublicController {

    private final WorshipService worshipService;

    public WorshipPublicController(WorshipService worshipService) {
        this.worshipService = worshipService;
    }

    @Operation(summary = "공개 신청 생성",
            description = "비회원 다단계 폼 제출 → 신청 1건 + 가족 N건(최대 5) 생성. 실제 알림 미발송.")
    @PostMapping
    public ResultDTO<WorshipRegisterResponse> create(@Valid @RequestBody WorshipRegisterRequest request) {
        return ResultDTO.ok(worshipService.create(request));
    }

    @Operation(summary = "공개 교회 목록", description = "신청 폼의 교회 선택용 (openYn='Y'만).")
    @GetMapping("/churches")
    public ResultDTO<List<ChurchOptionDto>> churches() {
        return ResultDTO.ok(worshipService.listOpenChurches());
    }
}
