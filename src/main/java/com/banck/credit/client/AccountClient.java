package com.banck.credit.client;

import com.banck.credit.client.dto.Account;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
public class AccountClient {

    private final WebClient webClient;

    public AccountClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "fallbackAccounts")
    public Flux<Account> getAccountsByCustomer(String customerId) {
        return webClient.get()
                .uri("http://account-service/accounts/customer/{id}", customerId)
                .retrieve()
                .bodyToFlux(Account.class);
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "fallbackAccounts")
    public Mono<Account> getAccountById(String accountId) {
        return webClient.get()
                .uri("http://account-service/accounts/{id}", accountId)
                .retrieve()
                .bodyToMono(Account.class);
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "fallbackWithdraw")
    public Mono<Account> withdraw(String accountId, BigDecimal amount) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http")
                        .host("account-service")
                        .path("/accounts/{id}/withdraw")
                        .queryParam("amount", amount)
                        .build(accountId))
                .retrieve()
                .bodyToMono(Account.class);
    }

    public Flux<Account> fallbackAccounts(String customerId, Throwable ex) {
        return Flux.empty();
    }

    public Mono<Account> fallbackWithdraw(String accountId, BigDecimal amount, Throwable ex) {
        return Mono.error(
                new RuntimeException("Account Service unavailable", ex));
    }
}

