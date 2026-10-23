package com.perfumestore.backend.order;

record OrderAddressRecord(String recipientName, String label, String postalCode, String street, String number,
        String complement, String neighborhood, String city, String state, String reference) { }
