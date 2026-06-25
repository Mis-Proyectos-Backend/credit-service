package com.banck.credit.model;

import com.banck.credit.enums.CreditType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credits")
public class Credit {

    @Id
    private String id;

    private String customerId;

    private CreditType creditType;

    private BigDecimal amount;

    private BigDecimal outstandingBalance;

    private LocalDate createdAt;
}