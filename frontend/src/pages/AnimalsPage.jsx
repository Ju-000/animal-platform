import { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { fetchAnimalFilters, fetchAnimals } from "../lib/api";
import { createAnimalSlug } from "../lib/slug";

const PAGE_SIZE = 20;

const statusLabelMap = {
  ADOPTABLE: "보호중",
  SUPPORT_NEEDED: "후원 필요",
  IN_COUNSELING: "상담 진행 중",
  PROTECTING: "보호중"
};

const defaultSearchForm = {
  useRecentRange: true,
  startDate: "2026-01-16",
  endDate: "2026-04-16",
  region: "",
  species: "",
  breed: "",
  status: "",
  sex: "",
  neutered: "",
  protectingOnly: false
};

function AnimalCardSkeletonGrid() {
  return (
    <section className="paw-directory-card-grid paw-skeleton-grid" aria-label="동물 목록 로딩 중">
      {Array.from({ length: 10 }).map((_, index) => (
        <article key={index} className="paw-directory-grid-card paw-card-skeleton" aria-hidden="true">
          <div className="paw-directory-grid-photo paw-skeleton-photo" />
          <div className="paw-directory-grid-copy">
            <span className="paw-skeleton-line" />
            <span className="paw-skeleton-line short" />
          </div>
        </article>
      ))}
    </section>
  );
}
function safeText(value, fallback = "정보 없음") {
  if (!value) return fallback;
  if (typeof value === "string" && value.includes("?")) return fallback;
  return value;
}

function normalizeSex(value) {
  const text = safeText(value, "");
  if (!text) return "정보없음";
  if (text.includes("수")) return "수컷";
  if (text.includes("암")) return "암컷";
  return "정보없음";
}

function normalizeNeutered(value) {
  const text = safeText(value, "");
  if (!text) return "전체";
  if (text.includes("예") || text.toUpperCase().includes("Y")) return "예";
  if (text.includes("아니오") || text.toUpperCase().includes("N")) return "아니오";
  return "미상";
}

function toMetroRegion(value) {
  const text = safeText(value, "");
  const metroRegions = [
    "서울특별시",
    "부산광역시",
    "대구광역시",
    "인천광역시",
    "광주광역시",
    "대전광역시",
    "울산광역시",
    "세종특별자치시",
    "경기도",
    "강원특별자치도",
    "충청북도",
    "충청남도",
    "전북특별자치도",
    "전라남도",
    "경상북도",
    "경상남도",
    "제주특별자치도"
  ];

  return metroRegions.find((region) => text.startsWith(region)) ?? text;
}

export default function AnimalsPage() {
  const [searchParams] = useSearchParams();
  const initialRegion = searchParams.get("region") ?? "";
  const [animals, setAnimals] = useState([]);
  const [filterOptions, setFilterOptions] = useState({ regions: [], species: [], statuses: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchForm, setSearchForm] = useState({ ...defaultSearchForm, region: initialRegion });
  const [appliedFilters, setAppliedFilters] = useState({ ...defaultSearchForm, region: initialRegion });

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");

    fetchAnimals({
      page: currentPage,
      size: PAGE_SIZE,
      region: appliedFilters.region || undefined,
      species: appliedFilters.species || undefined,
      breed: appliedFilters.breed || undefined,
      status: appliedFilters.status || undefined,
      sex: appliedFilters.sex || undefined,
      neutered: appliedFilters.neutered || undefined,
      protectingOnly: appliedFilters.protectingOnly,
      startDate: appliedFilters.useRecentRange || appliedFilters.startDate ? appliedFilters.startDate : undefined,
      endDate: appliedFilters.useRecentRange || appliedFilters.endDate ? appliedFilters.endDate : undefined
    })
      .then((data) => {
        if (!active) return;
        setAnimals(data.content ?? []);
        setTotalPages(data.totalPages ?? 1);
      })
      .catch(() => {
        if (!active) return;
        setError("유기동물 정보를 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!active) return;
        setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [currentPage, appliedFilters]);

  useEffect(() => {
    let active = true;

    fetchAnimalFilters()
      .then((data) => {
        if (!active) return;
        setFilterOptions({
          regions: data.regions ?? [],
          species: data.species ?? [],
          statuses: data.statuses ?? []
        });
      })
      .catch(() => {
        if (!active) return;
      });

    return () => {
      active = false;
    };
  }, []);

  const regionOptions = useMemo(() => filterOptions.regions, [filterOptions.regions]);
  const speciesOptions = useMemo(() => filterOptions.species, [filterOptions.species]);
  const breedOptions = useMemo(
    () => [...new Set(animals.map((animal) => animal.name).filter(Boolean))].sort(),
    [animals]
  );
  const statusOptions = useMemo(() => filterOptions.statuses, [filterOptions.statuses]);

  const visiblePages = useMemo(() => {
    const start = Math.max(1, currentPage - 2);
    const end = Math.min(totalPages, start + 4);
    const adjustedStart = Math.max(1, end - 4);
    return Array.from({ length: end - adjustedStart + 1 }, (_, index) => adjustedStart + index);
  }, [currentPage, totalPages]);

  function movePage(page) {
    if (page < 1 || page > totalPages || page === currentPage) return;
    setCurrentPage(page);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function openSearchModal() {
    setSearchForm(appliedFilters);
    setSearchOpen(true);
  }

  function closeSearchModal() {
    setSearchOpen(false);
  }

  function resetFilters() {
    setSearchForm(defaultSearchForm);
    setAppliedFilters(defaultSearchForm);
    setCurrentPage(1);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function handleSearchFieldChange(event) {
    const { name, value, type, checked } = event.target;
    setSearchForm((current) => ({
      ...current,
      [name]: type === "checkbox" ? checked : value
    }));
  }

  function submitSearch(event) {
    event.preventDefault();
    setAppliedFilters(searchForm);
    setSearchOpen(false);
    setCurrentPage(1);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  return (
    <div className="paw-animal-directory stack">
      <section className="paw-directory-head">
        <div className="paw-directory-tabs" aria-label="동물 탐색 탭">
          <button type="button" className="paw-directory-tab active">
            보호동물
          </button>
          <Link className="paw-directory-tab" to="/shelters">
            보호소 찾기
          </Link>
          <button type="button" className="paw-directory-tab muted">
            추천 입양 동물
          </button>
        </div>

        <div className="paw-directory-filters paw-directory-filters-summary">
          <button type="button" className="paw-filter-all-button" onClick={resetFilters}>
            전체보기
          </button>
          <button type="button" className="paw-filter-icon-button" onClick={openSearchModal} aria-label="검색조건 설정">
            ⌕
          </button>
        </div>
      </section>

      {loading ? <AnimalCardSkeletonGrid /> : null}
      {error ? <section className="paw-message-card">{error}</section> : null}

      {!loading && !error ? (
        <section className="paw-directory-card-grid" aria-label="보호동물 목록">
          {animals.map((animal) => (
            <Link
              key={animal.id}
              to={`/shelter/animal/detail/${createAnimalSlug(animal)}`}
              className="paw-directory-grid-card"
            >
              <div className="paw-directory-grid-photo">
                <span
                  className={`paw-directory-status ${
                    animal.serviceStatus === "ADOPTABLE"
                      ? "green"
                      : animal.serviceStatus === "IN_COUNSELING"
                        ? "warm"
                        : "dark"
                  }`}
                >
                  {statusLabelMap[animal.serviceStatus] ?? "상태 확인중"}
                </span>
                {animal.imageUrl ? <img src={animal.imageUrl} alt={animal.name} /> : <div>NO IMAGE</div>}
              </div>
              <div className="paw-directory-grid-copy">
                <strong>{safeText(animal.name, "동물")}</strong>
                <p className="paw-directory-location">📍 {safeText(animal.region, "지역 정보 없음")}</p>
              </div>
            </Link>
          ))}
        </section>
      ) : null}

      {!loading && !error && totalPages > 1 ? (
        <nav className="pagination paw-pagination" aria-label="페이지 이동">
          <button
            type="button"
            className="page-button"
            disabled={currentPage === 1}
            onClick={() => movePage(currentPage - 1)}
          >
            이전
          </button>
          {visiblePages.map((page) => (
            <button
              key={page}
              type="button"
              className={`page-button ${page === currentPage ? "active" : ""}`}
              onClick={() => movePage(page)}
            >
              {page}
            </button>
          ))}
          <button
            type="button"
            className="page-button"
            disabled={currentPage === totalPages}
            onClick={() => movePage(currentPage + 1)}
          >
            다음
          </button>
        </nav>
      ) : null}

      {searchOpen ? (
        <div className="search-modal-backdrop" onClick={closeSearchModal}>
          <div className="search-modal" onClick={(event) => event.stopPropagation()}>
            <div className="search-modal-head">
              <h2>검색조건 설정</h2>
              <button type="button" className="search-close" onClick={closeSearchModal} aria-label="닫기">
                ×
              </button>
            </div>

            <form className="search-modal-form" onSubmit={submitSearch}>
              <section className="search-modal-section">
                <div className="search-section-head">
                  <h3>기간 설정</h3>
                  <label className="search-checkbox-inline">
                    <input
                      type="checkbox"
                      name="useRecentRange"
                      checked={searchForm.useRecentRange}
                      onChange={handleSearchFieldChange}
                    />
                    최근 3개월 검색
                  </label>
                </div>
                <div className="search-date-row">
                  <input type="date" name="startDate" value={searchForm.startDate} onChange={handleSearchFieldChange} />
                  <span>~</span>
                  <input type="date" name="endDate" value={searchForm.endDate} onChange={handleSearchFieldChange} />
                </div>
              </section>

              <section className="search-modal-section">
                <h3>지역 설정</h3>
                <select name="region" value={searchForm.region} onChange={handleSearchFieldChange}>
                  <option value="">모든 지역</option>
                  {regionOptions.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              </section>

              <section className="search-modal-section">
                <h3>축종 설정</h3>
                <select name="species" value={searchForm.species} onChange={handleSearchFieldChange}>
                  <option value="">모든 동물</option>
                  {speciesOptions.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              </section>

              <section className="search-modal-section">
                <h3>상태</h3>
                <select name="status" value={searchForm.status} onChange={handleSearchFieldChange}>
                  <option value="">전체</option>
                  {statusOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </section>

              <section className="search-modal-section">
                <h3>성별</h3>
                <select name="sex" value={searchForm.sex} onChange={handleSearchFieldChange}>
                  <option value="">전체</option>
                  <option value="수컷">수컷</option>
                  <option value="암컷">암컷</option>
                  <option value="정보없음">정보없음</option>
                </select>
              </section>

              <section className="search-modal-section">
                <h3>중성화</h3>
                <select name="neutered" value={searchForm.neutered} onChange={handleSearchFieldChange}>
                  <option value="">전체</option>
                  <option value="예">예</option>
                  <option value="아니오">아니오</option>
                  <option value="미상">미상</option>
                </select>
              </section>

              <label className="search-checkbox-row">
                <input
                  type="checkbox"
                  name="protectingOnly"
                  checked={searchForm.protectingOnly}
                  onChange={handleSearchFieldChange}
                />
                보호중인 동물만 검색
              </label>

              <button type="submit" className="search-submit-button">
                검색하기
              </button>
            </form>
          </div>
        </div>
      ) : null}
    </div>
  );
}
