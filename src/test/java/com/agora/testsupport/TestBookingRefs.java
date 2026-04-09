package com.agora.testsupport;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Préfixes de test (yyMMdd artificiel) pour satisfaire {@code booking_reference NOT NULL} en JPA / intégration.
 */
public final class TestBookingRefs {

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    private TestBookingRefs() {}

    /** Référence unique 11 caractères ({@code yyMMdd} + 5 chiffres). */
    public static String next() {
        return String.format("991231%05d", SEQ.getAndIncrement());
    }
}
