import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import HeroBannerCarousel from "../components/home/HeroBannerCarousel";
import { absoluteAssetUrl, fetchAnimals, fetchCampaigns, fetchRecommendedAnimals, fetchStatsSummary } from "../lib/api";
import { getRecentAnimals } from "../lib/recentAnimals";
import { createAnimalSlug } from "../lib/slug";

const recommendationPresets = [
  { id: "small-home", label: "아파트·소형 주거", matcher: (animal) => extractWeight(animal?.weightText) <= 8 },
  { id: "first-pet", label: "초보 보호자", matcher: (animal) => String(animal?.specialMark ?? "").length < 40 },
  {
    id: "young",
    label: "어린 개체",
    matcher: (animal) => String(animal?.ageText ?? "").includes("개월") || String(animal?.ageText ?? "").includes("60일")
  },
  { id: "cat-family", label: "고양이 가족", matcher: (animal) => String(animal?.species ?? "").includes("고양이") }
];

const regions = [
  "서울특별시",
  "부산광역시",
  "대구광역시",
  "인천광역시",
  "광주광역시",
  "대전광역시",
  "울산광역시",
  "세종특별자치시",
  "경기도",
  "강원특별자치도",
  "충청북도",
  "충청남도",
  "전북특별자치도",
  "전라남도",
  "경상북도",
  "경상남도",
  "제주특별자치도"
];

function animalMetaText(animal) {
  const parts = [animal?.ageText, animal?.sex, animal?.weightText].filter(Boolean);
  return parts.length ? parts.join(" | ") : "정보 준비 중";
}

function extractWeight(weightText) {
  if (!weightText) return Number.POSITIVE_INFINITY;
  const match = String(weightText).match(/(\d+(\.\d+)?)/);
  return match ? Number(match[1]) : Number.POSITIVE_INFINITY;
}

function campaignImageSrc(imageUrl) {
  if (!imageUrl) return "";
  if (imageUrl.startsWith("/uploads")) return absoluteAssetUrl(imageUrl);
  return imageUrl;
}

function campaignPercent(campaign) {
  const current = Number(campaign.currentAmount ?? campaign.current ?? 0);
  const goal = Number(campaign.goalAmount ?? campaign.goal ?? 1);
  return Math.min(100, Math.round((current / goal) * 100));
}

function AnimalCard({ animal, className = "paw-featured-card" }) {
  return (
    <Link to={`/shelter/animal/detail/${createAnimalSlug(animal)}`} className={className}>
      <div className={className === "paw-recent-animal-card" ? "paw-recent-animal-photo" : "paw-featured-photo"}>
        {animal.imageUrl ? <img src={animal.imageUrl} alt={animal.name} /> : <div>NO IMAGE</div>}
        {animal.urgencyBadge ? (
          <span className={`paw-urgency-badge ${animal.urgencyBadge === "마감임박" ? "urgent" : "long-wait"}`}>
            {animal.urgencyBadge}
          </span>
        ) : null}
      </div>
      {className === "paw-recent-animal-card" ? (
        <div className="paw-recent-animal-info">
          <strong>{animal.noticeNumber}</strong>
          <p>{animalMetaText(animal)}</p>
        </div>
      ) : (
        <>
          <strong>{animal.noticeNumber}</strong>
          <p>{animalMetaText(animal)}</p>
        </>
      )}
    </Link>
  );
}

