import { Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

export default function AdminCharts({ data }) {
  const responseTimes = data ?? [];

  return (
    <div className="paw-ops-chart-card">
      <div>
        <h4>{"공공 API 응답 시간"}</h4>
        <p>{"최근 20회 호출 기준"}</p>
      </div>
      {responseTimes.length ? (
        <ResponsiveContainer width="100%" height={220}>
          <LineChart data={responseTimes} margin={{ top: 10, right: 18, left: -18, bottom: 0 }}>
            <XAxis dataKey="label" tickLine={false} axisLine={false} />
            <YAxis tickLine={false} axisLine={false} width={54} />
            <Tooltip formatter={(value) => [`${value}ms`, "응답 시간"]} labelFormatter={(label) => `${label}번째 호출`} />
            <Line type="monotone" dataKey="responseMs" stroke="#ff7b11" strokeWidth={3} dot={{ r: 4 }} activeDot={{ r: 6 }} />
          </LineChart>
        </ResponsiveContainer>
      ) : (
        <div className="paw-message-card">{"아직 기록된 공공 API 호출이 없습니다."}</div>
      )}
    </div>
  );
}
