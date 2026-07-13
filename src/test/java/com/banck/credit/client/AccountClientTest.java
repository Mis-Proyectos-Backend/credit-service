package com.banck.credit.client;

import com.banck.credit.client.dto.Account;
import com.banck.credit.client.dto.WithdrawRequest;
import com.banck.credit.enums.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.net.URI;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountClientTest {

    @Mock
    private WebClient.Builder builder;

    @Mock
    private WebClient webClient;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private AccountClient accountClient;

    @BeforeEach
    void setUp() {
        when(builder.build()).thenReturn(webClient);
        accountClient = new AccountClient(builder);
    }

    @Test
    void getAccountsByCustomer_shouldReturnAccounts() {
        Account account = Account.builder()
                .id("a1")
                .customerId("c1")
                .balance(BigDecimal.valueOf(1000))
                .build();
        doReturn(requestHeadersUriSpec).when(webClient).get();

        doReturn(requestHeadersSpec)
                .when(requestHeadersUriSpec)
                .uri(
                        eq("http://account-service/accounts/customer/{id}"),
                        eq("c1")
                );

        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(Account.class))
                .thenReturn(Flux.just(account));

        StepVerifier.create(accountClient.getAccountsByCustomer("c1"))
                .expectNext(account)
                .verifyComplete();
    }
    @Test
    void withdraw_shouldReturnAccount() {

        Account account = Account.builder()
                .id("a1")
                .balance(BigDecimal.valueOf(900))
                .build();

        WithdrawRequest request = WithdrawRequest.builder()
                .amount(BigDecimal.valueOf(100))
                .paymentMethod(PaymentMethod.CREDIT_PAYMENT)
                .build();

        doReturn(requestBodyUriSpec)
                .when(webClient)
                .post();

        doAnswer(invocation -> {

            @SuppressWarnings("unchecked")
            Function<UriBuilder, URI> function =
                    invocation.getArgument(0);

            URI uri = function.apply(
                    new DefaultUriBuilderFactory().builder());

            assertEquals("http://account-service/accounts/a1/withdraw",
                    uri.toString());

            return requestBodyUriSpec;

        }).when(requestBodyUriSpec)
                .uri(any(Function.class));

        when(requestBodyUriSpec.bodyValue(any(WithdrawRequest.class)))
                .thenReturn(requestHeadersSpec);

        when(requestHeadersSpec.retrieve())
                .thenReturn(responseSpec);

        when(responseSpec.bodyToMono(Account.class))
                .thenReturn(Mono.just(account));

        StepVerifier.create(accountClient.withdraw("a1", request))
                .expectNext(account)
                .verifyComplete();
    }


    @Test
    void fallbackAccounts_shouldReturnEmptyFlux() {
        StepVerifier.create(
                        accountClient.fallbackAccounts(
                                "c1",
                                new RuntimeException("error")))
                .verifyComplete();
    }

    @Test
    void fallbackWithdraw_shouldReturnError() {
        StepVerifier.create(
                        accountClient.fallbackWithdraw(
                                "a1",
                                BigDecimal.valueOf(100),
                                new RuntimeException("error")))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException
                                && ex.getMessage().equals("Account Service unavailable"))
                .verify();
    }

    @Test
    void getAccountById_shouldReturnAccount() {
        Account account = Account.builder()
                .id("a1")
                .balance(BigDecimal.valueOf(1000))
                .build();

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(
                "http://account-service/accounts/{id}",
                "a1"))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve())
                .thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Account.class))
                .thenReturn(Mono.just(account));

        StepVerifier.create(accountClient.getAccountById("a1"))
                .expectNext(account)
                .verifyComplete();
    }
}
