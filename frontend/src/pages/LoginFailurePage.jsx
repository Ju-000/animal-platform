import { useMemo } from "react";
import { useLocation } from "react-router-dom";

export default function LoginFailurePage() {
  const location = useLocation();
  const params = useMemo(() => new URLSearchParams(location.search), [location.search]);

  return (
    <section className="info-card">
      <p className="eyebrow">Login Failure</p>
      <h2>소셜 로그인에 실패했습니다</h2>
      <p>{params.get("message") ?? "로그인 처리 중 오류가 발생했습니다."}</p>
    </section>
  );
}