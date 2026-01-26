package com.jypLord.domain.user.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.jypLord.api.BrokerageFirm;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "firm",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = LsStockOAuthSaveRequest.class, name = "LS")
})
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StockOAuthSaveRequest {
    BrokerageFirm firm;
    Long userId;
}
