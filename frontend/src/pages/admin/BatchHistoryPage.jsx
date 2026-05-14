import { useEffect, useState } from "react";
import { fetchAdminBatchHistory, runAdminBatch } from "../../lib/api";

const WON = new Intl.NumberFormat("ko-KR");

function duration(startedAt, completedAt) {
  if (!startedAt || !completedAt) return "-";
  const start = new Date(startedAt);
  const end = new Date(completedAt);
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return "-";
  const seconds = Math.max(0, Math.floor((end.getTime() - start.getTime()) / 1000));
  const minutes = Math.floor(seconds / 60);
  const remain = seconds % 60;
  return minutes > 0 ? `${minutes}분 ${remain}초` : `${remain}초`;
}

export default function BatchHistoryPage() {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);

  async function loadHistory() {
    const data = await fetchAdminBatchHistory({ limit: 30 });
    setHistory(Array.isArray(data) ? data : []);
  }

  useEffect(() => {
    loadHistory().finally(() => setLoading(false));
  }, []);

  async function handleRunBatch() {
    setRunning(true);
    try {
      await runAdminBatch();
      await loadHistory();
    } finally {
      setRunning(false);
    }
  }

  return (
    <div className="stack paw-admin-page">
      <section className="paw-list-header">
        <div>
          <p className="eyebrow">BATCH HISTORY</p>
          <h2>배치 히스토리</h2>
          <p className="paw-sub-copy">공공 API 수집 배치 실행 이력을 확인합니다.</p>
        </div>
        <button type="button" className="paw-action orange" onClick={handleRunBatch} disabled={running}>
          {running ? "배치 실행 중..." : "지금 배치 실행"}
        </button>
      </section>

      <section className="paw-campaign-admin-card">
        {loading ? (
          <div className="paw-message-card">배치 이력을 불러오는 중입니다.</div>
        ) : (
          <table className="paw-admin-table">
            <thead>
              <tr>
                <th>실행일시</th>
                <th>소요시간</th>
                <th>수집건수</th>
                <th>오류수</th>
                <th>상태</th>
              </tr>
            </thead>
            <tbody>
              {history.map((item) => (
                <tr key={item.id}>
                  <td>{item.startedAt ? new Date(item.startedAt).toLocaleString("ko-KR") : "-"}</td>
                  <td>{duration(item.startedAt, item.completedAt)}</td>
                  <td>{WON.format(Number(item.totalFetched ?? 0))}건</td>
                  <td>{WON.format(Number(item.totalErrors ?? 0))}건</td>
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
