import { useEffect, useMemo, useState } from "react";
import {
  absoluteAssetUrl,
  fetchCampaigns,
  fetchDonationStats,
  fetchMyProfile,
  fetchShelters,
  preparePayment
} from "../lib/api";
import donationBanner from "../assets/donation_banner.png";

const AMOUNT_OPTIONS = [5000, 10000, 20000, 30000, 50000];

function formatAmount(value) {
  if (value == null || Number.isNaN(Number(value))) return "0원";
  return `${Number(value).toLocaleString("ko-KR")}원`;
}

function progressPercent(current, goal) {
  if (!goal) return 0;
  return Math.min(100, Math.round((Number(current ?? 0) / Number(goal)) * 100));
}

export default function DonationsPage() {
  const [activeTab, setActiveTab] = useState("shelter");
  const [shelters, setShelters] = useState([]);
  const [campaigns, setCampaigns] = useState([]);
  const [donationStats, setDonationStats] = useState(null);
  const [selectedShelterId, setSelectedShelterId] = useState("");
  const [selectedCampaign, setSelectedCampaign] = useState(null);
  const [selectedAmount, setSelectedAmount] = useState(10000);
  const [customAmount, setCustomAmount] = useState("");
  const [recurring, setRecurring] = useState(false);
  const [paymentMessage, setPaymentMessage] = useState("");
  const [donorProfile, setDonorProfile] = useState(null);

  useEffect(() => {
    fetchShelters().then((data) => setShelters(data ?? [])).catch(() => setShelters([]));
    fetchCampaigns().then((data) => setCampaigns(data ?? [])).catch(() => setCampaigns([]));
    fetchDonationStats().then((data) => setDonationStats(data)).catch(() => setDonationStats(null));
    fetchMyProfile().then((data) => setDonorProfile(data)).catch(() => setDonorProfile(null));
  }, []);

  const finalAmount = useMemo(() => {
    if (selectedAmount === "custom") {
      return Number(customAmount || 0);
    }
    return Number(selectedAmount || 0);
  }, [customAmount, selectedAmount]);

  const uniqueShelters = useMemo(() => {
    const seen = new Set();
    return shelters.filter((shelter) => {
      const key = `${shelter.name ?? ""}|${shelter.region ?? ""}`;
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    });
  }, [shelters]);

  const selectedShelter = uniqueShelters.find((shelter) => String(shelter.id) === String(selectedShelterId));

  const impactCards = [
    { icon: "🐶", label: "의료 지원 받은 아이", value: donationStats?.medicalCount ?? 0, suffix: "마리" },
    { icon: "🍚", label: "지원된 사료", value: donationStats?.foodKg ?? 0, suffix: "kg" },
    { icon: "🏠", label: "운영 지원 보호소", value: donationStats?.shelterCount ?? 0, suffix: "곳" },
    { icon: "👨‍👩‍👧", label: "새 가족을 만난 아이", value: donationStats?.adoptedCount ?? 0, suffix: "마리" }
  ];

  const handleCampaignDonate = (campaign) => {
    setSelectedCampaign(campaign);
    setActiveTab("shelter");
    setPaymentMessage(`${campaign.title} 캠페인 후원을 선택했어요. 보호소와 금액을 확인해주세요.`);
  };

  const handleDonate = async () => {
    if (!selectedShelterId) {
      setPaymentMessage("보호소를 먼저 선택해주세요.");
      return;
    }
    if (finalAmount < 1000) {
      setPaymentMessage("후원 금액은 1,000원 이상 입력해주세요.");
      return;
    }

    try {
      await preparePayment({
        targetType: "SHELTER",
        donationType: recurring ? "SUBSCRIPTION" : "ONE_TIME",
        targetId: Number(selectedShelterId),
        orderName: selectedCampaign?.title ?? `${selectedShelter?.name ?? "보호소"} 후원`,
        amount: finalAmount,
        buyerName: donorProfile?.name ?? "후원자",
        buyerEmail: donorProfile?.email ?? "guest@dasigajok.com",
        buyerTel: donorProfile?.phone ?? "010-0000-0000",
        subscriptionInterval: recurring ? "MONTHLY" : null
      });
      setPaymentMessage("결제 준비가 완료되었습니다. PortOne 결제창 연동 단계로 이동할 수 있어요.");
    } catch (error) {
      setPaymentMessage(error.message || "결제 준비 중 문제가 생겼어요. 잠시 후 다시 시도해주세요.");
    }
  };

  return (
    <div className="stack paw-donation-page">
      <section className="paw-donation-tier-banner" aria-label="후원 등급 안내">
        <img src={donationBanner} alt="후원 등급 안내 배너" />
      </section>

      <section className="paw-donation-feature-section">
        <div className="paw-donation-tabs" role="tablist" aria-label="후원 방식 선택">
          <button
            type="button"
            className={`paw-donation-tab ${activeTab === "shelter" ? "active" : ""}`}
            onClick={() => setActiveTab("shelter")}
          >
            보호소 후원
          </button>
          <button
            type="button"
            className={`paw-donation-tab ${activeTab === "campaign" ? "active" : ""}`}
            onClick={() => setActiveTab("campaign")}
          >
            캠페인 후원
          </button>
        </div>

        {activeTab === "shelter" ? (
          <div className="paw-donation-tab-panel paw-shelter-donation-panel">
            <section className="paw-donation-form-card">
              <p className="eyebrow">DONATION FORM</p>
              <h2>보호소에 따뜻한 마음을 전해주세요</h2>
              {selectedCampaign && (
                <div className="paw-selected-campaign-note">
                  선택한 캠페인: <strong>{selectedCampaign.title}</strong>
                </div>
              )}

              <label className="paw-donation-field">
                <span>보호소 선택</span>
                <select value={selectedShelterId} onChange={(event) => setSelectedShelterId(event.target.value)}>
                  <option value="">보호소를 선택해주세요</option>
                  {uniqueShelters.map((shelter) => (
                    <option key={shelter.id} value={shelter.id}>
                      {shelter.name} {shelter.region ? `- ${shelter.region}` : ""}
                    </option>
                  ))}
                </select>
              </label>

              <div className="paw-donation-field">
                <span>후원 금액 선택</span>
                <div className="paw-amount-grid">
                  {AMOUNT_OPTIONS.map((amount) => (
                    <button
                      key={amount}
                      type="button"
                      className={`paw-amount-button ${selectedAmount === amount ? "active" : ""}`}
                      onClick={() => setSelectedAmount(amount)}
                    >
                      {formatAmount(amount)}
                    </button>
                  ))}
                  <button
                    type="button"
                    className={`paw-amount-button ${selectedAmount === "custom" ? "active" : ""}`}
                    onClick={() => setSelectedAmount("custom")}
                  >
                    직접 입력
                  </button>
                </div>
                {selectedAmount === "custom" && (
                  <input
                    className="paw-custom-amount-input"
                    type="number"
                    min="1000"
                    value={customAmount}
                    onChange={(event) => setCustomAmount(event.target.value)}
                    placeholder="금액을 입력해주세요"
                  />
                )}
              </div>

              <label className="paw-recurring-check">
                <input type="checkbox" checked={recurring} onChange={(event) => setRecurring(event.target.checked)} />
                <span>정기후원 (매월 자동 후원)</span>
              </label>

              <button type="button" className="paw-donate-submit" onClick={handleDonate}>
                안전하게 후원하기 ♥
              </button>
              {paymentMessage && <p className="paw-payment-message">{paymentMessage}</p>}

              <div className="paw-safe-payment-list" aria-label="안전 결제 안내">
                <span>🔒 개인정보 보호</span>
                <span>🛡️ 결제 보안</span>
                <span>🧾 기부금 영수증</span>
              </div>
            </section>

            <section className="paw-donation-impact-card">
              <p className="eyebrow">DONATION IMPACT</p>
              <h2>후원으로 이루어진 변화</h2>
              <div className="paw-donation-impact-grid">
                {impactCards.map((card) => (
                  <article key={card.label} className="paw-impact-stat-card">
                    <div className="paw-impact-icon">{card.icon}</div>
                    <strong>{Number(card.value).toLocaleString("ko-KR")}{card.suffix}</strong>
                    <span>{card.label}</span>
                  </article>
                ))}
              </div>
              <p className="paw-thanks-message">오늘도 소중한 생명을 지켜주셔서 진심으로 감사드립니다 ♥</p>
            </section>
          </div>
        ) : (
          <div className="paw-donation-tab-panel">
            <div className="paw-campaign-donation-grid">
              {campaigns.map((campaign) => {
                const percent = progressPercent(campaign.currentAmount, campaign.goalAmount);
                return (
                  <article key={campaign.id} className="paw-campaign-donation-card">
                    <div className="paw-campaign-donation-image">
                      {campaign.imageUrl ? (
                        <img src={absoluteAssetUrl(campaign.imageUrl)} alt={campaign.title} />
                      ) : (
                        <div>🐾</div>
                      )}
                    </div>
                    <div className="paw-campaign-donation-body">
                      <span className="paw-campaign-badge">{campaign.category}</span>
                      <h3>{campaign.title}</h3>
                      <div className="paw-campaign-progress-track">
                        <div style={{ width: `${percent}%` }} />
                      </div>
                      <div className="paw-campaign-meta-row">
                        <strong>{percent}% 달성</strong>
                        <span>{Number(campaign.participants ?? 0).toLocaleString("ko-KR")}명 참여</span>
                      </div>
                      <button type="button" onClick={() => handleCampaignDonate(campaign)}>
                        후원하기
                      </button>
                    </div>
                  </article>
                );
              })}
              {campaigns.length === 0 && (
                <div className="paw-empty-card">진행 중인 캠페인을 준비하고 있어요.</div>
              )}
            </div>
          </div>
        )}
      </section>
    </div>
  );
}