export default function HomePage() {
  const navigate = useNavigate();
  const [animals, setAnimals] = useState([]);
  const [campaigns, setCampaigns] = useState([]);
  const [recentAnimals, setRecentAnimals] = useState([]);
  const [stats, setStats] = useState(null);
  const [statsError, setStatsError] = useState(false);
  const [recommendationMode, setRecommendationMode] = useState("small-home");
  const [selectedRegion, setSelectedRegion] = useState(null);
  const [featuredAnimals, setFeaturedAnimals] = useState([]);

  useEffect(() => {
    setRecentAnimals(getRecentAnimals());
    fetchAnimals({ page: 1, size: 12 }).then((data) => setAnimals(data.content ?? [])).catch(() => setAnimals([]));
    fetchCampaigns().then((data) => setCampaigns(Array.isArray(data) ? data : [])).catch(() => setCampaigns([]));
    fetchStatsSummary()
      .then((data) => {
        setStats(data);
        setStatsError(false);
      })
      .catch(() => {
        setStats(null);
        setStatsError(true);
      });
  }, []);

  useEffect(() => {
    fetchRecommendedAnimals({ size: 10, orgNm: selectedRegion })
      .then((data) => setFeaturedAnimals(Array.isArray(data) ? data : data.content ?? []))
      .catch(() => setFeaturedAnimals([]));
  }, [selectedRegion]);

  const regionIcons = useMemo(
    () => [
      { id: "region-all", label: "전체", orgNm: null },
      ...regions.map((region, index) => ({
        id: `region-${index}-${region}`,
        label: region,
        orgNm: region
      }))
    ],
    []
  );

  const recommendedAnimals = useMemo(() => {
    const preset = recommendationPresets.find((item) => item.id === recommendationMode) ?? recommendationPresets[0];
    const filtered = animals.filter((animal) => preset.matcher(animal));
    return (filtered.length ? filtered : animals).slice(0, 5);
  }, [animals, recommendationMode]);

  const todayLabel = useMemo(() => {
    const now = new Date();
    const year = String(now.getFullYear()).slice(2);
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    return `${year}.${month}.${day}`;
  }, []);

  const rescuedCount = stats ? Number(stats.totalAnimalsCount ?? 0) : null;
  const adoptionRate = stats ? Number(stats.adoptionRate ?? 0) : null;
  const euthanasiaRate = stats ? Number(stats.euthanasiaRate ?? 0) : null;

  return (
    <div className="paw-home stack">
      <section className="paw-main-banner">
        <HeroBannerCarousel />
      </section>

      <section className="paw-section">
        <div className="paw-section-head">
          <h3>오늘의 추천 입양 동물</h3>
          <Link to="/animals">더보기 &gt;</Link>
        </div>
        <div className="paw-region-scroll-wrap" aria-label="지역별 추천 동물 필터">
          <div className="paw-region-scroll-inner">
            {[...regionIcons, ...regionIcons].map((region, index) => (
              <button
                key={`${region.id}-${index}`}
                type="button"
                className={`paw-region-badge ${selectedRegion === region.orgNm ? "active" : ""}`}
                onClick={() => setSelectedRegion(region.orgNm)}
                aria-hidden={index >= regionIcons.length ? "true" : undefined}
                tabIndex={index >= regionIcons.length ? -1 : undefined}
              >
                {region.label}
              </button>
            ))}
          </div>
        </div>
        <div className="paw-featured-grid">
          {(featuredAnimals.length ? featuredAnimals : animals).slice(0, 5).map((animal) => (
            <AnimalCard key={animal.id} animal={animal} />
          ))}
        </div>
      </section>

      <section className="paw-section">
        <div className="paw-section-head">
          <h3>후원 캠페인</h3>
          <Link to="/donations">캠페인 더보기 &gt;</Link>
        </div>
        <div className="paw-donation-campaign-grid">
          {campaigns.slice(0, 4).map((campaign) => {
            const percent = campaignPercent(campaign);
            const imageUrl = campaignImageSrc(campaign.imageUrl ?? campaign.image);

            return (
              <article key={campaign.id} className="paw-donation-campaign-card" onClick={() => navigate("/donations")}>
                <div className="paw-donation-campaign-image">
                  {imageUrl ? (
                    <img
                      src={imageUrl}
                      alt={campaign.title}
                      onError={(event) => {
                        event.currentTarget.style.display = "none";
                      }}
                    />
                  ) : null}
                  <span className="paw-donation-campaign-fallback">🐾</span>
                </div>
                <div className="paw-donation-campaign-body">
                  <span className="paw-donation-campaign-category">{campaign.category}</span>
                  <h4>{campaign.title}</h4>
                  <div className="paw-donation-campaign-progress" aria-label={`${percent}% 달성`}>
                    <div style={{ width: `${percent}%` }} />
                  </div>
                  <div className="paw-donation-campaign-meta">
                    <strong>{percent}% 달성</strong>
                    <span>{Number(campaign.participants ?? 0).toLocaleString("ko-KR")}명 참여</span>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      </section>

      <section className="paw-section">
        <div className="paw-section-head">
          <h3>조건별 맞춤 추천</h3>
          <Link to="/animals">더보기 &gt;</Link>
        </div>
        <div className="paw-tag-row">
          {recommendationPresets.map((preset) => (
            <button
              key={preset.id}
              type="button"
              className={`paw-tag ${recommendationMode === preset.id ? "active" : ""}`}
              onClick={() => setRecommendationMode(preset.id)}
            >
              {preset.label}
            </button>
          ))}
        </div>
        <div className="paw-recent-animal-grid">
          {recommendedAnimals.slice(0, 5).map((animal) => (
            <AnimalCard key={animal.id} animal={animal} className="paw-recent-animal-card" />
          ))}
        </div>
      </section>

      <section className="paw-section">
        <div className="paw-section-head">
          <h3>최근 본 동물</h3>
          <Link to="/animals">더보기 &gt;</Link>
        </div>
        <div className="paw-recent-animal-grid">
          {recentAnimals.slice(0, 5).map((animal) => (
            <AnimalCard key={animal.slug ?? animal.id} animal={animal} className="paw-recent-animal-card" />
          ))}
          {recentAnimals.length === 0 ? <div className="paw-message-card">아직 최근 본 동물이 없습니다.</div> : null}
        </div>
      </section>

      <section className="paw-floating-stats">
        <div>
          <span>{todayLabel} 유기동물 통계</span>
        </div>
        <strong>구조 <em className="paw-stat-green">{rescuedCount == null ? "-" : rescuedCount}</em> 마리</strong>
        <strong>입양률 <em className="paw-stat-blue">{adoptionRate == null ? "-" : `${adoptionRate.toFixed(1)} %`}</em></strong>
        <strong>안락사율 <em className="paw-stat-red">{euthanasiaRate == null ? "-" : `${euthanasiaRate.toFixed(1)} %`}</em></strong>
        <Link to="/stats">자세히 보기 &gt;</Link>
      </section>

      {statsError ? <section className="paw-message-card">통계 정보를 불러오지 못했습니다.</section> : null}
    </div>
  );
}
