import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  addFavorite,
  fetchAdoptionChecklist,
  fetchAnimalDetail,
  fetchAnimalSummary,
  submitAdoptionApplication
} from "../lib/api";
import { pushRecentAnimal } from "../lib/recentAnimals";
import ErrorBoundary from "../components/common/ErrorBoundary";
import AISummaryFallback from "../components/common/AISummaryFallback";

const statusLabelMap = {
  ADOPTABLE: "보호중",
  PROTECTING: "보호중",
  SUPPORT_NEEDED: "후원 필요",
  IN_COUNSELING: "상담 진행 중",
  COMPLETED: "완료"
};

function normalizeSexLabel(value) {
  if (!value) return "정보 없음";
  if (value === "FEMALE" || value === "F" || value === "암컷") return "암컷";
  if (value === "MALE" || value === "M" || value === "수컷") return "수컷";
  return value;
}

function safeText(value, fallback) {
  return value && String(value).trim() ? value : fallback;
}

function safeSummary(value) {
  const text = String(value ?? "").trim();
  return text && text.toLowerCase() !== "null" ? text : "";
}

export default function AnimalDetailPage() {
  const { animalSlug } = useParams();
  const [animal, setAnimal] = useState(null);
  const [checklistItems, setChecklistItems] = useState([]);
  const [checkedItems, setCheckedItems] = useState([]);
  const [applicationForm, setApplicationForm] = useState({
    applicantName: "",
    applicantPhone: "",
    applicantEmail: "",
    address: "",
    livingType: "APARTMENT",
    householdType: "1인",
    hasExperience: false,
    applicantMessage: ""
  });
  const [applicationMessage, setApplicationMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedImage, setSelectedImage] = useState("");
  const [aiSummary, setAiSummary] = useState("");
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [summaryError, setSummaryError] = useState("");

  useEffect(() => {
    let active = true;

    Promise.all([fetchAnimalDetail(animalSlug), fetchAdoptionChecklist()])
      .then(([animalData, checklistData]) => {
        if (!active) return;
        setAnimal(animalData);
        setChecklistItems(checklistData ?? []);
      })
      .catch(() => {
        if (!active) return;
        setError("상세 정보를 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!active) return;
        setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [animalSlug]);

  const gallery = useMemo(() => {
    if (!animal) return [];
    const merged = [...(animal.imageUrls ?? []), animal.imageUrl].filter(Boolean);
    return [...new Set(merged)];
  }, [animal]);

  useEffect(() => {
    setSelectedImage(gallery[0] ?? "");
  }, [gallery]);

  useEffect(() => {
    if (!animal) return;

    pushRecentAnimal({
      id: animal.id,
      slug: animalSlug,
      name: animal.name,
      imageUrl: animal.imageUrl,
      noticeNumber: animal.noticeNumber,
      ageText: animal.ageText,
      sex: animal.sex,
      weightText: animal.weightText,
      region: animal.region,
      species: animal.species,
      serviceStatus: animal.serviceStatus
    });
  }, [animal, animalSlug]);

  function refreshSummary() {
    if (!animal) return;
    const summaryKey = animal.noticeNumber ?? animal.id;
    if (!summaryKey) return;

    setSummaryLoading(true);
    setSummaryError("");
    setAiSummary("");

    fetchAnimalSummary(summaryKey)
      .then((data) => {
        setAiSummary(safeSummary(data?.summary));
      })
      .catch(() => {
        setSummaryError("✨ AI 소개를 준비 중이에요. 잠시 후 다시 확인해주세요 🐾");
      })
      .finally(() => {
        setSummaryLoading(false);
      });
  }

  useEffect(() => {
    refreshSummary();
    // animal이 바뀔 때마다 한 번만 새 소개를 요청합니다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [animal]);

  if (loading) {
    return <section className="paw-message-card">상세 정보를 불러오는 중입니다.</section>;
  }

  if (error || !animal) {
    return <section className="paw-message-card">{error || "상세 정보를 찾을 수 없습니다."}</section>;
  }

  const shelter = animal.shelter ?? {};
  const shelterName = safeText(shelter.name, "보호소 정보 없음");
  const statusText = statusLabelMap[animal.serviceStatus] ?? "보호중";
  const sexText = normalizeSexLabel(animal.sex);
  const ageText = safeText(animal.ageText, "나이 정보 없음");
  const weightText = safeText(animal.weightText, "체중 정보 없음");
  const heroImage = selectedImage || gallery[0] || animal.imageUrl;
  const checklistCompleted = checklistItems.length > 0 && checkedItems.length === checklistItems.length;

  async function handleApplicationSubmit(event) {
    event.preventDefault();

    if (!checklistCompleted) {
      setApplicationMessage("입양 전 체크리스트를 모두 확인해주세요.");
      return;
    }

    try {
      const result = await submitAdoptionApplication({
        animalNo: animal.noticeNumber ?? String(animal.id),
        applicantName: applicationForm.applicantName,
        applicantPhone: applicationForm.applicantPhone,
        applicantEmail: applicationForm.applicantEmail,
        address: applicationForm.address,
        housingType: applicationForm.livingType,
        hasExperience: applicationForm.hasExperience,
        reason: applicationForm.applicantMessage
      });
      setApplicationMessage(result.message ?? `입양 신청이 접수되었습니다. 접수 번호: ${result.id}`);
    } catch {
      setApplicationMessage("입양 신청을 접수하지 못했습니다. 입력값과 로그인 상태를 확인해주세요.");
    }
  }

  async function handleFavoriteAdd() {
    try {
      await addFavorite(animal.noticeNumber ?? String(animal.id));
      setApplicationMessage("관심 동물로 저장했습니다.");
    } catch {
      setApplicationMessage("관심 동물 저장은 로그인이 필요합니다.");
    }
  }

  return (
    <div className="paw-detail-page stack">
      <section className="paw-detail-top single-photo-layout">
        <article className="paw-detail-gallery">
          <div className="paw-detail-gallery-head">
            <div className="paw-shelter-chip paw-shelter-chip-header">{shelterName}</div>
            <span className={`paw-badge ${animal.serviceStatus === "ADOPTABLE" ? "green" : "warm"}`}>
              {statusText}
            </span>
          </div>

          <div className="paw-detail-main-image compact-detail-photo">
            {heroImage ? <img src={heroImage} alt={animal.name} /> : <div>NO IMAGE</div>}
          </div>

          {gallery.length > 1 ? (
            <div className="paw-detail-thumbnail-row">
              {gallery.map((image, index) => (
                <button
                  key={`${image}-${index}`}
                  type="button"
                  className={`paw-detail-thumbnail ${image === heroImage ? "active" : ""}`}
                  onClick={() => setSelectedImage(image)}
                >
                  <img src={image} alt={`${animal.name} ${index + 1}`} />
                </button>
              ))}
            </div>
          ) : null}
        </article>

        <article className="paw-detail-info">
          <h2>[{safeText(animal.species, "동물")}] {animal.name}</h2>
          <p className="paw-detail-sub">{sexText} / {ageText} / {weightText}</p>

          <dl className="paw-detail-list">
            <div>
              <dt>공고번호</dt>
              <dd>{animal.noticeNumber}</dd>
            </div>
            <div>
              <dt>발견장소</dt>
              <dd>{safeText(animal.foundPlace, "발견 장소 정보 없음")}</dd>
            </div>
            <div>
              <dt>특이사항</dt>
              <dd>{safeText(animal.specialMark, "특이사항 정보 없음")}</dd>
            </div>
            <div>
              <dt>보호센터</dt>
              <dd>{shelterName} (tel : {safeText(shelter.phone, "전화번호 없음")})</dd>
            </div>
            <div>
              <dt>주소</dt>
              <dd>{safeText(shelter.address, "주소 정보 없음")}</dd>
            </div>
          </dl>

          <ErrorBoundary fallback={<AISummaryFallback />}>
            <section className="paw-ai-summary-card">
              <div className="paw-ai-summary-head">
                <span aria-hidden="true">✨</span>
                <h3>AI 소개</h3>
              </div>
              {summaryLoading ? (
                <div className="paw-ai-summary-skeleton" aria-label="AI 소개 생성 중">
                  <span />
                  <span />
                  <span className="short" />
                </div>
              ) : summaryError ? (
                <div className="paw-ai-summary-fallback" role="status">
                  <p>{summaryError}</p>
                  <button type="button" className="paw-ai-retry-button" onClick={refreshSummary}>다시 시도</button>
                </div>
              ) : (
                <p>{aiSummary || "✨ AI 소개를 준비 중이에요. 잠시 후 다시 확인해주세요 🐾"}</p>
              )}
            </section>
          </ErrorBoundary>

          <p className="paw-detail-note">전화 문의는 보호소 운영시간 확인 후 이용 바랍니다.</p>

          <div className="paw-detail-actions">
            <button className="paw-action orange" type="button" onClick={handleFavoriteAdd}>관심 등록</button>
            <Link className="paw-action green" to="/donations">후원하기</Link>
            {shelter.id ? <Link className="paw-action dark" to={`/shelters/${shelter.id}`}>보호소 보기</Link> : null}
          </div>
        </article>
      </section>

      <section className="paw-section">
        <div className="paw-section-head">
          <h3>입양 전 체크리스트</h3>
        </div>
        <div className="paw-checklist-grid">
          {checklistItems.map((item) => {
            const checked = checkedItems.includes(item.id);
            return (
              <label key={item.id} className={`paw-checklist-card ${checked ? "checked" : ""}`}>
                <input
                  type="checkbox"
                  checked={checked}
                  onChange={(event) => {
                    setCheckedItems((current) =>
                      event.target.checked ? [...current, item.id] : current.filter((candidate) => candidate !== item.id)
                    );
                  }}
                />
                <span>{item.label}</span>
              </label>
            );
          })}
        </div>
      </section>

      <section className="paw-section">
        <div className="paw-section-head">
          <h3>입양 신청</h3>
        </div>
        <form className="paw-adoption-form" onSubmit={handleApplicationSubmit}>
          <div className="paw-form-grid two-column">
            <label>
              <span>신청자 이름</span>
              <input
                value={applicationForm.applicantName}
                onChange={(event) => setApplicationForm((current) => ({ ...current, applicantName: event.target.value }))}
                placeholder="이름 입력"
              />
            </label>
            <label>
              <span>연락처</span>
              <input
                value={applicationForm.applicantPhone}
                onChange={(event) => setApplicationForm((current) => ({ ...current, applicantPhone: event.target.value }))}
                placeholder="01012345678"
              />
            </label>
            <label>
              <span>이메일</span>
              <input
                type="email"
                value={applicationForm.applicantEmail}
                onChange={(event) => setApplicationForm((current) => ({ ...current, applicantEmail: event.target.value }))}
                placeholder="name@example.com"
              />
            </label>
            <label>
              <span>주소</span>
              <input
                value={applicationForm.address}
                onChange={(event) => setApplicationForm((current) => ({ ...current, address: event.target.value }))}
                placeholder="거주지 주소"
              />
            </label>
            <label>
              <span>주거 형태</span>
              <select
                value={applicationForm.livingType}
                onChange={(event) => setApplicationForm((current) => ({ ...current, livingType: event.target.value }))}
              >
                <option value="APARTMENT">아파트</option>
                <option value="HOUSE">단독주택</option>
                <option value="OTHER">기타</option>
              </select>
            </label>
            <label>
              <span>반려동물 양육 경험</span>
              <select
                value={applicationForm.hasExperience ? "yes" : "no"}
                onChange={(event) => setApplicationForm((current) => ({ ...current, hasExperience: event.target.value === "yes" }))}
              >
                <option value="no">없음</option>
                <option value="yes">있음</option>
              </select>
            </label>
            <label>
              <span>가구 구성</span>
              <select
                value={applicationForm.householdType}
                onChange={(event) => setApplicationForm((current) => ({ ...current, householdType: event.target.value }))}
              >
                <option>1인</option>
                <option>2인</option>
                <option>3인 이상</option>
                <option>반려동물 동거</option>
              </select>
            </label>
          </div>

          <label>
            <span>입양 신청 사유</span>
            <textarea
              rows="5"
              value={applicationForm.applicantMessage}
              onChange={(event) => setApplicationForm((current) => ({ ...current, applicantMessage: event.target.value }))}
              placeholder="입양을 희망하는 이유와 양육 계획을 적어주세요."
            />
          </label>

          <button type="submit" className="paw-action orange" disabled={!checklistCompleted}>
            입양 신청 접수
          </button>
          {applicationMessage ? <p className="paw-form-message">{applicationMessage}</p> : null}
        </form>
      </section>
    </div>
  );
}
