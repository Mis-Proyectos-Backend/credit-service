package com.banck.credit.client.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class Account {

    private String id;
    private String accountNumber;
    private String customerId;
    private String type;
    private BigDecimal balance;
}
