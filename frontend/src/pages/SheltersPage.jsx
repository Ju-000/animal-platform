import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { fetchShelters } from "../lib/api";

const T = {
  seoul: "서울특별시",
  incheon: "인천광역시",
  gyeonggi: "경기도",
  gangwon: "강원특별자치도",
  sejong: "세종특별자치시",
  daejeon: "대전광역시",
  chungbuk: "충청북도",
  chungnam: "충청남도",
  daegu: "대구광역시",
  gyeongbuk: "경상북도",
  busan: "부산광역시",
  ulsan: "울산광역시",
  gyeongnam: "경상남도",
  gwangju: "광주광역시",
  jeonbuk: "전북특별자치도",
  jeonnam: "전라남도",
  jeju: "제주특별자치도"
};

const REGION_LAYOUT = [
  { region: T.seoul, shortLabel: "서울" },
  { region: T.incheon, shortLabel: "인천" },
  { region: T.gyeonggi, shortLabel: "경기" },
  { region: T.gangwon, shortLabel: "강원" },
  { region: T.sejong, shortLabel: "세종" },
  { region: T.daejeon, shortLabel: "대전" },
  { region: T.chungbuk, shortLabel: "충북" },
  { region: T.chungnam, shortLabel: "충남" },
  { region: T.daegu, shortLabel: "대구" },
  { region: T.gyeongbuk, shortLabel: "경북" },
  { region: T.busan, shortLabel: "부산" },
  { region: T.ulsan, shortLabel: "울산" },
  { region: T.gyeongnam, shortLabel: "경남" },
  { region: T.gwangju, shortLabel: "광주" },
  { region: T.jeonbuk, shortLabel: "전북" },
  { region: T.jeonnam, shortLabel: "전남" },
  { region: T.jeju, shortLabel: "제주" }
];

const capacityFilters = [
  { value: "ALL", label: "전체" },
  { value: "CRITICAL", label: "위기 보호소" },
  { value: "NORMAL", label: "여유 있음" }
];

function extractMetroRegion(value) {
  const source = String(value ?? "");
  return REGION_LAYOUT.find((item) => source.includes(item.region))?.region ?? "기타";
}

function ShelterCardSkeletonGrid() {
  return (
    <section className="paw-shelter-grid detailed paw-skeleton-grid" aria-label="보호소 목록 로딩 중">
      {Array.from({ length: 6 }).map((_, index) => (
        <article key={index} className="paw-shelter-card detailed paw-card-skeleton" aria-hidden="true">
          <span className="paw-skeleton-pill" />
          <span className="paw-skeleton-line wide" />
          <span className="paw-skeleton-line short" />
          <span className="paw-skeleton-line" />
          <span className="paw-skeleton-line" />
          <span className="paw-skeleton-bar" />
        </article>
      ))}
    </section>
  );
}

function buildMapUrl(shelter) {
  const query = [shelter?.address, shelter?.name].filter(Boolean).join(" ");
  return `https://maps.google.com/maps?q=${encodeURIComponent(query)}&output=embed`;
}

