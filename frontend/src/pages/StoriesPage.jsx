import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import storyBanner from "../assets/입양후기/입양후기.png";
import { absoluteAssetUrl, fetchMyProfile, fetchStories } from "../lib/api";

function formatDate(value) {
  if (!value) return "";
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium" }).format(new Date(value));
}

export default function StoriesPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [profile, setProfile] = useState(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");

    fetchStories({ page, size: 9 })
      .then((payload) => {
        if (!active) return;
        setData(payload);
      })
      .catch(() => {
        if (!active) return;
        setError("입양 후기를 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!active) return;
        setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [page]);

  useEffect(() => {
    fetchMyProfile().then(setProfile).catch(() => setProfile(null));
  }, []);

  const stories = data?.content ?? [];
  const isAngel = profile?.tier === "ANGEL";

  return (
    <div className="paw-stories-page stack">
      <section
        style={{
          position: "relative",
          width: "100%",
          overflow: "hidden",
          borderRadius: "16px",
          marginBottom: "32px",
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
        }}
      >
        <img
          src={storyBanner}
          alt="입양 후기"
          style={{
            width: "auto",
            maxWidth: "100%",
            maxHeight: "300px",
            height: "auto",
            display: "block",
            objectFit: "contain",
          }}
        />
        <button
          type="button"
          onClick={() => navigate("/stories/write")}
          style={{
            position: "absolute",
            bottom: "24px",
            right: "24px",
            background: "#F97316",
            color: "white",
            border: "none",
            borderRadius: "12px",
            padding: "12px 24px",
            fontSize: "16px",
            fontWeight: "bold",
            cursor: "pointer",
          }}
        >
          후기 작성하기
        </button>
      </section>

      <section className={isAngel ? "paw-angel-story-section" : "paw-angel-story-section locked"}>
        <div>
          <p className="eyebrow">ANGEL THANKS</p>
          <h3>천사 후원자 감사 스토리</h3>
          <p>
            {isAngel
              ? "천사 후원자님들의 따뜻한 마음으로 이어진 특별한 이야기를 전해드려요."
              : "천사 후원자 전용 콘텐츠입니다. 누적 후원 500,000원 이상부터 열람할 수 있어요."}
          </p>
        </div>
        <div className="paw-angel-story-card">
          <span>{isAngel ? "👼" : "🔒"}</span>
          <strong>{isAngel ? "이번 주 지원받은 아이들의 소식" : "잠긴 감사 스토리"}</strong>
          <p>
            {isAngel
              ? "보호소 아이들이 새로운 가족을 기다리며 건강을 지키고 있어요."
              : "천사 후원자가 되면 특별 감사 스토리가 열립니다."}
          </p>
        </div>
      </section>

      {loading ? <section className="paw-message-card">입양 후기를 불러오는 중입니다.</section> : null}
      {error ? <section className="paw-message-card">{error}</section> : null}

      {!loading && !error ? (
        <>
          <section className="paw-story-board-grid">
            {stories.length ? (
              stories.map((story) => (
                <Link key={story.id} to={`/stories/${story.id}`} className="paw-story-board-card">
                  <div className="paw-story-board-photo">
                    {story.imageUrl ? (
                      <img src={absoluteAssetUrl(story.imageUrl)} alt={story.title} />
                    ) : (
                      <div>다시, 가족</div>
                    )}
                  </div>
                  <div className="paw-story-board-copy">
                    <strong>{story.title}</strong>
                    <p>{story.userNickname}</p>
                    <div>
                      <span>좋아요 {story.likeCount}</span>
                      <span>{formatDate(story.createdAt)}</span>
                    </div>
                  </div>
                </Link>
              ))
            ) : (
              <article className="paw-message-card">아직 등록된 입양 후기가 없습니다. 첫 이야기를 남겨주세요.</article>
            )}
          </section>

          <nav className="pagination paw-pagination">
            <button className="page-button" type="button" disabled={page <= 0} onClick={() => setPage((current) => current - 1)}>
              이전
            </button>
            <span className="page-button active">{page + 1}</span>
            <button
              className="page-button"
              type="button"
              disabled={data?.last ?? true}
              onClick={() => setPage((current) => current + 1)}
            >
              다음
            </button>
          </nav>
        </>
      ) : null}
    </div>
  );
}
