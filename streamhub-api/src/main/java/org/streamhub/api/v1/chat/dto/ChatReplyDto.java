package org.streamhub.api.v1.chat.dto;

import java.util.List;
import org.streamhub.api.v1.chat.adapter.ChatCard;
import org.streamhub.api.v1.chat.adapter.ChatReply;
import org.streamhub.api.v1.chat.entity.ChatIntent;

/**
 * Chat reply response (C5). Carries {@code testMode} so the widget can label a rule-based demo reply
 * honestly, plus optional rich-message {@code cards} (G) the widget renders as deep-link tiles.
 *
 * @param sessionKey   the (possibly newly issued) session id the client should store and replay
 * @param sessionToken the session's secret capability — store it and send it back to continue
 * @param text         bot reply text
 * @param intent       intent classified from this single message
 * @param quickReplies suggested follow-up buttons
 * @param cards        rich-message cards (product/content tiles); empty for plain replies
 * @param testMode     true for the rule provider (not a real LLM auto-response)
 */
public record ChatReplyDto(
        String sessionKey,
        String sessionToken,
        String text,
        ChatIntent intent,
        List<String> quickReplies,
        List<ChatCard> cards,
        boolean testMode) {

    /** Maps an adapter {@link ChatReply} to the API DTO, tagging the owning session's key+token. */
    public static ChatReplyDto of(ChatReply reply, boolean testMode, String sessionKey,
                                  String sessionToken) {
        return new ChatReplyDto(sessionKey, sessionToken, reply.text(), reply.intent(),
                reply.quickReplies(), reply.cards(), testMode);
    }
}
