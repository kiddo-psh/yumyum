package com.ssafy.manager.nyam.application;

import com.ssafy.manager.nyam.domain.BodyCategory;

public record NyamBodyResult(BodyCategory category) {

    public static NyamBodyResult from(BodyCategory category) {
        return new NyamBodyResult(category);
    }
}
