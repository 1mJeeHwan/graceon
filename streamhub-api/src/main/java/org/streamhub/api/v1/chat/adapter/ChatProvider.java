package org.streamhub.api.v1.chat.adapter;

import java.util.List;

/**
 * Chatbot reply seam (C5). The default {@link RuleChatProvider} classifies intent with keyword
 * rules and answers from a static FAQ table + order/goods/feature-catalog queries — <b>no external
 * LLM call</b>. {@link LlmChatProvider} (Google Gemini, function calling) implements the same
 * interface and activates by config ({@code app.chat.provider=llm}), falling back to the rule
 * provider when no key is set or a call fails.
 *
 * <p>Providers receive the prior conversation turns so context-aware ones (the LLM) can remember
 * earlier messages; the rule provider ignores them.
 */
public interface ChatProvider {

    /** Provider code this implementation reports ({@code RULE} / {@code LLM}). */
    String code();

    /**
     * Produces a reply for the latest user message, given the prior conversation turns.
     *
     * @param message the raw user message
     * @param history prior turns (oldest first, current message excluded); may be empty
     * @return the bot reply (text + classified intent + quick replies)
     */
    ChatReply reply(String message, List<ChatTurn> history);
}
