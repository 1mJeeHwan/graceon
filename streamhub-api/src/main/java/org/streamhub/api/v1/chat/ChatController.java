package org.streamhub.api.v1.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.streamhub.api.base.response.ResultDTO;
import org.streamhub.api.v1.chat.dto.ChatHistoryItem;
import org.streamhub.api.v1.chat.dto.ChatReplyDto;
import org.streamhub.api.v1.chat.dto.ChatSendRequest;

/**
 * Public chatbot widget endpoints (C5). Demo/rule-based — no LLM call, {@code testMode=true}.
 *
 * <p><b>Security note:</b> per spec §3.2 these endpoints are {@code permitAll} (public widget),
 * so {@code /v1/chat/**} must be added to {@code SecurityConfig.PUBLIC_PATHS}. No class-level
 * {@code @PreAuthorize} is declared here on purpose. Order lookup itself is protected by
 * requiring both order number and orderer name (spec §3.5), so no anonymous enumeration is
 * possible even though the endpoint is open.
 */
@Tag(name = "Chat", description = "공개 챗봇 위젯 (데모/룰베이스, 인증 불필요)")
@RestController
@RequestMapping("/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "챗봇 메시지 전송", description = "사용자 메시지를 분류해 룰베이스 응답을 반환한다(데모).")
    @PostMapping("/send")
    public ResultDTO<ChatReplyDto> send(@Valid @RequestBody ChatSendRequest request) {
        return ResultDTO.ok(chatService.send(request));
    }

    @Operation(summary = "챗봇 대화 이력", description = "세션키 기준 전체 대화 이력(오래된 순).")
    @GetMapping("/{sessionKey}/history")
    public ResultDTO<List<ChatHistoryItem>> history(@PathVariable String sessionKey) {
        return ResultDTO.ok(chatService.history(sessionKey));
    }
}
