### 설명

-

### Query Parameter

-

| key | 설명 | 필수여부 | 제약조건 | 형식 | 기본값 |
| --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |
|  |  |  |  |  |  |

### Path Variable

### Request Header

```java

```

### Request Body

```java

```

### Response Header

```bash
HTTP/1.1 200 OK
```

| key | value |
| --- | --- |
| Content-Type | application/json |

### Response Body

| Name | Type | Description |
| --- | --- | --- |
|  |  |  |
|  |  |  |
|  |  |  |
|  |  |  |
|  |  |  |
|  |  |  |

**예시 데이터**
```json
{
    "mealId": 1,
    "memberId": 42,
    "type": "BREAKFAST",              
    "date": "2026-06-02",
    "effectiveDate": "2026-06-02",    
    "items": [
        {
            "mealItemId": 1,
            "foodId": 10,
            "foodName": "현미밥",
            "amountGrams": 200.0,
            "calories": 330.0,
            "carbs": 71.0,
            "protein": 6.0,
            "fat": 1.4,
            "fiber": 2.4
        },
        {
            "mealItemId": 2,
            "foodId": 23,
            "foodName": "닭가슴살",
            "amountGrams": 150.0,
            "calories": 165.0,
            "carbs": 0.0,
            "protein": 37.5,
            "fat": 1.8,
            "fiber": 0.0
        }
    ]
}
```
