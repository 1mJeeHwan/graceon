package org.streamhub.api.v1.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A single product row returned to the chatbot's product-inquiry tool (C5). */
@Getter
@Setter
@NoArgsConstructor
public class ChatGoodsRow {
    private Long id;
    private String name;
    private Long price;
    private Integer stock;
    private String soldOut;
}
