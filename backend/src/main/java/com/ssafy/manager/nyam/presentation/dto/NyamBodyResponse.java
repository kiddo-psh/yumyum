package com.ssafy.manager.nyam.presentation.dto;

import com.ssafy.manager.nyam.application.NyamBodyResult;
import com.ssafy.manager.nyam.domain.BodyCategory;

public record NyamBodyResponse(BodyCategory category) {

    public static NyamBodyResponse from(NyamBodyResult result) {
        return new NyamBodyResponse(result.category());
    }
}
