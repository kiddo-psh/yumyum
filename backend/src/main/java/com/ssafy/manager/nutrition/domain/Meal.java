package com.ssafy.manager.nutrition.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long memberId;
    @Enumerated(EnumType.STRING)
    private MealType type;
    private LocalDate date;
    private LocalDate effectiveDate;
    @Enumerated(EnumType.STRING)
    private MealSource source;
    private LocalDateTime recordedAt;

    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL)
    private List<MealItem> items = new ArrayList<>();

    public Meal(Long memberId, MealType type, LocalDate date, LocalDate effectiveDate,
                MealSource source, LocalDateTime recordedAt) {
        this.memberId = memberId;
        this.type = type;
        this.date = date;
        this.effectiveDate = effectiveDate;
        this.source = source;
        this.recordedAt = recordedAt;
    }

    public MealItem addItem(Food food, double amountGrams) {
        MealItem item = MealItem.from(food, amountGrams);
        item.bindTo(this);
        items.add(item);
        return item;
    }

    public MealItem addAiItem(String name, double amountGrams,
                               double kcal, double protein,
                               double carb, double fat) {
        MealItem item = MealItem.fromAiEstimate(name, amountGrams, kcal, protein, carb, fat);
        item.bindTo(this);
        items.add(item);
        return item;
    }
}
