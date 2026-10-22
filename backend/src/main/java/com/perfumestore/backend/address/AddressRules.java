package com.perfumestore.backend.address;

import java.util.Set;

final class AddressRules {

    private static final Set<String> STATES = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
            "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC",
            "SP", "SE", "TO");

    private AddressRules() {
    }

    static boolean isBrazilianState(String state) {
        return STATES.contains(state);
    }
}
