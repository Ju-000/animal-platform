import { useNavigate } from "react-router-dom";

export default function PageFallback() {
  const navigate = useNavigate();

  return (
    <section className="paw-page-fallback" role="alert">
      <span aria-hidden="true">{"🐾"}</span>
      <h2>{"페이지를 불러오는 중 문제가 생겼어요"}</h2>
      <p>{"잠시 후 다시 시도하거나 홈으로 돌아가주세요"}</p>
      <div className="paw-page-fallback-actions">
        <button type="button" className="paw-action green" onClick={() => navigate("/")}>
          {"홈으로"}
        </button>
        <button type="button" className="paw-action orange" onClick={() => window.location.reload()}>
          {"새로고침"}
        </button>
      </div>
    </section>
  );
}
