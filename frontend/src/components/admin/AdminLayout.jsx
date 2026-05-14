import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { logoutSession } from "../../lib/api";

const menuItems = [
  { to: "/admin", label: "대시보드", icon: "🏠", end: true },
  { to: "/admin/adoptions", label: "입양 신청 관리", icon: "📋" },
  { to: "/admin/donations", label: "후원/결제 관리", icon: "💰" },
  { to: "/admin/campaigns", label: "캠페인 관리", icon: "📣" },
  { to: "/admin/users", label: "회원 관리", icon: "👥" },
  { to: "/admin/batch", label: "배치 히스토리", icon: "🔄" }
];

export default function AdminLayout() {
  const navigate = useNavigate();

  async function handleLogout() {
    try {
      await logoutSession();
    } finally {
      navigate("/");
    }
  }

  return (
    <div className="paw-admin-console">
      <aside className="paw-admin-sidebar">
        <div className="paw-admin-sidebar-brand">
          <strong>다시, 가족</strong>
          <span>Admin Console</span>
        </div>

        <nav className="paw-admin-sidebar-nav">
          {menuItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => (isActive ? "active" : "")}
            >
              <span aria-hidden="true">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <button type="button" className="paw-admin-sidebar-logout" onClick={handleLogout}>
          로그아웃
        </button>
      </aside>

      <section className="paw-admin-console-main">
        <Outlet />
      </section>
    </div>
  );
}
