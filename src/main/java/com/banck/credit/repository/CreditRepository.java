package com.banck.credit.repository;

import com.banck.credit.enums.CreditType;
import com.banck.credit.model.Credit;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CreditRepository extends ReactiveMongoRepository<Credit, String> {

    Flux<Credit> findByCustomerId(String customerId);

    Mono<Boolean> existsByCustomerIdAndCreditType(
            String customerId,
            CreditType creditType);
}
