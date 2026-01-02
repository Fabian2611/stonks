package io.fabianbuthere.stonks.api;

public record DeliveryJobSpecification(String item, int weight, int uMin, int uMax, int step, int paymentMin, int paymentMax) {

}
