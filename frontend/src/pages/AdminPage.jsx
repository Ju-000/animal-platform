import React, { Suspense, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { fetchAdminDashboard, fetchAdminMonitor, runAdminBatch } from "../lib/api";

const AdminCharts = React.lazy(() => import("../components/admin/AdminCharts"));

const WON = new Intl.NumberFormat("ko-KR");

function formatDateTime(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("ko-KR");
}

function timeAgo(value) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const seconds = Math.max(0, Math.floor((Date.now() - date.getTime()) / 1000));
  if (seconds < 60) return `${seconds}초 전`;
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  return `${Math.floor(hours / 24)}일 전`;
}

function statusTone(status) {
  if (status === "HEALTHY") return "green";
  if (status === "DEGRADED") return "yellow";
  return "red";
}

function statusLabel(status) {
  if (status === "HEALTHY") return "정상";
  if (status === "DEGRADED") return "지연";
  if (status === "DOWN") return "장애";
  return "미확인";
}

export default function AdminPage() {
  const [dashboard, setDashboard] = useState(null);
  const [monitor, setMonitor] = useState(null);
  const [loading, setLoading] = useState(true);
  const [batchRunning, setBatchRunning] = useState(false);
  const [error, setError] = useState("");

  async function loadAdminData({ initial = false } = {}) {
    try {
      const [dashboardData, monitorData] = await Promise.all([fetchAdminDashboard(), fetchAdminMonitor()]);
      setDashboard(dashboardData);
      setMonitor(monitorData);
      setError("");
    } catch {
      setDashboard(null);
      setMonitor(null);
      setError("관리자 수집 모니터링 정보를 불러오지 못했습니다.");
    } finally {
      if (initial) setLoading(false);
    }
  }

  useEffect(() => {
    loadAdminData({ initial: true });
    const timer = window.setInterval(() => loadAdminData(), 30000);
    return () => window.clearInterval(timer);
  }, []);

  async function handleRunBatch() {
    setBatchRunning(true);
    try {
      await runAdminBatch();
      await loadAdminData();
    } finally {
      setBatchRunning(false);
    }
  }

  const chartData = useMemo(() => monitor?.publicApi?.responseTimes ?? [], [monitor]);

  if (loading) {
    return <section className="paw-message-card">관리자 수집 모니터링을 불러오는 중입니다.</section>;
  }

  if (error || !dashboard || !monitor) {
    return <section className="paw-message-card">{error || "관리자 정보를 찾을 수 없습니다."}</section>;
  }

  const publicApi = monitor.publicApi ?? {};
  const lastBatch = monitor.lastBatch ?? {};
  const systemStats = monitor.systemStats ?? {};
  const tone = statusTone(publicApi.status);

  return (
    <div className="stack paw-admin-page">
      <section className="paw-list-header">
        <div>
          <p className="eyebrow">ADMIN MONITOR</p>
          <h2>관리자 수집 모니터링</h2>
          <p className="paw-sub-copy">공공데이터 연결 상태와 최근 수집 스냅샷 현황을 한눈에 확인합니다.</p>
        </div>
        <Link to="/admin/campaigns" className="paw-action orange">캠페인 관리</Link>
      </section>

      <section className="paw-ops-panel">
        <div className="paw-section-head">
          <div>
            <p className="eyebrow">REAL-TIME OPS</p>
            <h3>시스템 현황</h3>
          </div>
          <button type="button" className="paw-action dark" onClick={handleRunBatch} disabled={batchRunning}>
            {batchRunning ? "배치 실행 중..." : "지금 배치 실행"}
          </button>
        </div>

        <div className="paw-ops-card-grid">
          <article className="paw-ops-card">
            <span className={`paw-status-dot ${tone}`} />
            <h4>공공 API 상태</h4>
            <strong>{statusLabel(publicApi.status)}</strong>
            <p>평균 {publicApi.avgResponseMs ?? 0}ms · 성공률 {publicApi.successRate ?? 0}%</p>
          </article>
          <article className="paw-ops-card">
            <span className="paw-status-dot green" />
            <h4>마지막 배치 수집</h4>
            <strong>{timeAgo(lastBatch.completedAt)}</strong>
            <p>{WON.format(Number(lastBatch.recordsCollected ?? 0))}건 · {lastBatch.duration}</p>
          </article>
          <article className="paw-ops-card">
            <span className="paw-status-dot yellow" />
            <h4>오늘 후원 합계</h4>
            <strong>{WON.format(Number(systemStats.todayDonationAmount ?? 0))}원</strong>
            <p>확정 결제 기준</p>
          </article>
          <article className="paw-ops-card">
            <span className="paw-status-dot red" />
            <h4>처리 대기 입양 신청</h4>
            <strong>{WON.format(Number(systemStats.pendingAdoptions ?? 0))}건</strong>
            <p><Link to="/admin#adoptions">입양 관리로 이동</Link></p>
          </article>
        </div>

        <Suspense fallback={<div className="chart-skeleton">{"차트 불러오는 중..."}</div>}>
          <AdminCharts data={chartData} />
        </Suspense>
      </section>

      <section
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(2, 1fr)",
          gap: "16px",
          marginTop: "24px"
        }}
      >
        <article className="paw-admin-connection-card">
          <span className="paw-admin-connection-icon" aria-hidden="true">🔗</span>
          <h3>연결 상태</h3>
          <strong style={{ color: "#16a34a" }}>{dashboard.connectionStatus}</strong>
          <p>{dashboard.connectionMessage || "공공데이터 인증키가 설정되어 있습니다"}</p>
        </article>
        <article className="paw-admin-connection-card">
          <span className="paw-admin-connection-icon" aria-hidden="true">📊</span>
          <h3>수집 스냅샷</h3>
          <strong style={{ color: "#f97316" }}>{WON.format(Number(dashboard.snapshotCount ?? 0))}건</strong>
          <p>{dashboard.snapshotPages}페이지 x {dashboard.pageSize}건 기준</p>
        </article>
        <article className="paw-admin-connection-card">
          <span className="paw-admin-connection-icon" aria-hidden="true">🏠</span>
          <h3>연결된 보호소</h3>
          <strong style={{ color: "#2563eb" }}>{WON.format(Number(dashboard.shelterCount ?? 0))}곳</strong>
          <p>보호소 목록 API 기준</p>
        </article>
        <article className="paw-admin-connection-card">
          <span className="paw-admin-connection-icon" aria-hidden="true">🕐</span>
          <h3>마지막 확인</h3>
          <strong style={{ color: "#4b5563" }}>{formatDateTime(dashboard.checkedAt)}</strong>
          <p>{dashboard.provider || "data-go-kr"}</p>
        </article>
      </section>

      <section className="paw-section">
        <div className="paw-section-head">
          <h3>실시간 알림</h3>
        </div>
        <div className="paw-admin-alert-grid">
          {dashboard.alerts.map((alert) => (
            <article key={alert.title} className="paw-admin-alert-card">
              <strong>{alert.title}</strong>
              <b>{alert.value}</b>
              <p>{alert.description}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="paw-section">
        <div className="paw-section-head">
          <h3>상위 수집 지역</h3>
        </div>
        <div className="paw-horizontal-bars">
          {dashboard.topRegions.map((region, index) => {
            const max = Number(dashboard.topRegions[0]?.value ?? 1);
            const ratio = Math.max(8, (Number(region.value ?? 0) / max) * 100);
            return (
              <div key={region.label} className="paw-bar-row">
                <span>{region.label}</span>
                <div className="paw-bar-track">
                  <div className={`paw-bar-fill tone-${(index % 6) + 1}`} style={{ width: `${ratio}%` }} />
                </div>
                <strong>{region.value}</strong>
              </div>
            );
          })}
        </div>
      </section>
    </div>
  );
}
