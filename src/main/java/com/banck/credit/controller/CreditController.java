package com.banck.credit.controller;

import com.banck.credit.dto.PaymentRequest;
import com.banck.credit.model.Credit;
import com.banck.credit.service.CreditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/credits")
public class CreditController {

    private final CreditService service;

    public CreditController(CreditService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<ResponseEntity<Credit>> create(
            @RequestBody Credit credit) {

        return service.create(credit)
                .map(ResponseEntity::ok);
    }

    @GetMapping
    public Flux<Credit> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Credit>> findById(
            @PathVariable String id) {

        return service.findById(id)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/customer/{customerId}")
    public Flux<Credit> findByCustomerId(
            @PathVariable String customerId) {

        return service.findByCustomerId(customerId);
    }

    @PostMapping("/{creditId}/consume")
    public Mono<ResponseEntity<Credit>> consume(
            @PathVariable String creditId,
            @RequestParam BigDecimal amount) {

        return service.consume(creditId, amount)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{creditId}/payment")
    public Mono<ResponseEntity<Credit>> payCredit(
            @PathVariable String creditId,
            @RequestBody PaymentRequest request) {

        return service.payCredit(
                        creditId,
                        request.getAccountId(),
                        request.getAmount())
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> delete(@PathVariable String id) {
        return service.delete(id);
    }
}
