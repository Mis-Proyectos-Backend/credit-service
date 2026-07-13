package com.banck.credit.service.Impl;

import com.banck.credit.client.AccountClient;
import com.banck.credit.client.CustomerClient;
import com.banck.credit.client.dto.Account;
import com.banck.credit.client.dto.Customer;
import com.banck.credit.config.CreditProperties;
import com.banck.credit.enums.CreditType;
import com.banck.credit.enums.CustomerType;
import com.banck.credit.model.Credit;
import com.banck.credit.repository.CreditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


class CreditServiceImplTest {

    private CreditRepository repository;
    private CustomerClient customerClient;
    private AccountClient accountClient;
    private CreditServiceImpl service;

    private ReactiveRedisTemplate<String, Credit> redisTemplate;
    private ReactiveValueOperations<String, Credit> valueOperations;

    private CreditProperties creditProperties;


    @BeforeEach
    void setUp() {

        repository = Mockito.mock(CreditRepository.class);
        customerClient = Mockito.mock(CustomerClient.class);
        accountClient = Mockito.mock(AccountClient.class);

        redisTemplate = Mockito.mock(ReactiveRedisTemplate.class);
        valueOperations = Mockito.mock(ReactiveValueOperations.class);

        creditProperties = Mockito.mock(CreditProperties.class);

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);
        when(creditProperties.getDueDays())
                .thenReturn(30);
        service = new CreditServiceImpl(
                repository,
                customerClient,
                accountClient,
                redisTemplate,
                creditProperties
        );
    }


    @Test
    void create_personalCredit_shouldSaveCredit() {
        Credit credit = Credit.builder()
                .customerId("c1")
                .creditType(CreditType.PERSONAL)
                .creditLimit(BigDecimal.valueOf(1000))
                .build();
        Customer customer = Customer.builder()
                .customerType(CustomerType.PERSONAL)
                .build();
        when(repository.findByCustomerId("c1"))
                .thenReturn(Flux.empty());

        when(customerClient.getCustomerById("c1"))
                .thenReturn(Mono.just(customer));

        when(repository.existsByCustomerIdAndCreditType(
                "c1",
                CreditType.PERSONAL))
                .thenReturn(Mono.just(false));

        when(repository.save(any(Credit.class)))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));

        when(valueOperations.set(anyString(), any(Credit.class)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(service.create(credit))
                .expectNextMatches(result ->
                        result.getOutstandingBalance()
                                .equals(BigDecimal.valueOf(1000))
                                &&
                                result.getDueDate()
                                        .equals(LocalDate.now().plusDays(30))
                )
                .verifyComplete();
    }

    @Test
    void create_personalCredit_whenAlreadyExists_shouldFail() {
        Credit credit = Credit.builder()
                .customerId("c1")
                .creditType(CreditType.PERSONAL)
                .build();
        Customer customer = Customer.builder()
                .customerType(CustomerType.PERSONAL)
                .build();
        when(customerClient.getCustomerById("c1"))
                .thenReturn(Mono.just(customer));
        when(repository.existsByCustomerIdAndCreditType(
                "c1",
                CreditType.PERSONAL
        ))
                .thenReturn(Mono.just(true));
        when(repository.findByCustomerId("c1"))
                .thenReturn(Flux.empty());
        StepVerifier.create(service.create(credit))
                .expectErrorMessage(
                        "Customer already has a personal credit"
                )
                .verify();
    }

    @Test
    void findAll_shouldReturnCredits() {
        Credit credit = Credit.builder()
                .id("cr1")
                .build();
        when(repository.findAll())
                .thenReturn(Flux.just(credit));

        StepVerifier.create(service.findAll())
                .expectNext(credit)
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnCredit() {

        Credit credit = Credit.builder()
                .id("cr1")
                .build();

        when(valueOperations.get("credit:cr1"))
                .thenReturn(Mono.just(credit));
        // Aunque no debería usarse, debe devolver un Mono y no null
        when(repository.findById("cr1"))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.findById("cr1"))
                .expectNext(credit)
                .verifyComplete();
    }

    @Test
    void consume_creditCard_shouldIncreaseDebt() {
        Credit credit = Credit.builder()
                .id("cr1")
                .creditType(CreditType.CREDIT_CARD)
                .creditLimit(BigDecimal.valueOf(1000))
                .outstandingBalance(BigDecimal.ZERO)
                .build();
        when(repository.findById("cr1"))
                .thenReturn(Mono.just(credit));
        when(repository.save(any(Credit.class)))
                .thenAnswer(i ->
                        Mono.just(i.getArgument(0))
                );
        when(redisTemplate.delete("credit:cr1"))
                .thenReturn(Mono.just(1L));
        StepVerifier.create(
                        service.consume(
                                "cr1",
                                BigDecimal.valueOf(100)
                        )
                )

                .expectNextMatches(result ->
                        result.getOutstandingBalance()
                                .equals(BigDecimal.valueOf(100))
                )
                .verifyComplete();
    }

    @Test
    void consume_whenAmountInvalid_shouldFail() {
        StepVerifier.create(
                        service.consume(
                                "cr1",
                                BigDecimal.ZERO
                        )
                )

                .expectErrorMessage(
                        "Amount must be greater than zero"
                )
                .verify();
    }

    @Test
    void consume_nonCreditCard_shouldFail() {
        Credit credit = Credit.builder()
                .id("cr1")
                .creditType(CreditType.PERSONAL)
                .build();
        when(repository.findById("cr1"))
                .thenReturn(Mono.just(credit));
        StepVerifier.create(
                        service.consume(
                                "cr1",
                                BigDecimal.valueOf(100)
                        )
                )

                .expectErrorMessage(
                        "Only credit cards allow consumption"
                )
                .verify();
    }

    @Test
    void payCredit_shouldPayCredit() {
        Credit credit = Credit.builder()
                .id("cr1")
                .outstandingBalance(BigDecimal.valueOf(500))
                .build();
        when(repository.findById("cr1"))
                .thenReturn(Mono.just(credit));


        when(accountClient.getAccountById("a1"))
                .thenReturn(
                        Mono.just(
                                Account.builder()
                                        .id("a1")
                                        .balance(BigDecimal.valueOf(1000))
                                        .build()
                        )
                );
        when(accountClient.withdraw(
                "a1",
                BigDecimal.valueOf(100)
        ))
                .thenReturn(
                        Mono.just(
                                Account.builder()
                                        .id("a1")
                                        .balance(BigDecimal.valueOf(900))
                                        .build()
                        )
                );
        when(repository.save(any(Credit.class)))
                .thenAnswer(i ->
                        Mono.just(i.getArgument(0))
                );
        when(redisTemplate.delete("credit:cr1"))
                .thenReturn(Mono.just(1L));
        StepVerifier.create(
                        service.payCredit(
                                "cr1",
                                "a1",
                                BigDecimal.valueOf(100)
                        )
                )

                .expectNextMatches(result ->
                        result.getOutstandingBalance()
                                .equals(BigDecimal.valueOf(400))
                )
                .verifyComplete();
    }

    @Test
    void payCredit_whenAmountExceedsDebt_shouldFail() {
        Credit credit = Credit.builder()
                .id("cr1")
                .outstandingBalance(BigDecimal.valueOf(100))
                .build();
        when(repository.findById("cr1"))
                .thenReturn(Mono.just(credit));
        when(accountClient.getAccountById("a1"))
                .thenReturn(
                        Mono.just(
                                Account.builder()
                                        .id("a1")
                                        .balance(BigDecimal.valueOf(1000))
                                        .build()
                        )
                );
        StepVerifier.create(
                        service.payCredit(
                                "cr1",
                                "a1",
                                BigDecimal.valueOf(200)
                        )
                )
                .expectErrorMessage(
                        "Payment exceeds debt"
                )
                .verify();
    }

    @Test
    void delete_shouldComplete() {
        Credit credit = Credit.builder()
                .id("cr1")
                .build();
        when(repository.findById("cr1"))
                .thenReturn(Mono.just(credit));
        when(repository.delete(credit))
                .thenReturn(Mono.empty());
        when(redisTemplate.delete("credit:cr1"))
                .thenReturn(Mono.just(1L));
        StepVerifier.create(service.delete("cr1"))
                .verifyComplete();


        verify(repository)
                .delete(credit);


        verify(redisTemplate)
                .delete("credit:cr1");
    }

    @Test
    void findById_shouldReturnCreditFromCache() {
        Credit credit = Credit.builder()
                .id("cr1")
                .build();
        when(valueOperations.get("credit:cr1"))
                .thenReturn(Mono.just(credit));
        // Aunque no debería usarse, debe devolver un Mono y no null
        when(repository.findById("cr1"))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.findById("cr1"))
                .expectNext(credit)
                .verifyComplete();
        verify(valueOperations)
                .get("credit:cr1");
    }

    @Test
    void hasOverdueDebt_whenCreditIsExpired_shouldReturnTrue() {


        Credit credit = Credit.builder()
                .customerId("c1")
                .outstandingBalance(BigDecimal.valueOf(500))
                .dueDate(LocalDate.now().minusDays(5))
                .build();
        when(repository.findByCustomerId("c1"))
                .thenReturn(Flux.just(credit));
        StepVerifier.create(
                        service.hasOverdueDebt("c1")
                )
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void hasOverdueDebt_whenCreditIsNotExpired_shouldReturnFalse() {
        Credit credit = Credit.builder()
                .customerId("c1")
                .outstandingBalance(BigDecimal.valueOf(500))
                .dueDate(LocalDate.now().plusDays(10))
                .build();
        when(repository.findByCustomerId("c1"))
                .thenReturn(Flux.just(credit));
        StepVerifier.create(
                        service.hasOverdueDebt("c1")
                )
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void create_whenCustomerHasOverdueDebt_shouldFail() {

        Credit credit = Credit.builder()
                .customerId("c1")
                .creditType(CreditType.PERSONAL)
                .creditLimit(BigDecimal.valueOf(1000))
                .build();

        Credit overdue = Credit.builder()
                .customerId("c1")
                .outstandingBalance(BigDecimal.valueOf(500))
                .dueDate(LocalDate.now().minusDays(1))
                .build();

        when(repository.findByCustomerId("c1"))
                .thenReturn(Flux.just(overdue));
        when(customerClient.getCustomerById("c1"))
                .thenReturn(Mono.just(Customer.builder().build()));

        StepVerifier.create(service.create(credit))
                .expectErrorMessage("Customer has overdue credit debt")
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    void findById_whenCacheIsEmpty_shouldLoadFromRepository() {

        Credit credit = Credit.builder()
                .id("cr1")
                .build();

        when(valueOperations.get("credit:cr1"))
                .thenReturn(Mono.empty());

        when(repository.findById("cr1"))
                .thenReturn(Mono.just(credit));

        when(valueOperations.set("credit:cr1", credit))
                .thenReturn(Mono.just(true));

        StepVerifier.create(service.findById("cr1"))
                .expectNext(credit)
                .verifyComplete();

        verify(repository).findById("cr1");
        verify(valueOperations).set("credit:cr1", credit);
    }
    @Test
    void findById_whenCreditDoesNotExist_shouldFail() {

        when(valueOperations.get("credit:cr1"))
                .thenReturn(Mono.empty());

        when(repository.findById("cr1"))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.findById("cr1"))
                .expectErrorMessage("Credit not found")
                .verify();
    }
    @Test
    void consume_whenCreditLimitExceeded_shouldFail() {

        Credit credit = Credit.builder()
                .id("cr1")
                .creditType(CreditType.CREDIT_CARD)
                .creditLimit(BigDecimal.valueOf(1000))
                .outstandingBalance(BigDecimal.valueOf(950))
                .build();

        when(repository.findById("cr1"))
                .thenReturn(Mono.just(credit));

        StepVerifier.create(
                        service.consume("cr1", BigDecimal.valueOf(100)))
                .expectErrorMessage("Credit limit exceeded")
                .verify();
    }

    @Test
    void consume_whenCreditNotFound_shouldFail() {

        when(repository.findById("cr1"))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        service.consume("cr1", BigDecimal.valueOf(100)))
                .expectErrorMessage("Credit not found")
                .verify();
    }
    @Test
    void payCredit_whenAccountHasInsufficientBalance_shouldFail() {

        Credit credit = Credit.builder()
                .id("cr1")
                .outstandingBalance(BigDecimal.valueOf(500))
                .build();

        when(repository.findById("cr1"))
                .thenReturn(Mono.just(credit));

        when(accountClient.getAccountById("a1"))
                .thenReturn(
                        Mono.just(
                                Account.builder()
                                        .id("a1")
                                        .balance(BigDecimal.valueOf(50))
                                        .build()
                        )
                );

        StepVerifier.create(
                        service.payCredit(
                                "cr1",
                                "a1",
                                BigDecimal.valueOf(100)
                        ))
                .expectErrorMessage("Insufficient balance")
                .verify();
    }
    @Test
    void payCredit_whenCreditNotFound_shouldFail() {

        when(repository.findById("cr1"))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        service.payCredit(
                                "cr1",
                                "a1",
                                BigDecimal.valueOf(100)
                        ))
                .expectErrorMessage("Credit not found")
                .verify();
    }
    @Test
    void delete_whenCreditNotFound_shouldFail() {

        when(repository.findById("cr1"))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.delete("cr1"))
                .expectErrorMessage("Credit not found")
                .verify();

        verify(repository, never()).delete(any());
    }
    @Test
    void hasOverdueDebt_whenCustomerHasNoCredits_shouldReturnFalse() {

        when(repository.findByCustomerId("c1"))
                .thenReturn(Flux.empty());

        StepVerifier.create(service.hasOverdueDebt("c1"))
                .expectNext(false)
                .verifyComplete();
    }
}