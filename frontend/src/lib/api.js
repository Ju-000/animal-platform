const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

async function request(path) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: "include"
  });

  return handleApiResponse(response);
}

async function requestOptional(path) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: "include"
  });

  if (response.status === 401) {
    return null;
  }

  return handleApiResponse(response);
}

async function requestJson(path, options) {
  const csrfToken = await getCsrfToken();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(csrfToken ? { "X-XSRF-TOKEN": csrfToken } : {})
    },
    ...options
  });

  return handleApiResponse(response);
}

async function requestForm(path, formData, options = {}) {
  const csrfToken = await getCsrfToken();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: "include",
    headers: {
      ...(csrfToken ? { "X-XSRF-TOKEN": csrfToken } : {})
    },
    ...options,
    body: formData
  });

  return handleApiResponse(response);
}

async function getCsrfToken() {
  const cookieToken = readCookie("XSRF-TOKEN");
  if (cookieToken) {
    return cookieToken;
  }

  await fetch(`${API_BASE_URL}/api/csrf`, {
    credentials: "include"
  });
  return readCookie("XSRF-TOKEN");
}

function readCookie(name) {
  return document.cookie
    .split(";")
    .map((item) => item.trim())
    .find((item) => item.startsWith(`${name}=`))
    ?.slice(name.length + 1);
}

async function handleApiResponse(response) {
  const payload = await readJsonPayload(response);
  if (!response.ok) {
    throw new Error(payload?.message || `Request failed: ${response.status}`);
  }
  return payload?.data;
}

async function readJsonPayload(response) {
  const text = await response.text();
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch (error) {
    return { message: text };
  }
}

export function fetchAnimals({
  page = 1,
  size = 20,
  region,
  orgNm,
  species,
  breed,
  status,
  sex,
  neutered,
  protectingOnly,
  startDate,
  endDate
} = {}) {
  const params = new URLSearchParams();
  params.set("page", String(page));
  params.set("size", String(size));
  if (region) params.set("region", region);
  if (orgNm) params.set("orgNm", orgNm);
  if (species) params.set("species", species);
  if (breed) params.set("breed", breed);
  if (status) params.set("status", status);
  if (sex) params.set("sex", sex);
  if (neutered) params.set("neutered", neutered);
  if (protectingOnly) params.set("protectingOnly", "true");
  if (startDate) params.set("startDate", startDate);
  if (endDate) params.set("endDate", endDate);
  return request(`/api/animals?${params.toString()}`);
}

export function fetchRecommendedAnimals({ orgNm, size = 10 } = {}) {
  const params = new URLSearchParams();
  params.set("size", String(size));
  if (orgNm) params.set("orgNm", orgNm);
  return request(`/api/animals/recommended?${params.toString()}`);
}

export function fetchAnimalFilters() {
  return request("/api/animals/filters");
}

export function fetchAnimalDetail(animalSlug) {
  return request(`/api/animals/${encodeURIComponent(animalSlug)}`);
}

export function fetchAnimalSummary(desertionNo) {
  return request(`/api/animals/${encodeURIComponent(desertionNo)}/summary`);
}

export function sendChatMessage(messages) {
  return requestJson("/api/chat", {
    method: "POST",
    body: JSON.stringify({ messages })
  });
}

export function fetchChatQuickAnswers() {
  return request("/api/chat/quick-answers");
}

export function fetchChatQuickAnswer(keyword) {
  return requestJson("/api/chat/quick-answer", {
    method: "POST",
    body: JSON.stringify({ keyword })
  });
}

