package dev.qlick.meta.demo;

record DemoProduct(String id, String sku, String name, String description, int stock, String price, String slug) {
    String url() { return "/api/product/" + slug; }
}
