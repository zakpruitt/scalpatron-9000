package com.zakpruitt.scalpatron9000.model;

public record ProductTarget(
        String alias,
        String url,
        double maxUnitPrice,
        int maxQuantity
) {
    public ProductTarget {
        if (maxUnitPrice <= 0) throw new IllegalArgumentException("maxUnitPrice must be > 0");
        if (maxQuantity <= 0) throw new IllegalArgumentException("maxQuantity must be > 0");
    }

    @Override
    public String toString() {
        return "ProductTarget{" +
                "url='" + url + '\'' +
                ", maxUnitPrice=" + maxUnitPrice +
                ", maxQuantity=" + maxQuantity +
                '}';
    }
}
