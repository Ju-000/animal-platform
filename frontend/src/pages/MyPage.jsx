import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  fetchFavorites,
  fetchMyAdoptions,
  fetchMyDonationHistory,
  fetchMyProfile,
  updateMyNotificationSettings
} from "../lib/api";

const WON = new Intl.NumberFormat("ko-KR");

const STATUS_LABELS = {
  PENDING: "심사 대기",
  APPROVED: "승인",
  REJECTED: "반려"
};

const BENEFITS = [
  { key: "welcome", tier: "SPROUT", label: "환영 배지", description: "첫 후원일과 새싹 환영 배지를 마이페이지에서 확인해요." },
  { key: "monthly", tier: "SUPPORTER", label: "월간 보호소 소식", description: "매월 보호소 변화와 후원금 사용 현황을 받아요." },
  { key: "urgent", tier: "CHAMPION", label: "긴급 구조 우선 알림", description: "마감 임박 동물과 긴급 캠페인을 먼저 확인해요." },
  { key: "angel", tier: "ANGEL", label: "특별 감사 스토리", description: "천사 후원자 전용 감사 스토리를 열람해요." }
];

const TIER_ORDER = { SPROUT: 0, SUPPORTER: 1, CHAMPION: 2, ANGEL: 3 };

function tierProgress(profile) {
  if (!profile) return 0;
  if (!profile.nextTierMinimumAmount) return 100;
  const min = Number(profile.tierMinimumAmount ?? 0);
  const next = Number(profile.nextTierMinimumAmount ?? 0);
  const total = Number(profile.totalDonatedAmount ?? 0);
  if (next <= min) return 100;
  return Math.min(100, Math.max(0, Math.round(((total - min) / (next - min)) * 100)));
}

function formatDateTime(value) {
  if (!value) return "날짜 정보 없음";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("ko-KR");
}

