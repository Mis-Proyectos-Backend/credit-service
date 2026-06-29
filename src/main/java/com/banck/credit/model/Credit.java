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

    // Cliente propietario del crédito
    private String customerId;

    // PERSONAL, BUSINESS o CREDIT_CARD
    private CreditType creditType;

    // Monto aprobado del préstamo o límite de la tarjeta
    private BigDecimal creditLimit;

    // Deuda pendiente del crédito o tarjeta
    private BigDecimal outstandingBalance;

    // Fecha de creación
    private LocalDate createdAt;
}