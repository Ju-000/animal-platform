import { useEffect, useState } from "react";
import { fetchAdminDonations } from "../../lib/api";

const WON = new Intl.NumberFormat("ko-KR");

export default function DonationManagePage() {
  const [data, setData] = useState({ content: [], stats: {} });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchAdminDonations({ page: 0, size: 20 })
      .then((payload) => setData(payload ?? { content: [], stats: {} }))
      .finally(() => setLoading(false));
  }, []);

  const stats = data.stats ?? {};

  return (
    <div className="stack paw-admin-page">
      <section className="paw-list-header">
        <div>
          <p className="eyebrow">DONATION ADMIN</p>
          <h2>후원/결제 관리</h2>
          <p className="paw-sub-copy">일시 후원과 정기 후원 결제 내역을 확인합니다.</p>
        </div>
      </section>

      <section className="paw-admin-stat-grid">
        <article><span>오늘 후원 합계</span><strong>{WON.format(Number(stats.todayDonationAmount ?? 0))}원</strong></article>
        <article><span>이번 달 후원 합계</span><strong>{WON.format(Number(stats.monthDonationAmount ?? 0))}원</strong></article>
        <article><span>전체 후원자 수</span><strong>{WON.format(Number(stats.totalDonorCount ?? 0))}명</strong></article>
      </section>

      <section className="paw-campaign-admin-card">
        {loading ? (
          <div className="paw-message-card">후원 내역을 불러오는 중입니다.</div>
        ) : (
          <table className="paw-admin-table">
            <thead>
              <tr>
                <th>후원자</th>
                <th>보호소</th>
                <th>금액</th>
                <th>유형</th>
                <th>결제일</th>
                <th>상태</th>
              </tr>
            </thead>
            <tbody>
              {(data.content ?? []).map((item) => (
                <tr key={item.id}>
                  <td>{item.donorName || item.donorEmail || "익명"}</td>
                  <td>{item.shelterName}</td>
                  <td>{WON.format(Number(item.amount ?? 0))}원</td>
                  <td>{item.type}</td>
                  <td>{item.paidAt ? new Date(item.paidAt).toLocaleString("ko-KR") : "-"}</td>
                  <td><span className={`paw-status-badge ${String(item.status).toLowerCase()}`}>{item.status}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
