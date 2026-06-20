package org.streamhub.api.v1.chat.adapter;

import org.streamhub.api.v1.chat.entity.ChatRole;

/**
 * One prior turn of a conversation, passed to {@link ChatProvider#reply(String, java.util.List)}
 * so context-aware providers (the LLM) can remember earlier messages. The rule provider ignores it.
 *
 * @param role    who authored the turn (USER / BOT)
 * @param content the message text
 */
public record ChatTurn(ChatRole role, String content) {
}
