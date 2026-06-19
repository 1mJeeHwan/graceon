package org.streamhub.api.v1.chat.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.chat.admin.dto.ChatMessageRow;
import org.streamhub.api.v1.chat.admin.dto.ChatReplyRequest;
import org.streamhub.api.v1.chat.admin.dto.ChatSessionRow;

/**
 * Admin chat-console endpoints (SYSTEM or CHURCH_MANAGER). Lets operators browse chatbot
 * sessions, read the full thread, and post a manual reply. Separate from the public widget
 * controller ({@code ChatController}), which stays {@code permitAll}.
 */
@Tag(name = "ChatAdmin", description = "관리자 챗봇 상담 콘솔")
@RestController
@RequestMapping("/v1/chat-admin")
@PreAuthorize("hasAuthority('chat:read')") // class default = read; mutations require chat:write
public class ChatAdminController {

    private final ChatAdminService chatAdminService;

    public ChatAdminController(ChatAdminService chatAdminService) {
        this.chatAdminService = chatAdminService;
    }

    @Operation(summary = "상담 세션 목록", description = "최근 활동순 세션 목록. 마지막 메시지/미응답 여부 포함.")
    @PostMapping("/sessions")
    public ResultDTO<List<ChatSessionRow>> sessions() {
        return ResultDTO.ok(chatAdminService.listSessions());
    }

    @Operation(summary = "세션 대화 이력", description = "세션키 기준 전체 대화 스레드(오래된 순).")
    @GetMapping("/sessions/{sessionKey}/messages")
    public ResultDTO<List<ChatMessageRow>> messages(@PathVariable String sessionKey) {
        return ResultDTO.ok(chatAdminService.messages(sessionKey));
    }

    @Operation(summary = "상담원 수동 답변", description = "해당 세션에 운영자(BOT) 답변을 추가한다.")
    @PreAuthorize("hasAuthority('chat:write')")
    @PostMapping("/sessions/{sessionKey}/reply")
    public ResultDTO<ChatMessageRow> reply(
            @PathVariable String sessionKey, @Valid @RequestBody ChatReplyRequest request) {
        return ResultDTO.ok(chatAdminService.reply(sessionKey, request));
    }
}
