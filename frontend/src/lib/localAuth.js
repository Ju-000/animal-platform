export function isFirebaseConfigured() {
  return Boolean(import.meta.env.VITE_FIREBASE_API_KEY && import.meta.env.VITE_FIREBASE_AUTH_DOMAIN);
}

export function getDevVerificationCode() {
  return "123456";
}