export default function MyPage() {
  const [profile, setProfile] = useState(null);
  const [history, setHistory] = useState({ content: [], hasNext: false, page: 0, size: 10 });
  const [favorites, setFavorites] = useState([]);
  const [adoptions, setAdoptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    Promise.all([
      fetchMyProfile(),
      fetchMyDonationHistory({ page: 0, size: 10 }),
      fetchFavorites().catch(() => []),
      fetchMyAdoptions().catch(() => [])
    ])
      .then(([me, donationHistory, favoriteItems, adoptionItems]) => {
        if (!active) return;
        if (!me) {
          setError("로그인이 필요합니다.");
          return;
        }
        setProfile(me);
        setHistory(donationHistory ?? { content: [], hasNext: false, page: 0, size: 10 });
        setFavorites(Array.isArray(favoriteItems) ? favoriteItems : []);
        setAdoptions(Array.isArray(adoptionItems) ? adoptionItems : []);
      })
      .catch(() => {
        if (!active) return;
        setError("마이페이지 정보를 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!active) return;
        setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  async function loadMore() {
    setHistoryLoading(true);
    try {
      const nextPage = Number(history.page ?? 0) + 1;
      const next = await fetchMyDonationHistory({ page: nextPage, size: history.size ?? 10 });
      setHistory((current) => ({
        ...next,
        content: [...(current.content ?? []), ...(next.content ?? [])]
      }));
    } finally {
      setHistoryLoading(false);
    }
  }

  const progress = useMemo(() => tierProgress(profile), [profile]);
  const total = Number(profile?.totalDonatedAmount ?? 0);
  const nextAmount = Number(profile?.nextTierAmount ?? 0);
  const paidCount = history.content?.length ?? 0;
  const pendingAdoptions = adoptions.filter((item) => item.status === "PENDING").length;
  const currentTierOrder = TIER_ORDER[profile?.tier] ?? 0;
  const notifications = profile?.notifications ?? {
    urgentAnimalAlert: true,
    monthlyNewsletter: true,
    weeklyNewsletter: true
  };

  async function handleNotificationToggle(key) {
    const nextSettings = {
      urgentAnimalAlert: Boolean(notifications.urgentAnimalAlert),
      monthlyNewsletter: Boolean(notifications.monthlyNewsletter),
      weeklyNewsletter: Boolean(notifications.weeklyNewsletter),
      [key]: !notifications[key]
    };
    const saved = await updateMyNotificationSettings(nextSettings);
    setProfile((current) => ({
      ...current,
      notifications: saved
    }));
  }

  if (loading) {
    return <section className="paw-message-card">마이페이지를 불러오는 중입니다.</section>;
  }

  if (error) {
    return (
      <section className="paw-message-card">
        <h2>{error}</h2>
        <button type="button" className="paw-action orange" onClick={() => window.scrollTo({ top: 0 })}>
          상단 로그인으로 이동
        </button>
      </section>
    );
  }

  return (
    <div className="paw-mypage stack">
      <section className="paw-mypage-hero">
        <div className="paw-mypage-profile-card">
          <span className="paw-mypage-avatar" aria-hidden="true">{profile?.tierEmoji ?? "🐾"}</span>
          <div>
            <p className="eyebrow">MY PAGE</p>
            <h2>{profile?.name ?? "회원"}님, 다시 가족을 이어주고 있어요</h2>
            <p>{profile?.username} · {profile?.phone || "휴대전화 미등록"}</p>
            <p>{profile?.email || "이메일 미등록"} · {profile?.role === "ADMIN" ? "관리자" : "일반 회원"}</p>
          </div>
        </div>

        <div className="paw-mypage-quick-actions">
          <Link to="/favorites">관심동물 보기</Link>
          <Link to="/donations">후원하러 가기</Link>
          <Link to="/donations">등급 혜택</Link>
        </div>
      </section>

      <section className="paw-mypage-stat-grid">
        <article>
          <span>누적 후원</span>
          <strong>{WON.format(total)}원</strong>
          <p>{profile?.tierLabel} 등급</p>
        </article>
        <article>
          <span>후원 내역</span>
          <strong>{WON.format(paidCount)}건</strong>
          <p>확정 결제 기준</p>
        </article>
        <article>
          <span>관심 동물</span>
          <strong>{WON.format(favorites.length)}마리</strong>
          <p>다시 만나볼 친구들</p>
        </article>
        <article>
          <span>입양 신청</span>
          <strong>{WON.format(adoptions.length)}건</strong>
          <p>{pendingAdoptions > 0 ? `${pendingAdoptions}건 심사 대기` : "진행 대기 없음"}</p>
        </article>
      </section>

      <section className={`paw-tier-card paw-mypage-tier ${profile?.tier?.toLowerCase() ?? "sprout"}`}>
        <div className="paw-tier-emoji" aria-hidden="true">{profile?.tierEmoji}</div>
        <div className="paw-tier-copy">
          <p className="eyebrow">DONOR TIER</p>
          <h3>{profile?.tierLabel}</h3>
          <p>누적 후원금 <strong>{WON.format(total)}원</strong></p>
          <div className="paw-tier-progress" aria-label={`tier progress ${progress}%`}>
            <div style={{ width: `${progress}%` }} />
          </div>
          <p className="paw-tier-message">
            {nextAmount > 0
              ? `다음 등급까지 ${WON.format(nextAmount)}원 남았어요!`
              : "최고 등급이에요. 따뜻한 후원에 감사해요!"}
          </p>
        </div>
        <Link className="paw-tier-link" to="/donations">등급 혜택 보기</Link>
      </section>

      {total > 0 ? (
        <section className="paw-welcome-badge-card">
          <div className="paw-welcome-badge-icon">🌱</div>
          <div>
            <p className="eyebrow">WELCOME BADGE</p>
            <h3>새싹 후원자 환영 배지</h3>
            <p>첫 후원일 {formatDateTime(profile?.firstDonationAt)} · 첫 후원 감사합니다.</p>
          </div>
        </section>
      ) : null}

      <section className="paw-mypage-panel paw-benefit-panel">
        <div className="paw-section-head">
          <div>
            <p className="eyebrow">MY BENEFITS</p>
            <h3>나의 혜택</h3>
          </div>
        </div>
        <div className="paw-benefit-list">
          {BENEFITS.map((benefit) => {
            const unlocked = currentTierOrder >= TIER_ORDER[benefit.tier];
            return (
              <article key={benefit.key} className={unlocked ? "paw-benefit-item unlocked" : "paw-benefit-item locked"}>
                <span>{unlocked ? "✅" : "🔒"}</span>
                <div>
                  <strong>{benefit.label}</strong>
                  <p>{benefit.description}</p>
                </div>
              </article>
            );
          })}
        </div>
      </section>

      <section className="paw-mypage-panel paw-notification-panel">
        <div className="paw-section-head">
          <div>
            <p className="eyebrow">NOTIFICATIONS</p>
            <h3>알림 설정</h3>
          </div>
        </div>
        <div className="paw-notification-list">
          <label>
            <span>월간 보호소 소식</span>
            <input
              type="checkbox"
              checked={Boolean(notifications.monthlyNewsletter)}
              onChange={() => handleNotificationToggle("monthlyNewsletter")}
              disabled={currentTierOrder < TIER_ORDER.SUPPORTER}
            />
          </label>
          <label>
            <span>긴급 구조 우선 알림</span>
            <input
              type="checkbox"
              checked={Boolean(notifications.urgentAnimalAlert)}
              onChange={() => handleNotificationToggle("urgentAnimalAlert")}
              disabled={currentTierOrder < TIER_ORDER.CHAMPION}
            />
          </label>
          <label>
            <span>천사 후원자 주간 소식</span>
            <input
              type="checkbox"
              checked={Boolean(notifications.weeklyNewsletter)}
              onChange={() => handleNotificationToggle("weeklyNewsletter")}
              disabled={currentTierOrder < TIER_ORDER.ANGEL}
            />
          </label>
        </div>
      </section>

      <section className="paw-mypage-grid">
        <article className="paw-mypage-panel">
          <div className="paw-section-head">
            <div>
              <p className="eyebrow">ADOPTION</p>
              <h3>내 입양 신청</h3>
            </div>
            <Link className="inline-link" to="/animals">동물 더 보기</Link>
          </div>
          {adoptions.length === 0 ? (
            <div className="paw-empty-state">아직 입양 신청 내역이 없습니다.</div>
          ) : (
            <div className="paw-mypage-list">
              {adoptions.slice(0, 4).map((item) => (
                <div key={item.id} className="paw-mypage-list-item">
                  <strong>{item.animalNo}</strong>
                  <span>{STATUS_LABELS[item.status] ?? item.status}</span>
                  <small>{formatDateTime(item.appliedAt)}</small>
                </div>
              ))}
            </div>
          )}
        </article>

        <article className="paw-mypage-panel">
          <div className="paw-section-head">
            <div>
              <p className="eyebrow">FAVORITES</p>
              <h3>관심 동물</h3>
            </div>
            <Link className="inline-link" to="/favorites">전체 보기</Link>
          </div>
          {favorites.length === 0 ? (
            <div className="paw-empty-state">아직 저장한 관심 동물이 없습니다.</div>
          ) : (
            <div className="paw-mypage-list">
              {favorites.slice(0, 4).map((item, index) => (
                <div key={`${item.animalNo ?? item.id}-${index}`} className="paw-mypage-list-item">
                  <strong>{item.animalNo ?? item.noticeNumber ?? "동물번호 미확인"}</strong>
                  <span>{item.savedAt ? formatDateTime(item.savedAt) : "저장됨"}</span>
                </div>
              ))}
            </div>
          )}
        </article>
      </section>

      <section className="paw-mypage-panel">
        <div className="paw-section-head">
          <div>
            <p className="eyebrow">DONATION TIMELINE</p>
            <h3>후원 히스토리</h3>
          </div>
        </div>
        {(history.content ?? []).length === 0 ? (
          <div className="paw-empty-state">아직 확정된 후원 내역이 없습니다.</div>
        ) : (
          <div className="paw-donation-timeline">
            {history.content.map((item, index) => (
              <article key={`${item.paidAt}-${item.amount}-${index}`} className="paw-donation-timeline-item">
                <span className="paw-donation-dot" />
                <div>
                  <strong>{item.shelterName || "보호소 후원"}</strong>
                  <p>{WON.format(Number(item.amount ?? 0))}원 · {item.type}</p>
                  {item.animalNo ? <small>동물번호 {item.animalNo}</small> : null}
                  <small>{formatDateTime(item.paidAt)}</small>
                </div>
              </article>
            ))}
          </div>
        )}
        {history.hasNext ? (
          <button type="button" className="secondary-button" onClick={loadMore} disabled={historyLoading}>
            {historyLoading ? "불러오는 중" : "더 보기"}
          </button>
        ) : null}
      </section>
    </div>
  );
}
