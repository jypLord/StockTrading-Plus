package com.jypLord.domain.user.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.jypLord.api.BrokerageFirm;
import lombok.Getter;

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
public class StockOAuthSaveRequest {
    private final BrokerageFirm firm;

    @JsonCreator
    public StockOAuthSaveRequest(@JsonProperty("firm") BrokerageFirm firm) {
        this.firm = firm;
    }
}
