package org.streamhub.api.v1.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.streamhub.api.auth.AuthService;
import org.streamhub.api.base.exception.ApiException;
import org.streamhub.api.base.response.ResultCode;
import org.streamhub.api.v1.admin.dto.PasswordChangeRequest;
import org.streamhub.api.v1.admin.entity.AdminAccount;
import org.streamhub.api.v1.admin.repository.AdminAccountRepository;
import org.streamhub.api.v1.security.SecurityMonitor;

/**
 * Operator self-service account actions.
 *
 * <p>Exists so the seeded bootstrap credentials can actually be rotated from the console — the
 * deploy runbook has always told operators to do that, and until now there was no endpoint that
 * made it possible.
 */
@Slf4j
@Service
public class AdminAccountService {

    private final AdminAccountRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final SecurityMonitor securityMonitor;

    public AdminAccountService(AdminAccountRepository adminRepository,
                               PasswordEncoder passwordEncoder,
                               AuthService authService,
                               SecurityMonitor securityMonitor) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.securityMonitor = securityMonitor;
    }

    /**
     * Changes the signed-in operator's password after re-verifying the current one, then revokes
     * the stored refresh token so the old session cannot be renewed. The in-flight access token
     * stays valid until it expires — a deliberate limit of the stateless-JWT design, bounded by
     * {@code jwt.access-exp-seconds}.
     *
     * @throws ApiException {@link ResultCode#LOGIN_FAILED} when the current password is wrong,
     *                      {@link ResultCode#INVALID_PARAMETER} when the new password equals the old
     */
    @Transactional
    public void changePassword(Long adminId, PasswordChangeRequest request) {
        AdminAccount admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new ApiException(ResultCode.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.currentPassword(), admin.getPassword())) {
            securityMonitor.record("PASSWORD_CHANGE_FAILED", "MEDIUM", "ADMIN", adminId,
                    admin.getLoginId(), "/v1/admin/me/password", "wrong current password");
            throw new ApiException(ResultCode.LOGIN_FAILED);
        }
        if (passwordEncoder.matches(request.newPassword(), admin.getPassword())) {
            throw new ApiException(ResultCode.INVALID_PARAMETER, "새 비밀번호가 기존 비밀번호와 같습니다");
        }

        admin.changePassword(passwordEncoder.encode(request.newPassword()));
        adminRepository.save(admin);
        authService.revokeRefreshToken(adminId);
        log.info("Password changed for operator {}", admin.getLoginId());
    }
}
