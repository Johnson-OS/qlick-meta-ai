package dev.qlick.meta.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DemoCatalogTest {
    private final DemoCatalog catalog = new DemoCatalog();

    @Test
    void findsTheFictionalProductFromNaturalSearchTerms() {
        var result = catalog.search("is the Sage + Oat serum available");

        assertEquals(1, result.size());
        assertEquals("p-001", result.getFirst().id());
    }

    @Test
    void doesNotTurnAvailabilityOnlyQueriesIntoUnfilteredCatalogSearches() {
        assertTrue(catalog.search("is anything available?").isEmpty());
    }
}
