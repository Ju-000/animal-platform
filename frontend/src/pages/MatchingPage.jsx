import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { submitMatching } from "../lib/api";
import { createAnimalSlug } from "../lib/slug";

const steps = [
  {
    key: "housingType",
    title: "어떤 집에서 함께 살 예정인가요?",
    subtitle: "공간은 반려동물의 활동량과 안정감에 큰 영향을 줘요.",
    options: [
      { value: "APARTMENT", label: "아파트", detail: "실내 생활 중심" },
      { value: "VILLA", label: "빌라/다세대", detail: "적당한 실내 공간" },
      { value: "HOUSE", label: "단독주택", detail: "마당이나 넓은 공간 가능" }
    ]
  },
  {
    key: "activityLevel",
    title: "평소 활동량은 어느 정도인가요?",
    subtitle: "산책과 놀이 시간을 기준으로 골라주세요.",
    options: [
      { value: "LOW", label: "낮아요", detail: "짧고 조용한 산책 선호" },
      { value: "MEDIUM", label: "보통이에요", detail: "하루 한두 번 산책 가능" },
      { value: "HIGH", label: "높아요", detail: "긴 산책과 야외활동을 좋아해요" }
    ]
  },
  {
    key: "preferredSize",
    title: "선호하는 체구가 있나요?",
    subtitle: "모르겠다면 상관없음을 선택해도 괜찮아요.",
    options: [
      { value: "SMALL", label: "소형", detail: "8kg 이하" },
      { value: "MEDIUM", label: "중형", detail: "8~20kg" },
      { value: "LARGE", label: "대형", detail: "20kg 이상" },
      { value: "ANY", label: "상관없음", detail: "성향을 더 우선할게요" }
    ]
  },
  {
    key: "preferredSpecies",
    title: "어떤 동물을 더 마음에 두고 있나요?",
    subtitle: "선호가 없으면 강아지와 고양이를 함께 추천해드려요.",
    options: [
      { value: "DOG", label: "강아지", detail: "산책과 교감 중심" },
      { value: "CAT", label: "고양이", detail: "독립적인 생활 리듬" },
      { value: "ANY", label: "상관없음", detail: "조건에 맞춰 추천" }
    ]
  },
  {
    key: "lifestyle",
    title: "마지막으로 생활 리듬을 알려주세요",
    subtitle: "아이, 다른 반려동물, 근무 시간을 함께 반영할게요."
  }
];

const initialForm = {
  housingType: "APARTMENT",
  activityLevel: "MEDIUM",
  preferredSize: "ANY",
  preferredSpecies: "ANY",
  hasChildren: false,
  hasOtherPets: false,
  workHoursPerDay: 8
};

function animalMetaText(animal) {
  return [animal?.ageText, animal?.sex, animal?.weightText].filter(Boolean).join(" | ") || "정보 준비 중";
}

export default function MatchingPage() {
  const [stepIndex, setStepIndex] = useState(0);
  const [form, setForm] = useState(initialForm);
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const currentStep = steps[stepIndex];
  const progress = useMemo(() => Math.round(((stepIndex + 1) / steps.length) * 100), [stepIndex]);

  function selectOption(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function nextStep() {
    setStepIndex((current) => Math.min(current + 1, steps.length - 1));
  }

  function previousStep() {
    setStepIndex((current) => Math.max(current - 1, 0));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setLoading(true);
    setError("");
    setResults([]);

    try {
      const data = await submitMatching(form);
      setResults(data ?? []);
    } catch {
      setError("맞춤 추천을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="paw-matching-page stack">
      <section className="paw-matching-hero">
        <div>
          <p className="eyebrow">Find My Perfect Pet</p>
          <h2>나의 생활 리듬에 맞는 가족을 찾아볼까요?</h2>
          <p>
            주거 형태, 활동량, 근무 시간 같은 조건을 바탕으로 공공데이터 보호동물 목록에서
            지금 가장 잘 맞는 후보 5마리를 추천해드려요.
          </p>
        </div>
        <div className="paw-matching-orbit">
          <span>5 steps</span>
          <strong>{progress}%</strong>
        </div>
      </section>

      <section className="paw-matching-card">
        <div className="paw-matching-progress" aria-label="진행률">
          <div style={{ width: `${progress}%` }} />
        </div>

        <form onSubmit={handleSubmit}>
          <div className="paw-matching-step-head">
            <span>Step {stepIndex + 1} / {steps.length}</span>
            <h3>{currentStep.title}</h3>
            <p>{currentStep.subtitle}</p>
          </div>

          {currentStep.key !== "lifestyle" ? (
            <div className="paw-matching-options">
              {currentStep.options.map((option) => (
                <button
                  key={option.value}
                  type="button"
                  className={`paw-matching-option ${form[currentStep.key] === option.value ? "active" : ""}`}
                  onClick={() => selectOption(currentStep.key, option.value)}
                >
                  <strong>{option.label}</strong>
                  <span>{option.detail}</span>
                </button>
              ))}
            </div>
          ) : (
            <div className="paw-lifestyle-panel">
              <label className="paw-toggle-row">
                <input
                  type="checkbox"
                  checked={form.hasChildren}
                  onChange={(event) => selectOption("hasChildren", event.target.checked)}
                />
                <span>아이와 함께 지낼 예정이에요</span>
              </label>
              <label className="paw-toggle-row">
                <input
                  type="checkbox"
                  checked={form.hasOtherPets}
                  onChange={(event) => selectOption("hasOtherPets", event.target.checked)}
                />
                <span>이미 함께 사는 반려동물이 있어요</span>
              </label>
              <label className="paw-work-hours">
                <span>하루 평균 집을 비우는 시간</span>
                <strong>{form.workHoursPerDay}시간</strong>
                <input
                  type="range"
                  min="0"
                  max="12"
                  value={form.workHoursPerDay}
                  onChange={(event) => selectOption("workHoursPerDay", Number(event.target.value))}
                />
              </label>
            </div>
          )}

          <div className="paw-matching-actions">
            <button type="button" className="secondary-button" disabled={stepIndex === 0} onClick={previousStep}>
              이전
            </button>
            {stepIndex < steps.length - 1 ? (
              <button type="button" className="primary-button" onClick={nextStep}>
                다음
              </button>
            ) : (
              <button type="submit" className="primary-button" disabled={loading}>
                {loading ? "추천 찾는 중..." : "결과 보기"}
              </button>
            )}
          </div>
        </form>
      </section>

      {error ? <section className="paw-message-card">{error}</section> : null}

      {results.length ? (
        <section className="paw-section">
          <div className="paw-section-head">
            <h3>추천 결과</h3>
            <Link to="/animals">전체 동물 보기 &gt;</Link>
          </div>
          <div className="paw-match-result-grid">
            {results.map((animal) => (
              <Link
                key={`${animal.desertionNo}-${animal.noticeNumber}`}
                to={`/shelter/animal/detail/${createAnimalSlug(animal)}`}
                className="paw-match-card"
              >
                <div className="paw-match-photo">
                  <span className="paw-match-score">{animal.matchScore}점</span>
                  {animal.imageUrl ? <img src={animal.imageUrl} alt={animal.name} /> : <div>NO IMAGE</div>}
                </div>
                <div className="paw-match-copy">
                  <strong>{animal.name || "보호동물"}</strong>
                  <p>{animalMetaText(animal)}</p>
                  <ul>
                    {(animal.matchReasons ?? []).slice(0, 3).map((reason) => (
                      <li key={reason}>{reason}</li>
                    ))}
                  </ul>
                </div>
              </Link>
            ))}
          </div>
        </section>
      ) : null}
    </div>
  );
}
