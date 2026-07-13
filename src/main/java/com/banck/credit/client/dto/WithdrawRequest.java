package com.banck.credit.client.dto;

import com.banck.credit.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawRequest {

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private PaymentMethod paymentMethod;
}