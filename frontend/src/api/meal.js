import { apiClient } from '@/services/apiClient';

/**
 * 이미지 File 객체를 base64 문자열로 변환한다 (data:... prefix 제거).
 */
function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => resolve(e.target.result.split(',')[1]);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

/**
 * 사진 분석 (DB 저장 없음). 감지된 음식 목록 + 총 칼로리 + AI 코멘트 반환.
 * @param {File} imageFile
 * @param {string} mealType - "BREAKFAST" | "LUNCH" | "DINNER" | "SNACK"
 */
export async function analyzePhoto(imageFile, mealType = 'LUNCH') {
  const imageBase64 = await fileToBase64(imageFile);
  const mediaType = imageFile.type || 'image/jpeg';
  return apiClient.post('/meals/photo/analyze', { imageBase64, mediaType, mealType });
}

/**
 * AI 분석 결과로 Meal 저장.
 * @param {string} mealType
 * @param {Array<{name, estimatedGrams, kcal, proteinG, carbG, fatG}>} items
 */
export function recordPhotoMeal(mealType, items) {
  return apiClient.post('/meals/photo', { mealType, items });
}
