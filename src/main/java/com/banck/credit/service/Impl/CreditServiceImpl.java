package com.banck.credit.service.Impl;

import com.banck.credit.client.AccountClient;
import com.banck.credit.client.CustomerClient;
import com.banck.credit.enums.CreditType;
import com.banck.credit.enums.CustomerType;
import com.banck.credit.model.Credit;
import com.banck.credit.repository.CreditRepository;
import com.banck.credit.service.CreditService;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class CreditServiceImpl implements CreditService {

    private final CreditRepository repository;
    private final CustomerClient customerClient;
    private final AccountClient accountClient;
    private final ReactiveRedisTemplate<String, Credit> redisTemplate;


    public CreditServiceImpl(CreditRepository repository,
                             CustomerClient customerClient,
                             AccountClient accountClient,
                             ReactiveRedisTemplate<String, Credit> redisTemplate) {
        this.repository = repository;
        this.customerClient = customerClient;
        this.accountClient = accountClient;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Credit> create(Credit credit) {

        return customerClient.getCustomerById(credit.getCustomerId())
                .flatMap(customer -> {
                    if (customer.getCustomerType() == CustomerType.PERSONAL
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

                                    return saveCredit(credit);
                                });
                    }
                    return saveCredit(credit);
                });
    }

    private Mono<Credit> saveCredit(Credit credit) {
        credit.setCreatedAt(LocalDate.now());
        if (credit.getCreditType() == CreditType.CREDIT_CARD) {
            credit.setOutstandingBalance(BigDecimal.ZERO);
        } else {
            credit.setOutstandingBalance(credit.getCreditLimit());
        }
        return repository.save(credit)
                .flatMap(saved ->
                        redisTemplate.opsForValue()
                                .set("credit:" + saved.getId(), saved)
                                .thenReturn(saved)
                );
    }

    @Override
    public Flux<Credit> findAll() {
        return repository.findAll();
    }

    @Override
    public Mono<Credit> findById(String id) {

        String key = "credit:" + id;

        return redisTemplate.opsForValue()
                .get(key)
                .switchIfEmpty(
                        repository.findById(id)
                                .switchIfEmpty(
                                        Mono.error(new RuntimeException("Credit not found"))
                                )
                                .flatMap(credit ->
                                        redisTemplate.opsForValue()
                                                .set(key, credit)
                                                .thenReturn(credit)
                                )
                );
    }

    @Override
    public Flux<Credit> findByCustomerId(String customerId) {
        return repository.findByCustomerId(customerId);
    }

    @Override
    public Mono<Credit> consume(String creditId, BigDecimal transactionAmount) {

        if (transactionAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(
                    new IllegalArgumentException("Amount must be greater than zero"));
        }

        return repository.findById(creditId)
                .switchIfEmpty(
                        Mono.error(new RuntimeException("Credit not found")))
                .flatMap(credit -> {

                    // Solo las tarjetas de crédito permiten consumos
                    if (credit.getCreditType() != CreditType.CREDIT_CARD) {
                        return Mono.error(new RuntimeException("Only credit cards allow consumption"));
                    }

                    // Crédito disponible
                    BigDecimal available = credit.getCreditLimit()
                            .subtract(credit.getOutstandingBalance());

                    // Validar que no exceda el límite
                    if (transactionAmount.compareTo(available) > 0) {
                        return Mono.error(
                                new RuntimeException("Credit limit exceeded"));
                    }

                    // Registrar el consumo (aumenta la deuda)
                    credit.setOutstandingBalance(
                            credit.getOutstandingBalance().add(transactionAmount));

                    return repository.save(credit)
                            .flatMap(saved ->
                                    redisTemplate.delete("credit:" + saved.getId())
                                            .thenReturn(saved)
                            );
                });
    }

    @Override
    public Mono<Void> delete(String id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Credit not found")))
                .flatMap(credit ->
                        repository.delete(credit)
                                .then(redisTemplate.delete("credit:" + credit.getId()))
                                .then()
                );
    }

    @Override
    public Mono<Credit> payCredit(String creditId, String accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(
                    new IllegalArgumentException(
                            "Amount must be greater than zero"));
        }

        return repository.findById(creditId)

                .switchIfEmpty(
                        Mono.error(new RuntimeException("Credit not found")))

                .flatMap(credit ->
                        validatePayment(accountId, amount)

                                .flatMap(valid -> {

                                    if (!valid) {
                                        return Mono.error(
                                                new RuntimeException(
                                                        "Insufficient balance"));
                                    }

                                    return executePayment(
                                            accountId,
                                            amount,
                                            credit);
                                }));
    }

    private Mono<Boolean> validatePayment(String accountId, BigDecimal amount) {
        return accountClient.getAccountsByCustomer(accountId)
                .any(account ->
                        account.getId().equals(accountId)
                                && account.getBalance()
                                .compareTo(amount) >= 0);
    }

    private Mono<Credit> executePayment(String accountId,
                                        BigDecimal amount,
                                        Credit credit) {

        return accountClient.withdraw(accountId, amount)

                .flatMap(account -> applyPayment(credit, amount));
    }

    private Mono<Credit> applyPayment(Credit credit,
                                      BigDecimal amount) {
        BigDecimal newBalance =
                credit.getOutstandingBalance().subtract(amount);
        credit.setOutstandingBalance(newBalance);
        return repository.save(credit)
                .flatMap(saved ->
                        redisTemplate.delete("credit:" + saved.getId())
                                .thenReturn(saved)
                );
    }
}
