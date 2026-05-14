import { useEffect, useMemo, useState } from "react";
import KoreaHeatmap from "../components/KoreaHeatmap";
import ErrorBoundary from "../components/common/ErrorBoundary";
import { fetchRegionStats, fetchStatsSummary } from "../lib/api";

function createEmptyRange() {
  return {
    startDate: "",
    endDate: ""
  };
}

function buildConicStyle(items, colors) {
  const total = items.reduce((sum, item) => sum + Number(item.value ?? 0), 0);
  if (!total) {
    return { background: "conic-gradient(#d9e5ff 0deg 360deg)" };
  }

  let current = 0;
  const segments = items.map((item) => {
    const degree = (Number(item.value ?? 0) / total) * 360;
    const start = current;
    const end = current + degree;
    current = end;
    return `${colors[item.label] ?? "#8aa5ff"} ${start}deg ${end}deg`;
  });

  return { background: `conic-gradient(${segments.join(", ")})` };
}

function formatPercent(value) {
  return `${Number(value ?? 0).toFixed(1)}%`;
}

export default function StatsPage() {
  const [summary, setSummary] = useState(null);
  const [regions, setRegions] = useState([]);
  const [draftRange, setDraftRange] = useState(createEmptyRange());
  const [appliedRange, setAppliedRange] = useState(createEmptyRange());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");

    Promise.all([fetchStatsSummary(appliedRange), fetchRegionStats(appliedRange)])
      .then(([summaryData, regionData]) => {
        if (!active) return;
        setSummary(summaryData);
        setRegions(regionData ?? []);
      })
      .catch(() => {
        if (!active) return;
        setSummary(null);
        setRegions([]);
        setError("통계 정보를 불러오지 못했습니다. 백엔드 실행 상태와 공공데이터 연결을 확인해주세요.");
      })
      .finally(() => {
        if (!active) return;
        setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [appliedRange]);

  const statusDistribution = useMemo(
    () =>
      Object.entries(summary?.statusDistribution ?? {}).map(([label, value]) => ({
        label,
        value: Number(value ?? 0)
      })),
    [summary]
  );

  const sexDistribution = useMemo(
    () =>
      Object.entries(summary?.sexDistribution ?? {}).map(([label, value]) => ({
        label,
        value: Number(value ?? 0)
      })),
    [summary]
  );

  const topRegions = useMemo(
    () => [...regions].sort((a, b) => Number(b.value ?? 0) - Number(a.value ?? 0)).slice(0, 8),
    [regions]
  );

  const regionRanking = useMemo(() => {
    const ranking = [...regions].sort((a, b) => Number(b.value ?? 0) - Number(a.value ?? 0)).slice(0, 12);
    const max = Number(ranking[0]?.value ?? 1);
    return ranking.map((item) => ({
      ...item,
      ratio: Math.max(8, (Number(item.value ?? 0) / max) * 100)
    }));
  }, [regions]);

  const totalAnimalsCount = Number(summary?.totalAnimalsCount ?? 0);
  const protectedAnimalsCount = Number(summary?.protectedAnimalsCount ?? 0);
  const adoptedAnimalsCount = Number(summary?.adoptedAnimalsCount ?? 0);
  const euthanasiaAnimalsCount = Number(summary?.euthanasiaAnimalsCount ?? 0);
  const adoptionRate = Number(summary?.adoptionRate ?? 0);
  const euthanasiaRate = Number(summary?.euthanasiaRate ?? 0);

  const statusChartStyle = buildConicStyle(statusDistribution, {
    보호중: "#5b7cff",
    입양완료: "#77d7d0",
    안락사: "#ff9f86",
    기타: "#c98ae3"
  });

  const sexChartStyle = buildConicStyle(sexDistribution, {
    수컷: "#5b7cff",
    암컷: "#78dfb6",
    미상: "#d8def1"
  });

  const outcomeChartStyle = buildConicStyle(
    [
      { label: "입양률", value: adoptionRate },
      { label: "안락사율", value: euthanasiaRate },
      { label: "기타", value: Math.max(0, 100 - adoptionRate - euthanasiaRate) }
    ],
    {
      입양률: "#8fc4ff",
      안락사율: "#ff8d7a",
      기타: "#1c327d"
    }
  );

  function handleSearch() {
    setAppliedRange({ ...draftRange });
  }

  return (
    <div className="stack paw-stats-page">
      <section className="paw-stats-header">
        <div>
          <p className="eyebrow">NATIONAL STATS</p>
          <h2>전국 유기동물 현황</h2>
        </div>
      </section>

      <section className="paw-stats-toolbar">
        <select value="전체 기간" readOnly>
          <option>전체 기간</option>
        </select>
        <input
          type="date"
          value={draftRange.startDate}
          onChange={(event) => setDraftRange((current) => ({ ...current, startDate: event.target.value }))}
        />
        <input
          type="date"
          value={draftRange.endDate}
          onChange={(event) => setDraftRange((current) => ({ ...current, endDate: event.target.value }))}
        />
        <button type="button" className="paw-stats-search-button" onClick={handleSearch}>
          검색
        </button>
      </section>

      {loading ? <section className="paw-message-card">통계 정보를 불러오는 중입니다.</section> : null}
      {error ? <section className="paw-message-card">{error}</section> : null}

      {!loading && !error ? (
        <>
          <section className="paw-map-hero-card">
            <div className="paw-map-hero-copy">
              <p className="eyebrow">REGIONAL HEATMAP</p>
              <h3>지역별 구조 건수 지도</h3>
              <p>색이 진할수록 구조 건수가 많은 지역입니다. 지도를 클릭하면 해당 지역 동물 목록으로 이동합니다.</p>
            </div>
            <div className="paw-map-desktop">
              <ErrorBoundary fallback={<div className="paw-map-fallback">지도를 불러올 수 없습니다</div>}><KoreaHeatmap regions={regions} /></ErrorBoundary>
            </div>
            <div className="paw-map-mobile-fallback">
              <div className="paw-ranking-list">
                {regionRanking.map((item) => (
                  <div key={item.label} className="paw-ranking-row">
                    <span>{item.label}</span>
                    <div className="paw-ranking-track blue">
                      <div className="paw-ranking-fill" style={{ width: `${item.ratio}%` }} />
                    </div>
                    <strong>{item.value}</strong>
                  </div>
                ))}
              </div>
            </div>
          </section>

          <section className="paw-stats-grid">
            <article className="paw-chart-card">
              <h3>상태별</h3>
              <div className="paw-donut-layout">
                <div className="paw-donut-chart" style={statusChartStyle}>
                  <div className="paw-donut-hole">
                    <strong>{totalAnimalsCount}</strong>
                    <span>전체</span>
                  </div>
                </div>
                <div className="paw-donut-legend">
                  {statusDistribution.map((item) => (
                    <div key={item.label}>
                      <span>{item.label}</span>
                      <strong>{item.value}건</strong>
                    </div>
                  ))}
                </div>
              </div>
            </article>

            <article className="paw-chart-card">
              <h3>상위 지역</h3>
              <div className="paw-horizontal-bars">
                {topRegions.map((item, index) => {
                  const max = Number(topRegions[0]?.value ?? 1);
                  const ratio = Math.max(6, (Number(item.value ?? 0) / max) * 100);
                  return (
                    <div key={item.label} className="paw-bar-row">
                      <span>{item.label}</span>
                      <div className="paw-bar-track">
                        <div className={`paw-bar-fill tone-${(index % 6) + 1}`} style={{ width: `${ratio}%` }} />
                      </div>
                      <strong>{item.value}</strong>
                    </div>
                  );
                })}
              </div>
            </article>

            <article className="paw-chart-card">
              <h3>성별</h3>
              <div className="paw-donut-layout compact">
                <div className="paw-donut-chart small" style={sexChartStyle}>
                  <div className="paw-donut-hole">
                    <strong>{protectedAnimalsCount}</strong>
                    <span>보호중</span>
                  </div>
                </div>
              </div>
            </article>

            <article className="paw-chart-card">
              <h3>입양/안락사 비율</h3>
              <div className="paw-donut-layout compact">
                <div className="paw-donut-chart small" style={outcomeChartStyle}>
                  <div className="paw-donut-hole">
                    <strong>{formatPercent(adoptionRate)}</strong>
                    <span>입양률</span>
                  </div>
                </div>
                <div className="paw-donut-legend">
                  <div>
                    <span>입양완료</span>
                    <strong>{adoptedAnimalsCount}건</strong>
                  </div>
                  <div>
                    <span>안락사</span>
                    <strong>{euthanasiaAnimalsCount}건</strong>
                  </div>
                </div>
              </div>
            </article>
          </section>

          <section className="paw-stats-header secondary">
            <div>
              <h2>지자체 유기동물 통계 순위</h2>
            </div>
          </section>

          <section className="paw-ranking-grid">
            <article className="paw-ranking-card">
              <h3>지역별 구조 건수</h3>
              <div className="paw-ranking-list">
                {regionRanking.map((item) => (
                  <div key={item.label} className="paw-ranking-row">
                    <span>{item.label}</span>
                    <div className="paw-ranking-track blue">
                      <div className="paw-ranking-fill" style={{ width: `${item.ratio}%` }} />
                    </div>
                    <strong>{item.value}</strong>
                  </div>
                ))}
              </div>
            </article>
          </section>
        </>
      ) : null}
    </div>
  );
}
