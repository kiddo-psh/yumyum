package com.ssafy.manager.routine.domain;

import java.util.Arrays;
import java.util.List;

public enum SplitType {
    FULL_BODY_2(2, List.of("전신", "전신")),
    FULL_BODY_3(3, List.of("전신", "전신", "전신")),
    UPPER_LOWER_FULL(3, List.of("상체", "하체", "전신")),
    UPPER_LOWER_4(4, List.of("상체", "하체", "상체", "하체")),
    PUSH_PULL_LEGS_UPPER(4, List.of("가슴/삼두", "등/이두", "하체", "어깨/팔")),
    PUSH_PULL_LEGS_UPPER_LOWER(5, List.of("가슴/삼두", "등/이두", "하체", "상체", "하체")),
    PPLFU(5, List.of("가슴/삼두", "등/이두", "하체", "전신", "상체"));

    private final int daysPerWeek;
    private final List<String> splitLabels;

    SplitType(int daysPerWeek, List<String> splitLabels) {
        this.daysPerWeek = daysPerWeek;
        this.splitLabels = splitLabels;
    }

    public int getDaysPerWeek() { return daysPerWeek; }
    public List<String> getSplitLabels() { return splitLabels; }
    public String getLabel() { return String.join("/", splitLabels); }

    public static List<SplitType> findByDaysPerWeek(int days) {
        return Arrays.stream(values())
                .filter(s -> s.daysPerWeek == days)
                .toList();
    }
}
