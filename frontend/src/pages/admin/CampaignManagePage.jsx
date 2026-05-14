import { useEffect, useMemo, useState } from "react";
import {
  absoluteAssetUrl,
  createCampaign,
  deleteCampaign,
  fetchAdminCampaigns,
  toggleCampaign,
  updateCampaign,
  uploadCampaignImage
} from "../../lib/api";

const CATEGORIES = ["사료 후원", "의료비 후원", "보호소 운영", "입양 지원"];
const EMPTY_FORM = {
  category: CATEGORIES[0],
  title: "",
  goalAmount: "",
  currentAmount: "0",
  participants: "0",
  expiresAt: "",
  imageUrl: "",
  imageFile: null
};

const WON = new Intl.NumberFormat("ko-KR");

function toDateTimeLocal(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toISOString().slice(0, 16);
}

function fromDateTimeLocal(value) {
  return value ? new Date(value).toISOString().slice(0, 19) : null;
}

function imageSrc(imageUrl) {
  if (!imageUrl) return "";
  if (imageUrl.startsWith("/uploads")) return absoluteAssetUrl(imageUrl);
  return imageUrl;
}

export default function CampaignManagePage() {
  const [campaigns, setCampaigns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalMode, setModalMode] = useState(null);
  const [editingCampaign, setEditingCampaign] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [preview, setPreview] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const sortedCampaigns = useMemo(() => campaigns, [campaigns]);

  useEffect(() => {
    loadCampaigns();
  }, []);

  async function loadCampaigns() {
    try {
      const data = await fetchAdminCampaigns();
      setCampaigns(Array.isArray(data) ? data : []);
      setError("");
    } catch {
      setCampaigns([]);
      setError("캠페인 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  function openCreateModal() {
    setModalMode("create");
    setEditingCampaign(null);
    setForm(EMPTY_FORM);
    setPreview("");
  }

  function openEditModal(campaign) {
    setModalMode("edit");
    setEditingCampaign(campaign);
    setForm({
      category: campaign.category,
      title: campaign.title,
      goalAmount: String(campaign.goalAmount ?? ""),
      currentAmount: String(campaign.currentAmount ?? 0),
      participants: String(campaign.participants ?? 0),
      expiresAt: toDateTimeLocal(campaign.expiresAt),
      imageUrl: campaign.imageUrl ?? "",
      imageFile: null
    });
    setPreview(imageSrc(campaign.imageUrl ?? ""));
  }

  function closeModal() {
    setModalMode(null);
    setEditingCampaign(null);
    setForm(EMPTY_FORM);
    setPreview("");
    setSaving(false);
  }

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  function handleImageChange(event) {
    const file = event.target.files?.[0] ?? null;
    setForm((current) => ({ ...current, imageFile: file }));
    setPreview(file ? URL.createObjectURL(file) : imageSrc(form.imageUrl));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    try {
      const payload = {
        category: form.category,
        title: form.title,
        imageUrl: form.imageUrl || null,
        goalAmount: Number(form.goalAmount),
        currentAmount: Number(form.currentAmount || 0),
        participants: Number(form.participants || 0),
        expiresAt: fromDateTimeLocal(form.expiresAt)
      };

      const saved = modalMode === "edit" && editingCampaign
        ? await updateCampaign(editingCampaign.id, payload)
        : await createCampaign(payload);

      if (form.imageFile) {
        await uploadCampaignImage(saved.id, form.imageFile);
      }

      await loadCampaigns();
      closeModal();
    } catch {
      setError("캠페인 저장 중 문제가 발생했습니다.");
      setSaving(false);
    }
  }

  async function handleDelete(campaign) {
    if (!window.confirm(`"${campaign.title}" 캠페인을 삭제할까요?`)) return;
    await deleteCampaign(campaign.id);
    await loadCampaigns();
  }

  async function handleToggle(campaign) {
    await toggleCampaign(campaign.id);
    await loadCampaigns();
  }

  if (loading) {
    return <section className="paw-message-card">캠페인 관리 정보를 불러오는 중입니다.</section>;
  }

  return (
    <div className="stack paw-admin-page paw-campaign-admin-page">
      <section className="paw-list-header">
        <div>
          <p className="eyebrow">CAMPAIGN ADMIN</p>
          <h2>캠페인 관리</h2>
          <p className="paw-sub-copy">홈페이지 후원 캠페인을 등록, 수정, 비활성화할 수 있습니다.</p>
        </div>
        <button type="button" className="paw-action orange" onClick={openCreateModal}>
          캠페인 등록
        </button>
      </section>

      {error ? <section className="paw-message-card">{error}</section> : null}

      <section className="paw-campaign-admin-card">
        <table className="paw-admin-table">
          <thead>
            <tr>
              <th>이미지</th>
              <th>카테고리</th>
              <th>제목</th>
              <th>달성률</th>
              <th>참여수</th>
              <th>상태</th>
              <th>등록일</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            {sortedCampaigns.map((campaign) => {
              const percent = Math.min(100, Math.round((Number(campaign.currentAmount ?? 0) / Number(campaign.goalAmount || 1)) * 100));
              return (
                <tr key={campaign.id}>
                  <td>
                    <div className="paw-admin-campaign-thumb">
                      {campaign.imageUrl ? <img src={imageSrc(campaign.imageUrl)} alt={campaign.title} /> : <span>🐾</span>}
                    </div>
                  </td>
                  <td>{campaign.category}</td>
                  <td><strong>{campaign.title}</strong></td>
                  <td>
                    <div className="paw-admin-progress">
                      <div style={{ width: `${percent}%` }} />
                    </div>
                    <span>{percent}%</span>
                  </td>
                  <td>{WON.format(Number(campaign.participants ?? 0))}명</td>
                  <td>
                    <button
                      type="button"
                      className={`paw-toggle ${campaign.active ? "active" : ""}`}
                      onClick={() => handleToggle(campaign)}
                    >
                      {campaign.active ? "활성" : "비활성"}
                    </button>
                  </td>
                  <td>{campaign.createdAt ? new Date(campaign.createdAt).toLocaleDateString("ko-KR") : "-"}</td>
                  <td>
                    <div className="paw-admin-actions">
                      <button type="button" onClick={() => openEditModal(campaign)}>수정</button>
                      <button type="button" className="danger" onClick={() => handleDelete(campaign)}>삭제</button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </section>

      {modalMode ? (
        <div className="paw-modal-backdrop" role="presentation" onClick={closeModal}>
          <form className="paw-campaign-modal" onSubmit={handleSubmit} onClick={(event) => event.stopPropagation()}>
            <div className="paw-section-head">
              <h3>{modalMode === "edit" ? "캠페인 수정" : "캠페인 등록"}</h3>
              <button type="button" className="paw-modal-close" onClick={closeModal}>×</button>
            </div>

            <label>
              카테고리
              <select name="category" value={form.category} onChange={handleChange}>
                {CATEGORIES.map((category) => <option key={category} value={category}>{category}</option>)}
              </select>
            </label>
            <label>
              제목
              <input name="title" value={form.title} onChange={handleChange} required maxLength={200} />
            </label>
            <div className="paw-campaign-form-grid">
              <label>
                목표 금액
                <input name="goalAmount" type="number" min="1" value={form.goalAmount} onChange={handleChange} required />
              </label>
              <label>
                현재 금액
                <input name="currentAmount" type="number" min="0" value={form.currentAmount} onChange={handleChange} />
              </label>
              <label>
                참여수
                <input name="participants" type="number" min="0" value={form.participants} onChange={handleChange} />
              </label>
              <label>
                만료일
                <input name="expiresAt" type="datetime-local" value={form.expiresAt} onChange={handleChange} />
              </label>
            </div>
            <label>
              이미지 업로드
              <input type="file" accept="image/*" onChange={handleImageChange} />
            </label>
            {preview ? (
              <div className="paw-campaign-preview">
                <img src={preview} alt="캠페인 미리보기" />
              </div>
            ) : null}

            <div className="paw-modal-actions">
              <button type="button" className="paw-action dark" onClick={closeModal}>취소</button>
              <button type="submit" className="paw-action orange" disabled={saving}>
                {saving ? "저장 중..." : "저장"}
              </button>
            </div>
          </form>
        </div>
      ) : null}
    </div>
  );
}
