package com.banck.credit.client.dto;

import com.banck.credit.enums.CustomerProfile;
import com.banck.credit.enums.CustomerType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Customer {

    private String id;

    private String name;

    @JsonProperty("document")
    private String documentNumber;

    @JsonProperty("type")
    private CustomerType customerType;

    @JsonProperty("customerProfile")
    private CustomerProfile customerProfile;
}
