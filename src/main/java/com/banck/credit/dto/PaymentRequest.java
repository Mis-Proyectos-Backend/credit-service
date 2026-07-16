package com.banck.credit.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    /**
     * Cuenta desde la cual se realizará el pago.
     */
    private String accountId;

    /**
     * Monto que se desea pagar.
     */
    private BigDecimal amount;
}
