export default function ShelterAdminPage() {
  return (
    <div className="stack">
      <section className="split-header">
        <div>
          <p className="eyebrow">Shelter Console</p>
          <h2>보호소 관리 화면</h2>
        </div>
      </section>

      <div className="dashboard-grid">
        <article className="info-card">
          <h3>보호 동물 관리</h3>
          <p>현재 보호 24마리 · 집중 관리 3마리</p>
        </article>
        <article className="info-card">
          <h3>입양 신청</h3>
          <p>대기 6건 · 검토 중 3건</p>
        </article>
        <article className="info-card">
          <h3>후원 현황</h3>
          <p>이번 달 수령액 3,200,000원</p>
        </article>
      </div>
    </div>
  );
}