import { Link } from "react-router-dom";
import { useEffect, useMemo, useState } from "react";
import { addFavorite, fetchAnimalDetail, fetchFavorites, removeFavorite } from "../lib/api";
import { createAnimalSlug } from "../lib/slug";

const statusToneMap = {
  PROTECTING: "green",
  IN_COUNSELING: "warm",
  COMPLETED: "dark"
};

const statusLabelMap = {
  PROTECTING: "보호중",
  IN_COUNSELING: "상담 진행 중",
  COMPLETED: "완료",
  OTHER: "기타"
};

export default function FavoritesPage() {
  const [favoriteNos, setFavoriteNos] = useState([]);
  const [animals, setAnimals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");

    fetchFavorites()
      .then(async (animalNos) => {
        if (!active) return;
        const normalizedNos = animalNos ?? [];
        setFavoriteNos(normalizedNos);

        const detailResults = await Promise.allSettled(
          normalizedNos.map((animalNo) => fetchAnimalDetail(animalNo))
        );
        if (!active) return;
        setAnimals(detailResults.filter((result) => result.status === "fulfilled").map((result) => result.value));
      })
      .catch((exception) => {
        if (!active) return;
        setFavoriteNos([]);
        setAnimals([]);
        setError(exception?.message?.includes("401") ? "로그인이 필요한 기능입니다." : "관심 동물을 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!active) return;
        setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const favoriteSet = useMemo(() => new Set(favoriteNos), [favoriteNos]);
  const summary = useMemo(() => ({
    total: animals.length,
    counseling: animals.filter((item) => item.serviceStatus === "IN_COUNSELING").length,
    completed: animals.filter((item) => item.serviceStatus === "COMPLETED").length
  }), [animals]);

  async function handleFavoriteToggle(animal) {
    const animalNo = animal.noticeNumber ?? String(animal.id);
    const isFavorited = favoriteSet.has(animalNo);

    try {
      if (isFavorited) {
        await removeFavorite(animalNo);
        setFavoriteNos((current) => current.filter((candidate) => candidate !== animalNo));
        setAnimals((current) => current.filter((candidate) => (candidate.noticeNumber ?? String(candidate.id)) !== animalNo));
      } else {
        await addFavorite(animalNo);
        setFavoriteNos((current) => [...new Set([...current, animalNo])]);
      }
    } catch {
      setError("관심 동물 저장 상태를 변경하지 못했습니다.");
    }
  }

  if (loading) {
    return <section className="paw-message-card">관심 동물을 불러오는 중입니다.</section>;
  }

  if (error) {
    return <section className="paw-message-card">{error}</section>;
  }

  return (
    <div className="stack paw-favorites-page">
      <section className="paw-list-header">
        <div>
          <p className="eyebrow">FAVORITE ALERTS</p>
          <h2>관심 동물</h2>
          <p className="paw-sub-copy">내가 저장한 동물의 보호 상태와 상세 정보를 다시 확인할 수 있습니다.</p>
        </div>
      </section>

      <section className="dashboard-grid paw-admin-grid">
        <article className="info-card">
          <h3>관심 동물</h3>
          <p>{summary.total}건</p>
          <span>현재 저장된 동물 수</span>
        </article>
        <article className="info-card">
          <h3>상담 진행 중</h3>
          <p>{summary.counseling}건</p>
          <span>입양 상담 단계로 이동한 동물</span>
        </article>
        <article className="info-card">
          <h3>완료 상태</h3>
          <p>{summary.completed}건</p>
          <span>입양/분양 또는 종료 상태</span>
        </article>
      </section>

      {animals.length === 0 ? (
        <section className="paw-message-card">아직 저장한 관심 동물이 없습니다.</section>
      ) : null}

      <section className="paw-story-feed-grid">
        {animals.map((animal) => {
          const animalNo = animal.noticeNumber ?? String(animal.id);
          const status = animal.serviceStatus ?? "PROTECTING";
          return (
            <article key={animalNo} className="paw-story-feed-card paw-favorite-alert-card">
              <div className="paw-favorite-alert-head">
                <span className={`paw-badge ${statusToneMap[status] ?? "green"}`}>
                  {statusLabelMap[status] ?? status}
                </span>
                <button
                  type="button"
                  className="paw-action orange"
                  onClick={() => handleFavoriteToggle(animal)}
                  aria-label="관심 동물 해제"
                >
                  ♥ 저장됨
                </button>
              </div>
              <div className="paw-favorite-alert-body">
                <div className="paw-favorite-alert-photo">
                  {animal.imageUrl ? <img src={animal.imageUrl} alt={animal.name} /> : <div>NO IMAGE</div>}
                </div>
                <div>
                  <h4>{animal.name}</h4>
                  <strong>{animal.noticeNumber}</strong>
                  <p>{animal.summary || animal.specialMark || "상세 정보를 확인해보세요."}</p>
                  <p>{animal.shelter?.name ?? "보호소 정보 없음"}</p>
                </div>
              </div>
              <div className="paw-detail-actions">
                <Link className="paw-action dark" to={`/shelter/animal/detail/${createAnimalSlug(animal)}`}>동물 상세</Link>
                <Link className="paw-action orange" to="/donations">후원하기</Link>
              </div>
            </article>
          );
        })}
      </section>
    </div>
  );
}
