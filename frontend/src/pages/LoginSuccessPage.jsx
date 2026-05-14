import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { fetchMyProfile } from "../lib/api";

export default function LoginSuccessPage() {
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;

    fetchMyProfile()
      .then((me) => {
        if (!active) return;
        setProfile(me);
      })
      .finally(() => {
        if (!active) return;
        setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  return (
    <section className="info-card">
      <p className="eyebrow">Login Success</p>
      <h2>소셜 로그인이 완료되었습니다</h2>
      {loading ? <p>회원 정보를 불러오는 중입니다.</p> : null}
      {!loading ? <p>provider: {params.get("provider") ?? "-"}</p> : null}
      {!loading ? <p>name: {profile?.name ?? params.get("name") ?? "-"}</p> : null}
      {!loading ? <p>email: {profile?.email ?? params.get("email") ?? "-"}</p> : null}
      <p>이제 마이페이지에서 내 후원 내역과 계정 정보를 확인할 수 있습니다.</p>
      <div className="card-actions">
        <Link className="inline-link" to="/me">마이페이지로 이동</Link>
        <Link className="inline-link" to="/donations">후원하러 가기</Link>
      </div>
    </section>
  );
}