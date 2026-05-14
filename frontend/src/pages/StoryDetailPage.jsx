import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { absoluteAssetUrl, deleteStory, fetchMyProfile, fetchStory, toggleStoryLike } from "../lib/api";

function formatDate(value) {
  if (!value) return "";
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "long", timeStyle: "short" }).format(new Date(value));
}

export default function StoryDetailPage() {
  const { storyId } = useParams();
  const navigate = useNavigate();
  const [story, setStory] = useState(null);
  const [me, setMe] = useState(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");

  useEffect(() => {
    let active = true;
    Promise.all([
      fetchStory(storyId),
      fetchMyProfile().catch(() => null)
    ])
      .then(([storyData, profile]) => {
        if (!active) return;
        setStory(storyData);
        setMe(profile);
      })
      .catch(() => {
        if (!active) return;
        setMessage("입양 후기를 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!active) return;
        setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [storyId]);

  async function handleLike() {
    try {
      const result = await toggleStoryLike(story.id);
      setStory((current) => ({ ...current, liked: result.liked, likeCount: result.likeCount }));
    } catch {
      setMessage("로그인 후 좋아요를 누를 수 있습니다.");
    }
  }

  async function handleDelete() {
    try {
      await deleteStory(story.id);
      navigate("/stories");
    } catch {
      setMessage("삭제하지 못했습니다.");
    }
  }

  if (loading) return <section className="paw-message-card">입양 후기를 불러오는 중입니다.</section>;
  if (!story) return <section className="paw-message-card">{message || "입양 후기를 찾을 수 없습니다."}</section>;

  const isOwner = me?.id === story.userId;

  return (
    <article className="paw-story-detail stack">
      <header className="paw-story-detail-head">
        <Link to="/stories" className="paw-link-arrow">← 목록으로</Link>
        <h2>{story.title}</h2>
        <div className="paw-story-detail-meta">
          <span>{story.userNickname}</span>
          <span>{formatDate(story.createdAt)}</span>
          {story.animalNo ? <span>연결 동물 {story.animalNo}</span> : null}
        </div>
      </header>

      {story.imageUrl ? (
        <div className="paw-story-detail-image">
          <img src={absoluteAssetUrl(story.imageUrl)} alt={story.title} />
        </div>
      ) : null}

      <section className="paw-story-detail-body">
        {story.content.split("\n").map((line, index) => (
          <p key={`${line}-${index}`}>{line}</p>
        ))}
      </section>

      <div className="paw-detail-actions">
        <button type="button" className={`paw-action ${story.liked ? "green" : "orange"}`} onClick={handleLike}>
          좋아요 {story.likeCount}
        </button>
        {isOwner ? (
          <button type="button" className="paw-action dark" onClick={handleDelete}>
            삭제하기
          </button>
        ) : null}
      </div>
      {message ? <p className="paw-form-message">{message}</p> : null}
    </article>
  );
}
