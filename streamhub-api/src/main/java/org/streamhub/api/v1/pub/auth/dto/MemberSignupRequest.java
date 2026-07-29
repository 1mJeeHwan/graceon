package org.streamhub.api.v1.pub.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Member sign-up. The phone number is stored as supplied — <b>it is not verified</b>. An earlier design routed sign-up through a
 * one-time SMS confirmation and this comment still referenced that endpoint long after it was
 * dropped, which reads as a guarantee the code does not make. Anything downstream that trusts this
 * number (order notifications, SMS dispatch) is trusting unvalidated input; restoring verification
 * means a Redis-held one-time flag consumed inside the sign-up transaction, not a comment.
 * The two mandatory consents ({@code agreeTerms}, {@code agreePrivacy}) are enforced both here
 * and on the client; {@code agreeMarketing} is optional and persisted on the member.
 */
public record MemberSignupRequest(
        @NotBlank(message = "이메일을 입력해 주세요")
        @Email(message = "올바른 이메일 형식이 아닙니다") String email,

        @NotBlank(message = "비밀번호를 입력해 주세요")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상이어야 합니다") String password,

        @NotBlank(message = "이름을 입력해 주세요")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다") String name,

        @NotBlank(message = "휴대폰 인증이 필요합니다") String phone,

        @AssertTrue(message = "이용약관에 동의해 주세요") boolean agreeTerms,
        @AssertTrue(message = "개인정보 수집·이용에 동의해 주세요") boolean agreePrivacy,
        boolean agreeMarketing) {
}
