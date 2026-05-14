export function createAnimalSlug(animal) {
  return normalizeNoticeNumber(animal.desertionNo ?? animal.id ?? animal.noticeNumber);
}

function normalizeNoticeNumber(value) {
  const text = String(value ?? "").trim();
  if (!text) {
    return "unknown";
  }

  return text
    .toLowerCase()
    .replace(/\s+/g, "-")
    .replace(/[^\p{L}\p{N}-]+/gu, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");
}
