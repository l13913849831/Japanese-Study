import Taro from "@tarojs/taro";

const TOKEN_STORAGE_KEY = "jp_mobile_token";
const TOKEN_EXPIRES_AT_STORAGE_KEY = "jp_mobile_token_expires_at";

export function getMobileToken() {
  return Taro.getStorageSync<string>(TOKEN_STORAGE_KEY) || undefined;
}

export function saveMobileToken(token: string, expiresAt: string) {
  Taro.setStorageSync(TOKEN_STORAGE_KEY, token);
  Taro.setStorageSync(TOKEN_EXPIRES_AT_STORAGE_KEY, expiresAt);
}

export function clearMobileToken() {
  Taro.removeStorageSync(TOKEN_STORAGE_KEY);
  Taro.removeStorageSync(TOKEN_EXPIRES_AT_STORAGE_KEY);
}

export function hasMobileToken() {
  return Boolean(getMobileToken());
}
