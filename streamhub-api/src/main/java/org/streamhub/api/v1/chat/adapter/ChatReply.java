package org.streamhub.api.v1.chat.adapter;

import java.util.List;
import org.streamhub.api.v1.chat.entity.ChatIntent;

/**
 * A bot reply produced by a {@link ChatProvider} (C5 seam).
 *
 * @param text         the reply body
 * @param intent       the classified intent (recorded on the BOT message)
 * @param quickReplies suggested follow-up buttons (may be empty)
 */
public record ChatReply(String text, ChatIntent intent, List<String> quickReplies) {

    /** A reply with no quick-reply buttons. */
    public static ChatReply of(String text, ChatIntent intent) {
        return new ChatReply(text, intent, List.of());
    }
}
