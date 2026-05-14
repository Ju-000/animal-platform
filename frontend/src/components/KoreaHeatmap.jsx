import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ComposableMap, Geographies, Geography } from "react-simple-maps";
import { scaleSequential } from "d3-scale";

const KOREA_GEO_URL =
  "https://raw.githubusercontent.com/southkorea/southkorea-maps/master/kostat/2018/json/skorea-provinces-2018-geo.json";

const PROVINCE_ALIASES = {
  서울: "서울특별시",
  부산: "부산광역시",
  대구: "대구광역시",
  인천: "인천광역시",
  광주: "광주광역시",
  대전: "대전광역시",
  울산: "울산광역시",
  세종: "세종특별자치시",
  경기: "경기도",
  강원: "강원특별자치도",
  충북: "충청북도",
  충남: "충청남도",
  전북: "전북특별자치도",
  전남: "전라남도",
  경북: "경상북도",
  경남: "경상남도",
  제주: "제주특별자치도",
  강원도: "강원특별자치도",
  전라북도: "전북특별자치도"
};

function normalizeProvinceName(value) {
  if (!value) return "";
  const text = String(value).trim();
  if (PROVINCE_ALIASES[text]) return PROVINCE_ALIASES[text];
  return Object.entries(PROVINCE_ALIASES).find(([shortName]) => text.startsWith(shortName))?.[1] ?? text;
}

function featureName(feature) {
  const properties = feature?.properties ?? {};
  return (
    properties.name ||
    properties.NAME_1 ||
    properties.CTP_KOR_NM ||
    properties.SIG_KOR_NM ||
    properties.name_eng ||
    ""
  );
}

function yellowToRed(t) {
  const start = [255, 246, 199];
  const end = [197, 31, 26];
  const channel = (index) => Math.round(start[index] + (end[index] - start[index]) * t);
  return `rgb(${channel(0)}, ${channel(1)}, ${channel(2)})`;
}

export function KoreaHeatmapLegend({ min = 0, max = 0, colorScale }) {
  const ticks = [min, Math.round((min + max) / 2), max];

  return (
    <div className="paw-map-legend" aria-label="구조 건수 색상 범례">
      <div className="paw-map-legend-gradient" />
      <div className="paw-map-legend-labels">
        {ticks.map((tick, index) => (
          <span key={`${tick}-${index}`} style={{ color: colorScale ? colorScale(tick) : undefined }}>
            {tick.toLocaleString()}건
          </span>
        ))}
      </div>
    </div>
  );
}

export default function KoreaHeatmap({ regions = [] }) {
  const navigate = useNavigate();
  const [geoData, setGeoData] = useState(null);
  const [tooltip, setTooltip] = useState(null);

  useEffect(() => {
    let active = true;

    fetch(KOREA_GEO_URL)
      .then((response) => response.json())
      .then((data) => {
        if (!active) return;
        setGeoData(data);
      })
      .catch(() => {
        if (!active) return;
        setGeoData(null);
      });

    return () => {
      active = false;
    };
  }, []);

  const countsByProvince = useMemo(() => {
    const next = new Map();
    regions.forEach((item) => {
      const name = normalizeProvinceName(item.label);
      next.set(name, (next.get(name) ?? 0) + Number(item.value ?? 0));
    });
    return next;
  }, [regions]);

  const maxCount = useMemo(
    () => Math.max(0, ...Array.from(countsByProvince.values())),
    [countsByProvince]
  );

  const colorScale = useMemo(
    () => scaleSequential(yellowToRed).domain([0, Math.max(maxCount, 1)]),
    [maxCount]
  );

  function handleClick(name) {
    const normalized = normalizeProvinceName(name);
    navigate(`/animals?region=${encodeURIComponent(normalized)}`);
  }

  if (!geoData) {
    return <div className="paw-map-empty">대한민국 지도를 불러오는 중입니다.</div>;
  }

  return (
    <div className="paw-korea-map-shell">
      <div className="paw-korea-map-canvas">
        <ComposableMap
          projection="geoMercator"
          projectionConfig={{ center: [127.8, 36.2], scale: 5200 }}
          width={640}
          height={760}
        >
          <Geographies geography={geoData}>
            {({ geographies }) =>
              geographies.map((geo) => {
                const name = normalizeProvinceName(featureName(geo));
                const count = countsByProvince.get(name) ?? 0;
                return (
                  <Geography
                    key={geo.rsmKey}
                    geography={geo}
                    fill={colorScale(count)}
                    stroke="#ffffff"
                    strokeWidth={1.2}
                    style={{
                      default: { outline: "none" },
                      hover: { outline: "none", fill: "#ff7b11", cursor: "pointer" },
                      pressed: { outline: "none" }
                    }}
                    onMouseEnter={(event) => {
                      setTooltip({
                        name,
                        count,
                        x: event.clientX,
                        y: event.clientY
                      });
                    }}
                    onMouseMove={(event) => {
                      setTooltip((current) =>
                        current ? { ...current, x: event.clientX, y: event.clientY } : current
                      );
                    }}
                    onMouseLeave={() => setTooltip(null)}
                    onClick={() => handleClick(name)}
                  />
                );
              })
            }
          </Geographies>
        </ComposableMap>
      </div>

      <KoreaHeatmapLegend min={0} max={maxCount} colorScale={colorScale} />

      {tooltip ? (
        <div className="paw-map-tooltip" style={{ left: tooltip.x + 14, top: tooltip.y + 14 }}>
          <strong>{tooltip.name}</strong>
          <span>{tooltip.count.toLocaleString()}건</span>
        </div>
      ) : null}
    </div>
  );
}
