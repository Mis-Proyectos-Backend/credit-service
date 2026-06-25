package com.banck.credit.service.Impl;

import com.banck.credit.client.CustomerClient;
import com.banck.credit.enums.CreditType;
import com.banck.credit.model.Credit;
import com.banck.credit.repository.CreditRepository;
import com.banck.credit.service.CreditService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class CreditServiceImpl implements CreditService {

    private final CreditRepository repository;
    private final CustomerClient customerClient;

    public CreditServiceImpl(CreditRepository repository,
                             CustomerClient customerClient) {
        this.repository = repository;
        this.customerClient = customerClient;
    }

    @Override
    public Mono<Credit> create(Credit credit) {

        return customerClient.getCustomerById(credit.getCustomerId())
                .flatMap(customer -> {

                    if ("PERSONAL".equals(customer.getCustomerType())
                            && credit.getCreditType() == CreditType.PERSONAL) {

                        return repository
                                .existsByCustomerIdAndCreditType(
                                        credit.getCustomerId(),
                                        CreditType.PERSONAL)
                                .flatMap(exists -> {

                                    if (exists) {
                                        return Mono.error(
                                                new RuntimeException(
                                                        "Customer already has a personal credit"));
                                    }

                                    credit.setCreatedAt(LocalDate.now());
                                    credit.setOutstandingBalance(
                                            credit.getAmount());

                                    return repository.save(credit);
                                });
                    }

                    credit.setCreatedAt(LocalDate.now());
                    credit.setOutstandingBalance(
                            credit.getAmount());

                    return repository.save(credit);
                });
    }

    @Override
    public Flux<Credit> findAll() {
        return repository.findAll();
    }

    @Override
    public Mono<Credit> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public Flux<Credit> findByCustomerId(String customerId) {
        return repository.findByCustomerId(customerId);
    }


    @Override
    public Mono<Credit> pay(String creditId, BigDecimal amount) {

        return repository.findById(creditId)
                .flatMap(credit -> {

                    BigDecimal newBalance =
                            credit.getOutstandingBalance()
                                    .subtract(amount);

                    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                        newBalance = BigDecimal.ZERO;
                    }

                    credit.setOutstandingBalance(newBalance);

                    return repository.save(credit);
                });
    }

    @Override
    public Mono<Void> delete(String id) {
        return repository.deleteById(id);
    }
}