function buildExternalMapUrl(shelter) {
  const query = [shelter?.address, shelter?.name].filter(Boolean).join(" ");
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}`;
}

function shelterDedupKey(shelter) {
  return [shelter?.name, shelter?.address].map((value) => String(value ?? "").trim()).join("|");
}

export default function SheltersPage() {
  const [shelters, setShelters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedRegion, setSelectedRegion] = useState(T.seoul);
  const [capacityFilter, setCapacityFilter] = useState("ALL");
  const [selectedShelterId, setSelectedShelterId] = useState(null);
  const [shelterQuery, setShelterQuery] = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");

    fetchShelters({ status: capacityFilter === "CRITICAL" ? "CRITICAL" : undefined })
      .then((data) => {
        if (!active) return;
        setShelters(
          (data ?? []).map((shelter) => ({
            ...shelter,
            metroRegion: extractMetroRegion(shelter.address || shelter.region || shelter.name)
          }))
        );
      })
      .catch(() => {
        if (!active) return;
        setError("보호소 정보를 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!active) return;
        setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [capacityFilter]);

  const visibleShelters = useMemo(
    () => shelters.filter((shelter) => capacityFilter !== "NORMAL" || shelter.capacityStatus === "NORMAL"),
    [capacityFilter, shelters]
  );

  const regionSummary = useMemo(
    () =>
      REGION_LAYOUT.map((item) => {
        const regionShelters = visibleShelters.filter((shelter) => shelter.metroRegion === item.region);
        const protectedAnimalCount = regionShelters.reduce(
          (sum, shelter) => sum + Number(shelter.currentCount ?? shelter.protectedAnimalCount ?? 0),
          0
        );

        return { ...item, shelterCount: regionShelters.length, protectedAnimalCount };
      }),
    [visibleShelters]
  );

  const selectedRegionInfo = useMemo(
    () => regionSummary.find((item) => item.region === selectedRegion),
    [regionSummary, selectedRegion]
  );

  const filteredShelters = useMemo(
    () => visibleShelters.filter((shelter) => shelter.metroRegion === selectedRegion),
    [selectedRegion, visibleShelters]
  );

  const uniqueFilteredShelters = useMemo(() => {
    const byKey = new Map();
    filteredShelters.forEach((shelter) => {
      const key = shelterDedupKey(shelter);
      const current = byKey.get(key);
      if (!current) {
        byKey.set(key, shelter);
        return;
      }

      byKey.set(key, {
        ...current,
        currentCount: Number(current.currentCount ?? 0) + Number(shelter.currentCount ?? 0),
        protectedAnimalCount: Number(current.protectedAnimalCount ?? 0) + Number(shelter.protectedAnimalCount ?? 0)
      });
    });
    return Array.from(byKey.values());
  }, [filteredShelters]);

  const searchedShelters = useMemo(() => {
    const query = shelterQuery.trim().toLowerCase();
    if (!query) return uniqueFilteredShelters;
    return uniqueFilteredShelters.filter((shelter) =>
      [shelter.name, shelter.address, shelter.phone].some((value) => String(value ?? "").toLowerCase().includes(query))
    );
  }, [uniqueFilteredShelters, shelterQuery]);

  const selectedShelter = useMemo(
    () => searchedShelters.find((shelter) => String(shelter.id) === String(selectedShelterId)) ?? searchedShelters[0],
    [searchedShelters, selectedShelterId]
  );

  useEffect(() => {
    setSelectedShelterId(searchedShelters[0]?.id ?? null);
  }, [searchedShelters]);

  return (
    <div className="stack paw-shelter-page">
      <section className="paw-list-header">
        <div>
          <p className="eyebrow">MAP FINDER</p>
          <h2>지도 기반 보호소/동물 찾기</h2>
          <p className="paw-sub-copy">전국 17개 광역 행정구역에서 보호소와 대표 동물을 빠르게 찾아보세요.</p>
        </div>
      </section>

      {loading ? <ShelterCardSkeletonGrid /> : null}
      {error ? <section className="paw-message-card">{error}</section> : null}

      {!loading && !error ? (
        <>
          <section className="paw-shelter-map-shell">
            <article className="paw-region-map-board">
              <div className="paw-section-head">
                <h3>지역 선택</h3>
                <span>{selectedRegion}</span>
              </div>

              <div className="paw-region-map-grid advanced">
                {regionSummary.map((item) => (
                  <button
                    key={item.region}
                    type="button"
                    className={selectedRegion === item.region ? "paw-region-map-card active" : "paw-region-map-card"}
                    onClick={() => {
                      setSelectedRegion(item.region);
                      setSelectedShelterId(null);
                      setShelterQuery("");
                    }}
                  >
                    <strong>{item.shortLabel}</strong>
                    <span>{item.region}</span>
                    <small>보호소 {item.shelterCount}곳 · 보호 동물 {item.protectedAnimalCount}마리</small>
                  </button>
                ))}
              </div>
            </article>

            <article className="paw-region-hero-card">
              <div className="paw-section-head">
                <h3>{selectedRegion} 보호소</h3>
                <Link to="/shelters">전체 보기 &gt;</Link>
              </div>
              <div className="paw-region-hero-copy">
                <strong>{selectedRegionInfo?.shelterCount ?? 0}개 보호소와 연결되어 있어요.</strong>
                <p>보호소를 선택하면 위치와 연락처를 바로 확인할 수 있습니다.</p>
              </div>

              {uniqueFilteredShelters.length ? (
                <div className="paw-region-shelter-browser">
                  <div className="paw-region-shelter-pane">
                    <label className="paw-region-shelter-search">
                      <span>보호소 검색</span>
                      <input
                        value={shelterQuery}
                        onChange={(event) => setShelterQuery(event.target.value)}
                        placeholder="이름, 주소, 전화번호"
                      />
                    </label>
                    <div className="paw-region-shelter-count">
                      <strong>{searchedShelters.length}</strong>
                      <span>곳 표시 중</span>
                    </div>
                    <div className="paw-region-shelter-list" aria-label={`${selectedRegion} 보호소 목록`}>
                      {searchedShelters.map((shelter) => (
                        <button
                          key={shelter.id}
                          type="button"
                          className={`paw-region-shelter-row ${String(selectedShelter?.id) === String(shelter.id) ? "active" : ""}`}
                          onClick={() => setSelectedShelterId(shelter.id)}
                        >
                          <span>
                            <strong>{shelter.name}</strong>
                            <small>{shelter.address || shelter.metroRegion}</small>
                          </span>
                          <em>{shelter.currentCount ?? shelter.protectedAnimalCount ?? 0}마리</em>
                        </button>
                      ))}
                      {!searchedShelters.length ? <div className="paw-message-card compact">검색 결과가 없습니다.</div> : null}
                    </div>
                  </div>

                  {selectedShelter ? (
                    <div className="paw-region-shelter-map">
                      <div className="paw-region-shelter-map-head">
                        <div>
                          <strong>{selectedShelter.name}</strong>
                          <span>{selectedShelter.address || "주소 정보 없음"}</span>
                          {selectedShelter.phone ? <small>{selectedShelter.phone}</small> : null}
                        </div>
                        <div className="paw-region-shelter-map-actions">
                          <a href={buildExternalMapUrl(selectedShelter)} target="_blank" rel="noreferrer">지도 크게</a>
                          <Link to={`/shelters/${selectedShelter.id}`}>상세 보기</Link>
                        </div>
                      </div>
                      <iframe
                        title={`${selectedShelter.name} 지도`}
                        src={buildMapUrl(selectedShelter)}
                        loading="lazy"
                        referrerPolicy="no-referrer-when-downgrade"
                      />
                    </div>
                  ) : null}
                </div>
              ) : (
                <div className="paw-message-card">이 지역 보호소 정보를 찾지 못했습니다.</div>
              )}
            </article>
          </section>

          <section className="paw-section paw-shelter-filter-section">
            <div className="paw-section-head">
              <h3>보호소 보기 옵션</h3>
              <span>현재 {searchedShelters.length}곳</span>
            </div>
            <div className="paw-tag-row">
              {capacityFilters.map((item) => (
                <button key={item.value} type="button" className={`paw-tag ${capacityFilter === item.value ? "active" : ""}`} onClick={() => setCapacityFilter(item.value)}>
                  {item.label}
                </button>
              ))}
            </div>
          </section>
        </>
      ) : null}
    </div>
  );
}
