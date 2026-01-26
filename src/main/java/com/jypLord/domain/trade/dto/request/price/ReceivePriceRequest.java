package com.jypLord.domain.trade.dto.request.price;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.jypLord.api.BrokerageFirm;
import lombok.AllArgsConstructor;
import lombok.Getter;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "firm",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = LsReceivePriceRequest.class, name = "LS")
})
@Getter
@AllArgsConstructor
public class ReceivePriceRequest {
    Long userId;
    BrokerageFirm firm;
}
