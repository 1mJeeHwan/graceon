package org.streamhub.api.v1.chat.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.streamhub.api.v1.chat.dto.ChatGoodsRow;
import org.streamhub.api.v1.chat.dto.ChatOrderRow;

/**
 * MyBatis mapper for chatbot tool queries (C5): order lookup (orderNo + name, both required —
 * cross-member leakage prevention, spec §3.5) and product keyword search. Maps to
 * {@code resources/mappers/ChatMapper.xml}. Bound parameters use {@code #{}} only (no string
 * concatenation).
 */
@Mapper
public interface ChatMapper {

    /** Looks up a single order by exact order number AND orderer name (both must match). */
    ChatOrderRow selectOrderByNoAndName(
            @Param("orderNo") String orderNo,
            @Param("orderedName") String orderedName);

    /** Top-N active products matching a keyword (name) for product-inquiry replies. */
    List<ChatGoodsRow> selectGoodsByKeyword(
            @Param("keyword") String keyword,
            @Param("size") int size);
}
