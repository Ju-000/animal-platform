import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { createAnimalSlug } from "../lib/slug";
import { fetchShelterDetail } from "../lib/api";

const statusLabelMap = {
  ADOPTABLE: "보호중",
  SUPPORT_NEEDED: "후원 필요",
  IN_COUNSELING: "상담 진행 중"
};

const priorityLabelMap = {
  HIGH: "우선 지원",
  MEDIUM: "기본 지원",
  LOW: "일반 지원"
};

export default function ShelterDetailPage() {
  const { shelterId } = useParams();
  const [shelter, setShelter] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    fetchShelterDetail(shelterId)
      .then((data) => {
        if (!active) return;
        setShelter(data);
      })
      .catch(() => {
        if (!active) return;
        setError("보호소 정보를 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!active) return;
        setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [shelterId]);

  const stories = useMemo(() => shelter?.stories ?? [], [shelter]);
  const notices = useMemo(() => {
    if (!shelter) return [];

    const items = [];
    if (shelter.medicalNeedCount > 0) {
      items.push({
        title: "치료 지원 우선 공지",
        summary: `현재 의료지원이 필요한 동물이 ${shelter.medicalNeedCount}마리 있어 치료비와 회복 케어 지원이 우선 필요합니다.`
      });
    }
    if (shelter.counselingCount > 0) {
      items.push({
        title: "입양 상담 진행 안내",
        summary: `현재 ${shelter.counselingCount}건의 입양 상담이 진행 중입니다. 문의 전 보호소 운영시간을 꼭 확인해주세요.`
      });
    }
    if (shelter.protectedAnimalCount > 0) {
      items.push({
        title: "보호 동물 관리 안내",
        summary: `현재 ${shelter.protectedAnimalCount}마리를 보호 중이며 사료, 소모품, 생활 환경 지원이 꾸준히 필요합니다.`
      });
    }

    return items.slice(0, 3);
  }, [shelter]);

  if (loading) {
    return <section className="paw-message-card">보호소 상세 정보를 불러오는 중입니다.</section>;
  }

  if (error || !shelter) {
    return <section className="paw-message-card">{error || "보호소 정보를 찾을 수 없습니다."}</section>;
  }

  const currentCount = Number(shelter.currentCount ?? shelter.protectedAnimalCount ?? 0);
  const estimatedCapacity = Number(shelter.estimatedCapacity ?? 30);
  const capacityRatio = Math.min(100, Math.round((currentCount / Math.max(estimatedCapacity, 1)) * 100));
  const isCritical = shelter.capacityStatus === "CRITICAL";
  const donationGoal = shelter.donationGoal;
  const goalSet = Boolean(donationGoal?.goalSet);
  const goalPercentage = Number(donationGoal?.percentage ?? 0);
  const goalFillPercentage = Math.min(100, Math.max(0, goalPercentage));
  const goalTone = !goalSet ? "empty" : goalPercentage >= 70 ? "success" : goalPercentage >= 30 ? "warning" : "danger";
  const formatWon = (value) => `₩${Number(value ?? 0).toLocaleString()}`;

  return (
    <div className="stack paw-shelter-detail-page">
      <section className="paw-shelter-hero paw-shelter-home-hero">
        <div>
          <p className="eyebrow">SHELTER HOME</p>
          <h2>{shelter.name}</h2>
          <p>{shelter.region}</p>
          <p>{shelter.address}</p>
          <p>{shelter.phone}</p>
          <div className="paw-shelter-meta-row">
            <span>보호중 {currentCount}마리</span>
            <span>수용 기준 {estimatedCapacity}마리</span>
            <span>{shelter.capacityStatusLabel ?? "여유"}</span>
            <span>상담중 {shelter.counselingCount}건</span>
            <span>의료지원 필요 {shelter.medicalNeedCount}건</span>
          </div>
          <div className="paw-capacity-gauge">
            <div className="paw-capacity-gauge-head">
              <strong>수용 현황</strong>
              <span>{currentCount} / {estimatedCapacity}마리</span>
            </div>
            <div className="paw-capacity-gauge-track">
              <div
                className={`paw-capacity-gauge-fill ${shelter.capacityStatus?.toLowerCase() ?? "normal"}`}
                style={{ width: `${Math.max(4, capacityRatio)}%` }}
              />
            </div>
          </div>
        </div>
        <div className="paw-detail-actions">
          <Link
            className={`paw-action ${isCritical ? "danger" : "orange"}`}
            to={`/donations?shelterId=${shelter.id}&shelterName=${encodeURIComponent(shelter.name)}`}
          >
            {isCritical ? "긴급 후원하기" : "보호소 후원"}
          </Link>
          <Link className="paw-action dark" to="/story">보호소 스토리</Link>
        </div>
      </section>

      <section className={`paw-donation-goal-card ${goalTone} ${goalPercentage >= 100 ? "complete" : ""}`}>
        <div className="paw-section-head">
          <div>
            <p className="eyebrow">Monthly Goal</p>
            <h3>이번 달 후원 현황</h3>
          </div>
          {goalSet && goalPercentage >= 100 ? <strong className="paw-goal-complete">목표 달성!</strong> : null}
        </div>
        {goalSet ? (
          <div className="paw-donation-goal-body">
            <p className="paw-donation-goal-amount">
              {formatWon(donationGoal.currentAmount)} / {formatWon(donationGoal.goalAmount)} 달성 ({goalPercentage}%)
            </p>
            <div className="paw-donation-goal-progress" aria-label={`후원 목표 달성률 ${goalPercentage}%`}>
              <div className={`paw-donation-goal-fill ${goalTone}`} style={{ width: `${goalFillPercentage}%` }} />
            </div>
            <p className="paw-donation-goal-days">마감까지 {donationGoal.daysLeft}일 남음</p>
          </div>
        ) : (
          <p className="paw-sub-copy">후원 목표 미설정</p>
        )}
      </section>

      <section className="paw-section">
        <div className="paw-section-head">
          <h3>현재 필요한 지원</h3>
        </div>
        <div className="paw-support-grid">
          {shelter.supportItems.map((item) => (
            <article key={item.title} className="paw-support-card">
              <span className={`paw-badge ${item.priority === "HIGH" ? "warm" : "green"}`}>
                {priorityLabelMap[item.priority] ?? item.priority}
              </span>
              <h4>{item.title}</h4>
              <p>{item.description}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="paw-section">
        <div className="paw-section-head">
          <h3>후원 사용 비중 공개</h3>
        </div>
        <div className="paw-donation-usage-grid">
          {(shelter.usageItems ?? []).map((item) => (
            <article key={item.title} className="paw-donation-usage-card compact">
              <div className="paw-donation-usage-head">
                <strong>{item.title}</strong>
                <span>{item.percent}%</span>
              </div>
              <div className="paw-bar-track compact">
                <div className="paw-bar-fill tone-1" style={{ width: `${Math.max(8, item.percent)}%` }} />
              </div>
              <p>{item.description}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="paw-section">
        <div className="paw-section-head">
          <h3>보호소 공지</h3>
        </div>
        <div className="paw-story-feed-grid">
          {notices.map((notice) => (
            <article key={notice.title} className="paw-story-feed-card">
              <span className="paw-story-feed-date">공지</span>
              <h4>{notice.title}</h4>
              <p>{notice.summary}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="paw-section">
        <div className="paw-section-head">
          <h3>보호소 스토리</h3>
        </div>
        {stories.length ? (
          <div className="paw-story-feed-grid">
            {stories.map((story) => (
              <article key={`${story.noticeNumber}-${story.title}`} className="paw-story-feed-card">
                <span className="paw-story-feed-date">{story.date || "최근 등록"}</span>
                <h4>{story.title}</h4>
                <p>{story.summary}</p>
                <Link to={`/shelter/animal/detail/${story.animalSlug}`}>해당 동물 보기 &gt;</Link>
              </article>
            ))}
          </div>
        ) : (
          <div className="paw-message-card">아직 공개된 보호소 스토리가 없습니다.</div>
        )}
      </section>

      <section className="paw-section">
        <div className="section-title">
          <p className="eyebrow">Protected Animals</p>
          <h2>이 보호소의 동물들</h2>
        </div>
        <div className="paw-animal-grid paw-animal-grid-large">
          {shelter.animals.map((animal) => (
            <article key={animal.id} className="paw-animal-card">
              <Link to={`/shelter/animal/detail/${createAnimalSlug(animal)}`} className="paw-animal-card-link">
                <div className="paw-animal-card-photo">
                  {animal.imageUrl ? <img src={animal.imageUrl} alt={animal.name} /> : <div>NO IMAGE</div>}
                </div>
                <div className="paw-animal-card-body">
                  <span className={`paw-badge ${animal.status === "ADOPTABLE" ? "green" : "warm"}`}>
                    {statusLabelMap[animal.status] ?? animal.status}
                  </span>
                  <h3>{animal.name}</h3>
                  <p>{animal.noticeNumber}</p>
                  <p>{animal.species}</p>
                </div>
              </Link>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}
