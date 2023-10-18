package com.zz.matchsystem.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.zz.matchsystem.serialize.IdCodeToEnumDeserializer;
import com.zz.matchsystem.trader.ExchangeOrderDirection;
import com.zz.matchsystem.trader.ExchangeOrderType;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author：EvilSay
 * @Date：18.10.23 00:17
 * @description:
 */
@Data
public class OrderRequest {
    @JsonDeserialize(using = IdCodeToEnumDeserializer.class)
    private ExchangeOrderDirection direction;
    private String symbol;
    private BigDecimal price;
    private BigDecimal amount;
    @JsonDeserialize(using = IdCodeToEnumDeserializer.class)
    private ExchangeOrderType type;
}
