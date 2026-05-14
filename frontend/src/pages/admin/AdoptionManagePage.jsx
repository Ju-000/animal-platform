import { useEffect, useState } from "react";
import { fetchAdminAdoptions, updateAdminAdoptionStatus } from "../../lib/api";

const FILTERS = [
  { value: "ALL", label: "전체" },
  { value: "PENDING", label: "대기중" },
  { value: "APPROVED", label: "승인" },
  { value: "REJECTED", label: "거절" }
];

const STATUS_LABELS = {
  PENDING: "대기중",
  APPROVED: "승인",
  REJECTED: "거절"
};

export default function AdoptionManagePage() {
  const [status, setStatus] = useState("ALL");
  const [pageData, setPageData] = useState({ content: [] });
  const [loading, setLoading] = useState(true);

  async function loadAdoptions(nextStatus = status) {
    setLoading(true);
    try {
      const data = await fetchAdminAdoptions({ status: nextStatus, page: 0, size: 50 });
      setPageData(data ?? { content: [] });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAdoptions(status);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status]);

  async function changeStatus(id, nextStatus) {
    await updateAdminAdoptionStatus(id, nextStatus);
    await loadAdoptions();
  }

  return (
    <div className="stack paw-admin-page">
      <section className="paw-list-header">
        <div>
          <p className="eyebrow">ADOPTION ADMIN</p>
          <h2>입양 신청 관리</h2>
          <p className="paw-sub-copy">입양 신청을 확인하고 승인 또는 거절 처리합니다.</p>
        </div>
      </section>

      <div className="paw-admin-filter-tabs">
        {FILTERS.map((filter) => (
          <button
            key={filter.value}
            type="button"
            className={status === filter.value ? "active" : ""}
            onClick={() => setStatus(filter.value)}
          >
            {filter.label}
          </button>
        ))}
      </div>

      <section className="paw-campaign-admin-card">
        {loading ? (
          <div className="paw-message-card">입양 신청 목록을 불러오는 중입니다.</div>
        ) : (
          <table className="paw-admin-table">
            <thead>
              <tr>
                <th>신청자</th>
                <th>연락처</th>
                <th>동물번호</th>
                <th>신청일</th>
                <th>상태</th>
                <th>액션</th>
              </tr>
            </thead>
            <tbody>
              {(pageData.content ?? []).map((item) => (
                <tr key={item.id}>
                  <td>{item.applicantName}</td>
                  <td>{item.applicantPhone}</td>
                  <td>{item.animalNo}</td>
                  <td>{item.appliedAt ? new Date(item.appliedAt).toLocaleString("ko-KR") : "-"}</td>
                  <td><span className={`paw-status-badge ${item.status?.toLowerCase()}`}>{STATUS_LABELS[item.status] ?? item.status}</span></td>
                  <td>
                    <div className="paw-admin-actions">
                      <button type="button" disabled={item.status === "APPROVED"} onClick={() => changeStatus(item.id, "APPROVED")}>승인</button>
                      <button type="button" className="danger" disabled={item.status === "REJECTED"} onClick={() => changeStatus(item.id, "REJECTED")}>거절</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
