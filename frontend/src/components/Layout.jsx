import { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import { fetchMyProfile, logoutSession } from "../lib/api";
import AuthModal from "./AuthModal";
import ChatWidget from "./ChatWidget";
import ErrorBoundary from "./common/ErrorBoundary";
import ChatbotFallback from "./common/ChatbotFallback";

const navItems = [
  { to: "/", label: "홈" },
  { to: "/animals", label: "유기동물" },
  { to: "/shelters", label: "보호소" },
  { to: "/donations", label: "후원" },
  { to: "/stories", label: "입양 후기" },
  { to: "/story", label: "스토리" }
];

const navLinkStyle = ({ isActive }) => ({
  color: isActive ? "#F97316" : "#6b5a4e",
  fontWeight: isActive ? "bold" : "500",
  padding: "5px 14px",
  borderRadius: "20px",
  border: `1.5px solid ${isActive ? "#F97316" : "#E8D5C0"}`,
  background: isActive ? "#FFF3E0" : "white",
  textDecoration: "none",
  transition: "all 0.2s ease"
});

export default function Layout() {
  const location = useLocation();
  const [me, setMe] = useState(null);
  const [authOpen, setAuthOpen] = useState(false);
  const showChatWidget = location.pathname === "/"
    || location.pathname === "/matching"
    || location.pathname.startsWith("/animals/")
    || location.pathname.startsWith("/shelter/animal/detail/");
  const isAdmin = me?.role === "ADMIN";

  useEffect(() => {
    let active = true;

    fetchMyProfile()
      .then((profile) => {
        if (!active) return;
        setMe(profile);
      })
      .catch(() => {
        if (!active) return;
        setMe(null);
      });

    return () => {
      active = false;
    };
  }, []);

  async function handleLogout() {
    try {
      await logoutSession();
    } catch {
      // ignore logout failure and clear UI state
    }
    setMe(null);
  }

  return (
    <div className="app-shell paw-shell">
      <header className="paw-header">
        <div className="paw-brand-lockup">
          <NavLink to="/" className="paw-brand">다시, 가족</NavLink>
          <p className="paw-brand-slogan">버려진 인연을 다시 이어줍니다</p>
        </div>
        <nav className="paw-nav">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === "/"}
              style={navLinkStyle}
              className={({ isActive }) => (isActive ? "nav-link paw-nav-link active" : "nav-link paw-nav-link")}
            >
              {item.label}
            </NavLink>
          ))}
          {isAdmin ? (
            <NavLink to="/admin" className={({ isActive }) => (isActive ? "paw-nav-link paw-admin-nav-link active" : "paw-nav-link paw-admin-nav-link")}>
              관리자 페이지
            </NavLink>
          ) : null}
          {me ? (
            <>
              <NavLink to="/me" className={({ isActive }) => (isActive ? "paw-nav-link paw-profile-nav-link active" : "paw-nav-link paw-profile-nav-link")}>
                {me.tierEmoji ? (
                  <span className="paw-nav-user-badge" aria-label={me.tierLabel}>{me.tierEmoji}</span>
                ) : null}
                {me.name || "회원"}님
              </NavLink>
              <button type="button" className="paw-nav-link paw-nav-button" onClick={handleLogout}>
                로그아웃
              </button>
            </>
          ) : (
            <button type="button" className="paw-nav-link paw-nav-button" onClick={() => setAuthOpen(true)}>
              로그인
            </button>
          )}
        </nav>
      </header>
      <main className="page paw-page">
        <Outlet />
      </main>
      {showChatWidget ? (
        <ErrorBoundary fallback={<ChatbotFallback />}>
          <ChatWidget />
        </ErrorBoundary>
      ) : null}
      <AuthModal open={authOpen} onClose={() => setAuthOpen(false)} onSuccess={(profile) => setMe(profile)} />
    </div>
  );
}
