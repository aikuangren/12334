package com.fiftytalk.carddraw.service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToDoubleFunction;

public class WeightedRandomPicker {
    private WeightedRandomPicker() {
    }

    public static <T> T pick(List<T> items, ToDoubleFunction<T> weightFunction) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        double total = items.stream()
                .filter(Objects::nonNull)
                .mapToDouble(weightFunction)
                .filter(weight -> weight > 0)
                .sum();
        if (total <= 0) {
            return items.get(ThreadLocalRandom.current().nextInt(items.size()));
        }
        double cursor = ThreadLocalRandom.current().nextDouble(total);
        for (T item : items) {
            double weight = weightFunction.applyAsDouble(item);
            if (weight <= 0) {
                continue;
            }
            cursor -= weight;
            if (cursor <= 0) {
                return item;
            }
        }
        return items.get(items.size() - 1);
    }
}

