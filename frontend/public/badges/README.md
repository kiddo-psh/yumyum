# 뱃지 이미지 에셋

뱃지 이미지를 이 폴더에 둡니다. 파일명은 **Badge enum 코드 + `.png`** 입니다.

| 파일명 | 뱃지 |
|---|---|
| `ALL_RIGHT.png` | 올라잇!!!🔥🔥🔥 (운동) |
| `WEEKEND_WARRIOR.png` | 주말 전사 (운동) |
| `VEGGIE_MANIA.png` | 채소매니아 (식단) |
| `CHICKEN_BREAST_EVANGELIST.png` | 닭가슴살 전도사 (식단) |
| `PHOTO_KING.png` | 사진왕 냠냠 (식단) |
| `NIGHT_EATER.png` | 밤냠족 (식단) |
| `NOVICE_TAMER.png` | 초보 냠냠이 조련사 (연속) |
| `LEGENDARY_TAMER.png` | 전설의 냠냠이 조련사 (연속) |

- 경로 참조: `${import.meta.env.BASE_URL}badges/<CODE>.png` (`BadgeImage.vue`)
- 배경 투명 PNG 권장(사이드바·도감에 테두리 없이 그대로 올라감).
- 미획득 뱃지는 코드에서 `grayscale`로 회색 처리되므로 컬러 원본 하나만 두면 됩니다.
- 확장자를 png 외로 쓰려면 `BadgeImage.vue`의 `src` 계산식을 함께 바꾸세요.
