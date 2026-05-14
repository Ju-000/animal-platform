import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { createAnimalSlug } from "../lib/slug";
import { absoluteAssetUrl, fetchAnimals, fetchCampaigns, fetchShelters } from "../lib/api";

function animalMetaText(animal) {
  const parts = [animal?.ageText, animal?.weightText].filter(Boolean);
  return parts.length ? parts.join(" | ") : "정보 준비 중";
}

function campaignProgress(campaign) {
  const current = Number(campaign?.currentAmount ?? 0);
  const goal = Number(campaign?.goalAmount ?? 1);
  return Math.min(100, Math.round((current / goal) * 100));
}

function campaignImageSrc(imageUrl) {
  if (!imageUrl) return "";
  if (imageUrl.startsWith("/uploads")) return absoluteAssetUrl(imageUrl);
  return imageUrl;
}

function buildShelterStoryFeeds(animals, shelters) {
  const shelterMap = new Map((shelters ?? []).map((shelter) => [shelter.region, shelter]));

  return (animals ?? [])
    .filter((animal) => animal.specialMark || animal.noticeNumber)
    .slice(0, 8)
    .map((animal, index) => ({
      id: `${animal.id}-${index}`,
      title: `${animal.region || "보호소"} 보호 이야기`,
      summary: animal.specialMark || `${animal.name}의 보호와 입양 연결을 진행 중입니다.`,
      animal,
      shelterName: animal.careName || shelterMap.get(animal.region)?.name || animal.region || "보호소"
    }));
}

function buildReconnectFeeds(animals) {
  return (animals ?? [])
    .filter((animal) => animal.noticeNumber && animal.region)
    .slice(0, 6)
    .map((animal, index) => ({
      id: `${animal.id}-reconnect-${index}`,
      title: `${animal.region} 발견 동물 연결`,
      summary: `${animal.noticeNumber} 공고번호로 등록된 동물입니다. 발견 장소와 특징을 확인하고 기존 실종 신고와 비교해보세요.`,
      animal
    }));
}

function buildAdoptionReviews(animals) {
  return (animals ?? [])
    .filter((animal) => {
      const status = String(animal.serviceStatus ?? animal.status ?? "");
      return status.includes("IN_COUNSELING") || status.includes("ADOPT");
    })
    .slice(0, 6)
    .map((animal, index) => ({
      id: `${animal.id}-review-${index}`,
      title: `${animal.name} 입양 연결 후기`,
      summary: animal.specialMark || `${animal.noticeNumber} 개체의 상담이 진행 중이며 입양 연결이 이어지고 있습니다.`,
      animal
    }));
}

export default function StoryPage() {
  const [animals, setAnimals] = useState([]);
  const [shelters, setShelters] = useState([]);
  const [campaigns, setCampaigns] = useState([]);

  useEffect(() => {
    fetchAnimals({ page: 1, size: 20 }).then((data) => setAnimals(data.content ?? [])).catch(() => setAnimals([]));
    fetchShelters().then((data) => setShelters(data ?? [])).catch(() => setShelters([]));
    fetchCampaigns().then((data) => setCampaigns(Array.isArray(data) ? data : [])).catch(() => setCampaigns([]));
  }, []);

  const storyFeeds = useMemo(() => buildShelterStoryFeeds(animals, shelters), [animals, shelters]);
  const reconnectFeeds = useMemo(() => buildReconnectFeeds(animals), [animals]);
  const adoptionReviews = useMemo(() => buildAdoptionReviews(animals), [animals]);

  return (
    <div className="stack paw-story-page">
      <section className="paw-list-header">
        <div>
          <p className="eyebrow">SHELTER STORIES</p>
          <h2>보호소 스토리</h2>
          <p className="paw-sub-copy">구조 이후 보호와 돌봄 과정, 보호소 운영 이야기, 입양 연결의 흐름을 모아봅니다.</p>
        </div>
      </section>

      <section className="paw-section">
        <div className="paw-section-head">
          <h3>최근 보호소 이야기</h3>
          <Link to="/animals">더보기 &gt;</Link>
        </div>
        <div className="paw-story-feed-grid">
          {storyFeeds.map((story) => (
            <article key={story.id} className="paw-story-feed-card">
              <span className="paw-story-feed-date">{story.shelterName}</span>
              <h4>{story.title}</h4>
              <p>{story.summary}</p>
              <Link to={`/shelter/animal/detail/${createAnimalSlug(story.animal)}`}>해당 동물 보기 &gt;</Link>
            </article>
          ))}
        </div>
      </section>

      <section className="paw-section">
        <div className="paw-section-head">
          <h3>입양 연결 후기</h3>
          <Link to="/animals">입양 동물 보기 &gt;</Link>
        </div>
        <div className="paw-story-feed-grid">
          {adoptionReviews.length ? (
            adoptionReviews.map((review) => (
              <article key={review.id} className="paw-story-feed-card">
                <span className="paw-story-feed-date">입양 연결 후기</span>
                <h4>{review.title}</h4>
                <p>{review.summary}</p>
                <Link to={`/shelter/animal/detail/${createAnimalSlug(review.animal)}`}>상세 보기 &gt;</Link>
              </article>
            ))
          ) : (
            <article className="paw-story-feed-card">
              <span className="paw-story-feed-date">안내</span>
              <h4>아직 공개된 입양 연결 후기가 없습니다.</h4>
              <p>상담 진행 또는 입양 완료 상태의 동물이 생기면 후기 섹션이 자동으로 채워집니다.</p>
            </article>
          )}
        </div>
      </section>

      <section className="paw-section">
        <div className="paw-section-head">
          <h3>실종·발견 연결</h3>
          <Link to="/animals">유기동물 보기 &gt;</Link>
        </div>
        <div className="paw-story-feed-grid">
          {reconnectFeeds.map((item) => (
            <article key={item.id} className="paw-story-feed-card reconnect">
              <span className="paw-story-feed-date">실종·발견 연결</span>
              <h4>{item.title}</h4>
              <p>{item.summary}</p>
              <Link to={`/shelter/animal/detail/${createAnimalSlug(item.animal)}`}>발견 동물 상세 확인 &gt;</Link>
            </article>
          ))}
        </div>
      </section>

      <section className="paw-section">
        <div className="paw-section-head">
          <h3>후원 캠페인</h3>
          <Link to="/donations">더보기 &gt;</Link>
        </div>
        <div className="paw-donation-campaign-grid">
          {campaigns.slice(0, 4).map((campaign) => {
            const percent = campaignProgress(campaign);
            const imageUrl = campaignImageSrc(campaign.imageUrl);

            return (
              <article key={campaign.id} className="paw-donation-campaign-card">
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
                  <Link className="paw-campaign-donate-link" to="/donations">후원하기</Link>
                </div>
              </article>
            );
          })}
          {campaigns.length === 0 ? <div className="paw-empty-card">진행 중인 후원 캠페인을 준비하고 있어요.</div> : null}
        </div>
      </section>
    </div>
  );
}