export function submitMatching(payload) {
  return requestJson("/api/matching", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function fetchShelters({ status } = {}) {
  const params = new URLSearchParams();
  if (status) params.set("status", status);
  const suffix = params.toString() ? `?${params.toString()}` : "";
  return request(`/api/shelters${suffix}`);
}

export function fetchShelterDetail(shelterId) {
  return request(`/api/shelters/${shelterId}`);
}

export function fetchShelterDonationGoal(careRegNo) {
  return request(`/api/shelters/${encodeURIComponent(careRegNo)}/donation-goal`);
}

export function fetchFavorites() {
  return request("/api/favorites");
}

export function addFavorite(animalNo) {
  return requestJson(`/api/favorites/${encodeURIComponent(animalNo)}`, {
    method: "POST",
    body: JSON.stringify({})
  });
}

export function removeFavorite(animalNo) {
  return requestJson(`/api/favorites/${encodeURIComponent(animalNo)}`, {
    method: "DELETE",
    body: JSON.stringify({})
  });
}

export function fetchDonationOptions() {
  return request("/api/donations/options");
}

export function fetchDonationUsage() {
  return request("/api/donations/usage");
}

export function fetchDonationStats() {
  return request("/api/donations/stats");
}

export function fetchCampaigns() {
  return request("/api/campaigns");
}

export function fetchAdminCampaigns() {
  return request("/api/admin/campaigns");
}

export function createCampaign(payload) {
  return requestJson("/api/admin/campaigns", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function updateCampaign(campaignId, payload) {
  return requestJson(`/api/admin/campaigns/${encodeURIComponent(campaignId)}`, {
    method: "PUT",
    body: JSON.stringify(payload)
  });
}

export function deleteCampaign(campaignId) {
  return requestJson(`/api/admin/campaigns/${encodeURIComponent(campaignId)}`, {
    method: "DELETE",
    body: JSON.stringify({})
  });
}

export function toggleCampaign(campaignId) {
  return requestJson(`/api/admin/campaigns/${encodeURIComponent(campaignId)}/toggle`, {
    method: "PATCH",
    body: JSON.stringify({})
  });
}

export function uploadCampaignImage(campaignId, image) {
  const formData = new FormData();
  formData.append("image", image);
  return requestForm(`/api/admin/campaigns/${encodeURIComponent(campaignId)}/image`, formData, {
    method: "POST"
  });
}

export function fetchStatsSummary({ startDate, endDate } = {}) {
  const params = new URLSearchParams();
  if (startDate) params.set("startDate", startDate);
  if (endDate) params.set("endDate", endDate);
  const suffix = params.toString() ? `?${params.toString()}` : "";
  return request(`/api/stats/summary${suffix}`);
}

export function fetchRegionStats({ startDate, endDate } = {}) {
  const params = new URLSearchParams();
  if (startDate) params.set("startDate", startDate);
  if (endDate) params.set("endDate", endDate);
  const suffix = params.toString() ? `?${params.toString()}` : "";
  return request(`/api/stats/regions${suffix}`);
}

export function fetchAdminDashboard() {
  return request("/api/admin/dashboard");
}

export function fetchAdminMonitor() {
  return request("/api/admin/monitor");
}

export function runAdminBatch() {
  return requestJson("/api/admin/batch/run", {
    method: "POST",
    body: JSON.stringify({})
  });
}

export function fetchAdminAdoptions({ status, page = 0, size = 20 } = {}) {
  const params = new URLSearchParams();
  params.set("page", String(page));
  params.set("size", String(size));
  if (status && status !== "ALL") params.set("status", status);
  return request(`/api/admin/adoptions?${params.toString()}`);
}

export function updateAdminAdoptionStatus(id, status) {
  return requestJson(`/api/admin/adoptions/${encodeURIComponent(id)}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status })
  });
}

export function fetchAdminDonations({ page = 0, size = 20 } = {}) {
  const params = new URLSearchParams();
  params.set("page", String(page));
  params.set("size", String(size));
  return request(`/api/admin/donations?${params.toString()}`);
}

export function fetchAdminUsers({ page = 0, size = 20 } = {}) {
  const params = new URLSearchParams();
  params.set("page", String(page));
  params.set("size", String(size));
  return request(`/api/admin/users?${params.toString()}`);
}

export function updateAdminUserRole(id, role) {
  return requestJson(`/api/admin/users/${encodeURIComponent(id)}/role`, {
    method: "PATCH",
    body: JSON.stringify({ role })
  });
}

export function fetchAdminBatchHistory({ limit = 20 } = {}) {
  const params = new URLSearchParams();
  params.set("limit", String(limit));
  return request(`/api/admin/batch/history?${params.toString()}`);
}

export function fetchAdoptionChecklist() {
  return request("/api/adoptions/checklist");
}

export function submitAdoptionApplication(payload) {
  return requestJson("/api/adoptions", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function fetchSocialProviders() {
  return request("/api/auth/social/providers");
}

export function checkUsername(username) {
  const params = new URLSearchParams();
  params.set("username", username);
  return request(`/api/auth/check-username?${params.toString()}`);
}

export function checkNickname(nickname) {
  const params = new URLSearchParams();
  params.set("nickname", nickname);
  return request(`/api/auth/check-nickname?${params.toString()}`);
}

export function loginWithPassword(payload) {
  return requestJson("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function signUpWithPassword(payload) {
  return requestJson("/api/auth/signup", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function logoutSession() {
  return requestJson("/api/auth/logout", {
    method: "POST",
    body: JSON.stringify({})
  });
}

export function fetchMyProfile() {
  return requestOptional("/api/users/me");
}

export function updateMyNotificationSettings(payload) {
  return requestJson("/api/users/me/notifications", {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export function fetchMyDonations() {
  return request("/api/donations/me");
}

export function fetchMyDonationHistory({ page = 0, size = 10 } = {}) {
  const params = new URLSearchParams();
  params.set("page", String(page));
  params.set("size", String(size));
  return request(`/api/users/me/donation-history?${params.toString()}`);
}

export function fetchMyAdoptions() {
  return request("/api/adoptions/my");
}

export function fetchPortOneConfig() {
  return request("/api/payments/portone/config");
}

export function preparePayment(payload) {
  return requestJson("/api/payments/prepare", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function confirmPayment(payload) {
  return requestJson("/api/payments/confirm", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function fetchStories({ page = 0, size = 9 } = {}) {
  const params = new URLSearchParams();
  params.set("page", String(page));
  params.set("size", String(size));
  return request(`/api/stories?${params.toString()}`);
}

export function fetchStory(storyId) {
  return request(`/api/stories/${storyId}`);
}

export function createStory({ title, content, animalNo, image }) {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("content", content);
  if (animalNo) formData.append("animalNo", animalNo);
  if (image) formData.append("image", image);
  return requestForm("/api/stories", formData, { method: "POST" });
}

export function deleteStory(storyId) {
  return requestJson(`/api/stories/${storyId}`, {
    method: "DELETE",
    body: JSON.stringify({})
  });
}

export function toggleStoryLike(storyId) {
  return requestJson(`/api/stories/${storyId}/like`, {
    method: "POST",
    body: JSON.stringify({})
  });
}

export function absoluteAssetUrl(path) {
  if (!path) return "";
  if (path.startsWith("http")) return path;
  return `${API_BASE_URL}${path}`;
}

