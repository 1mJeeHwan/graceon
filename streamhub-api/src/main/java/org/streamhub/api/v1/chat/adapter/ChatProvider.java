package org.streamhub.api.v1.chat.adapter;

/**
 * Chatbot reply seam (C5). The default {@link RuleChatProvider} classifies intent with keyword
 * rules and answers from a static FAQ table + order/goods queries — <b>no external LLM call</b>.
 * {@link LlmChatProvider} implements the same interface; swapping to it is a config change
 * ({@code app.chat.provider=llm}), not a code branch.
 */
public interface ChatProvider {

    /** Provider code this implementation reports ({@code RULE} / {@code LLM}). */
    String code();

    /**
     * Produces a reply for a user message.
     *
     * @param message the raw user message
     * @return the bot reply (text + classified intent + quick replies)
     */
    ChatReply reply(String message);
}
