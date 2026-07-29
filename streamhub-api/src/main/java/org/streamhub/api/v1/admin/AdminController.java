package org.streamhub.api.v1.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.base.security.AdminPrincipal;
import org.streamhub.api.v1.admin.dto.MeResponse;
import org.streamhub.api.v1.admin.dto.PasswordChangeRequest;
import org.streamhub.api.v1.admin.entity.AdminAccount;
import org.streamhub.api.v1.admin.repository.AdminAccountRepository;

/**
 * Operator account endpoints. {@code /v1/admin/me} doubles as the protected-route probe
 * proving JWT auth works end-to-end.
 */
@Tag(name = "Admin", description = "운영자 계정")
@RestController
@RequestMapping("/v1/admin")
public class AdminController {

    private final AdminAccountRepository adminRepository;
    private final AdminAccountService adminAccountService;

    public AdminController(AdminAccountRepository adminRepository,
                           AdminAccountService adminAccountService) {
        this.adminRepository = adminRepository;
        this.adminAccountService = adminAccountService;
    }

    @Operation(summary = "내 정보", description = "현재 인증된 운영자 정보를 반환한다.")
    @GetMapping("/me")
    public ResultDTO<MeResponse> me(@AuthenticationPrincipal AdminPrincipal principal) {
        AdminAccount admin = adminRepository.findById(principal.id())
                .orElseThrow(() -> new ApiException(ResultCode.UNAUTHORIZED));
        return ResultDTO.ok(MeResponse.from(admin));
    }

    @Operation(summary = "비밀번호 변경",
            description = "현재 비밀번호 확인 후 변경한다. 변경 시 저장된 refresh 토큰이 폐기되어 재로그인이 필요하다. "
                    + "권한 검사 없이 본인 계정만 대상이므로 VIEWER 데모 계정도 자기 비밀번호는 바꿀 수 있다.")
    @PostMapping("/me/password")
    public ResultDTO<Void> changePassword(@AuthenticationPrincipal AdminPrincipal principal,
                                          @Valid @RequestBody PasswordChangeRequest request) {
        adminAccountService.changePassword(principal.id(), request);
        return ResultDTO.ok();
    }
}
