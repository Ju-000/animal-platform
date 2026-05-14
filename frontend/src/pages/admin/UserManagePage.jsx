import { useEffect, useState } from "react";
import { fetchAdminUsers, updateAdminUserRole } from "../../lib/api";

const WON = new Intl.NumberFormat("ko-KR");

export default function UserManagePage() {
  const [data, setData] = useState({ content: [] });
  const [loading, setLoading] = useState(true);

  async function loadUsers() {
    setLoading(true);
    try {
      const payload = await fetchAdminUsers({ page: 0, size: 50 });
      setData(payload ?? { content: [] });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadUsers();
  }, []);

  async function changeRole(id, role) {
    await updateAdminUserRole(id, role);
    await loadUsers();
  }

  return (
    <div className="stack paw-admin-page">
      <section className="paw-list-header">
        <div>
          <p className="eyebrow">USER ADMIN</p>
          <h2>회원 관리</h2>
          <p className="paw-sub-copy">회원 등급과 역할을 확인하고 관리자 권한을 조정합니다.</p>
        </div>
      </section>

      <section className="paw-campaign-admin-card">
        {loading ? (
          <div className="paw-message-card">회원 목록을 불러오는 중입니다.</div>
        ) : (
          <table className="paw-admin-table">
            <thead>
              <tr>
                <th>이름</th>
                <th>이메일</th>
                <th>가입일</th>
                <th>등급</th>
                <th>총 후원금</th>
                <th>역할</th>
              </tr>
            </thead>
            <tbody>
              {(data.content ?? []).map((user) => (
                <tr key={user.id}>
                  <td>{user.tierEmoji} {user.name}</td>
                  <td>{user.email || user.username}</td>
                  <td>{user.createdAt ? new Date(user.createdAt).toLocaleDateString("ko-KR") : "-"}</td>
                  <td>{user.tierLabel}</td>
                  <td>{WON.format(Number(user.totalDonatedAmount ?? 0))}원</td>
                  <td>
                    <select value={user.role === "MEMBER" ? "USER" : user.role} onChange={(event) => changeRole(user.id, event.target.value)}>
                      <option value="USER">USER</option>
                      <option value="ADMIN">ADMIN</option>
                    </select>
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
