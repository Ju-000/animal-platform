export default function AISummaryFallback({ resetErrorBoundary }) {
  return (
    <section className="paw-ai-fallback" role="alert">
      <span aria-hidden="true">{"✨"}</span>
      <p>{"AI 소개를 잠시 불러올 수 없어요"}</p>
      <button type="button" onClick={resetErrorBoundary}>
        {"다시 시도"}
      </button>
    </section>
  );
}
