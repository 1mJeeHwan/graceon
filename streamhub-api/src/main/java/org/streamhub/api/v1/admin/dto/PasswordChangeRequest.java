package org.streamhub.api.v1.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Self-service password change for the signed-in operator.
 *
 * @param currentPassword the existing password, re-entered to prove the session belongs to the
 *                        account holder and not to someone who walked up to an unlocked console
 * @param newPassword     the replacement, minimum 10 characters
 */
public record PasswordChangeRequest(
        @NotBlank(message = "현재 비밀번호를 입력해주세요") String currentPassword,
        @NotBlank(message = "새 비밀번호를 입력해주세요")
        @Size(min = 10, max = 72, message = "새 비밀번호는 10자 이상이어야 합니다") String newPassword) {
}
