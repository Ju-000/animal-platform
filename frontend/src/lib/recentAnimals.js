const STORAGE_KEY = "dasi-family-recent-animals";
const MAX_ITEMS = 8;

function canUseStorage() {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}

export function getRecentAnimals() {
  if (!canUseStorage()) return [];

  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function pushRecentAnimal(animal) {
  if (!canUseStorage() || !animal?.slug) return [];

  const current = getRecentAnimals().filter((item) => item.slug !== animal.slug);
  const next = [animal, ...current].slice(0, MAX_ITEMS);

  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  } catch {
    return current;
  }

  return next;
}
