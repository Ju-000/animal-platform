import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { createStory, fetchAnimals } from "../lib/api";

export default function StoryWritePage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ title: "", content: "", animalNo: "" });
  const [image, setImage] = useState(null);
  const [previewUrl, setPreviewUrl] = useState("");
  const [query, setQuery] = useState("");
  const [animals, setAnimals] = useState([]);
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    fetchAnimals({ page: 1, size: 30 })
      .then((data) => setAnimals(data.content ?? []))
      .catch(() => setAnimals([]));
  }, []);

  useEffect(() => {
    if (!image) {
      setPreviewUrl("");
      return;
    }
    const nextUrl = URL.createObjectURL(image);
    setPreviewUrl(nextUrl);
    return () => URL.revokeObjectURL(nextUrl);
  }, [image]);

  const filteredAnimals = useMemo(() => {
    const keyword = query.trim();
    if (!keyword) return animals.slice(0, 6);
    return animals
      .filter((animal) => `${animal.name} ${animal.noticeNumber} ${animal.region}`.includes(keyword))
      .slice(0, 6);
  }, [animals, query]);

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    if (form.content.trim().length < 50) {
      setMessage("내용은 50자 이상 작성해주세요.");
      return;
    }

    setSubmitting(true);
    setMessage("");
    try {
      const story = await createStory({ ...form, image });
      navigate(`/stories/${story.id}`);
    } catch {
      setMessage("후기를 저장하지 못했습니다. 로그인 상태와 입력 내용을 확인해주세요.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="paw-story-write-page stack">
      <section className="paw-list-header">
        <div>
          <p className="eyebrow">WRITE YOUR STORY</p>
          <h2>입양 후기를 작성해 주세요</h2>
          <p className="paw-sub-copy">누군가에게는 당신의 기록이 입양을 결심하는 작은 용기가 됩니다.</p>
        </div>
      </section>

      <form className="paw-story-write-form" onSubmit={submit}>
        <label>
          <span>제목</span>
          <input name="title" value={form.title} onChange={updateField} required maxLength={160} placeholder="예: 겁 많던 아이가 우리 집 막내가 되기까지" />
        </label>

        <label>
          <span>내용</span>
          <textarea name="content" value={form.content} onChange={updateField} required minLength={50} rows={10} placeholder="50자 이상 작성해주세요." />
        </label>

        <label>
          <span>사진 업로드</span>
          <input type="file" accept="image/*" onChange={(event) => setImage(event.target.files?.[0] ?? null)} />
        </label>
        {previewUrl ? <img className="paw-story-preview" src={previewUrl} alt="업로드 미리보기" /> : null}

        <section className="paw-animal-link-box">
          <div>
            <span>입양한 동물 연결하기</span>
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="공고번호, 품종, 지역으로 검색" />
          </div>
          <div className="paw-animal-search-results">
            {filteredAnimals.map((animal) => (
              <button
                key={`${animal.noticeNumber}-${animal.id}`}
                type="button"
                className={form.animalNo === animal.noticeNumber ? "active" : ""}
                onClick={() => setForm((current) => ({ ...current, animalNo: animal.noticeNumber }))}
              >
                <strong>{animal.name}</strong>
                <span>{animal.noticeNumber} · {animal.region}</span>
              </button>
            ))}
          </div>
          {form.animalNo ? <p className="paw-form-message">연결된 동물: {form.animalNo}</p> : null}
        </section>

        <button className="primary-button" type="submit" disabled={submitting}>
          {submitting ? "저장 중..." : "후기 등록하기"}
        </button>
        {message ? <p className="paw-form-message">{message}</p> : null}
      </form>
    </div>
  );
}
