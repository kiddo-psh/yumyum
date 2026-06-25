package com.ssafy.manager.global.dev;

import com.ssafy.manager.program.application.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// TEMP: 주간 리포트 평가용 생성 트리거. 평가 종료 후 이 컨트롤러와
//       SecurityConfig 의 "/dev/**" permitAll 라인을 함께 제거한다.
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
public class DevWeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    @PostMapping("/weekly-reports/run")
    public ResponseEntity<Void> run(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        weeklyReportService.createStubs(date != null ? date : LocalDate.now());
        return ResponseEntity.ok().build();
    }
}
