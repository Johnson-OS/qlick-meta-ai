package dev.qlick.meta.demo;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Fictional test merchant data; inventory and price remain deterministic application data. */
@Component
class DemoCatalog {
    private static final Set<String> SEARCH_STOP_WORDS = Set.of(
            "a", "an", "are", "available", "availability", "do", "for", "have", "in", "is", "it", "me", "of", "on", "please", "product", "stock", "the", "this", "to", "you");
    private final List<DemoProduct> products = List.of(
            new DemoProduct("p-001", "SAGE-30", "Sage + Oat Barrier Serum", "A lightweight daily serum for dry-feeling skin.", 18, "$28.00", "sage-oat-barrier-serum"),
            new DemoProduct("p-002", "CLOUD-50", "Cloud Milk Cleanser", "A gentle, fragrance-free cream cleanser.", 0, "$22.00", "cloud-milk-cleanser"),
            new DemoProduct("p-003", "DEW-50", "Dewdrop Daily Moisturizer", "A simple moisturizer with squalane and glycerin.", 7, "$34.00", "dewdrop-daily-moisturizer"),
            new DemoProduct("p-004", "NIGHT-30", "Evening Primrose Facial Oil", "A nourishing evening oil in a recyclable glass bottle.", 4, "$38.00", "evening-primrose-facial-oil"),
            new DemoProduct("p-005", "SUN-50", "Daylight Mineral SPF 30", "A sheer mineral sunscreen for everyday use.", 24, "$30.00", "daylight-mineral-spf-30"));
    List<DemoProduct> search(String query) {
        if (query == null || query.isBlank()) return products.stream().limit(4).toList();
        List<String> terms = java.util.Arrays.stream(normalize(query).split(" "))
                .filter(term -> !term.isBlank() && !SEARCH_STOP_WORDS.contains(term))
                .distinct()
                .toList();
        if (terms.isEmpty()) return List.of();
        return products.stream()
                .filter(product -> {
                    String fields = normalize(product.name() + " " + product.description() + " " + product.sku());
                    return terms.stream().allMatch(fields::contains);
                })
                .limit(4)
                .toList();
    }
    Optional<DemoProduct> byId(String id) { return products.stream().filter(p -> p.id().equals(id)).findFirst(); }
    Optional<DemoProduct> bySlug(String slug) { return products.stream().filter(p -> p.slug().equals(slug)).findFirst(); }

    private static String normalize(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
    }
}
