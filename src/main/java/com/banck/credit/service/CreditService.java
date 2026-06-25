package com.banck.credit.service;

import com.banck.credit.model.Credit;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface CreditService {

    Mono<Credit> create(Credit credit);

    Flux<Credit> findAll();

    Mono<Credit> findById(String id);

    Flux<Credit> findByCustomerId(String customerId);

    Mono<Credit> pay(String creditId, BigDecimal amount);

    Mono<Void> delete(String id);
}