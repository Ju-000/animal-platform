import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import banner1 from "../../assets/banner1.png";
import banner2 from "../../assets/banner2.png";
import banner3 from "../../assets/banner3.png";
import banner4 from "../../assets/banner4.png";
import banner5 from "../../assets/banner5.png";
import banner6 from "../../assets/banner6.png";
import banner7 from "../../assets/banner7.png";

const banners = [
  { src: banner1, link: "/animals" },
  { src: banner2, link: null },
  { src: banner3, link: "/shelters" },
  { src: banner4, link: "/favorites" },
  { src: banner5, link: "/animals" },
  { src: banner6, link: "/donation" },
  { src: banner7, link: null }
];

export default function HeroBannerCarousel() {
  const navigate = useNavigate();
  const [current, setCurrent] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrent((prev) => (prev + 1) % banners.length);
    }, 4000);

    return () => clearInterval(timer);
  }, []);

  const goToSlide = (index) => {
    setCurrent((index + banners.length) % banners.length);
  };

  const handleBannerClick = () => {
    const link = banners[current].link;
    if (link) {
      navigate(link);
    }
  };

  return (
    <div className="hero-banner-carousel" onClick={handleBannerClick}>
      {banners.map((banner, index) => (
        <img
          key={banner.src}
          className={`hero-banner-carousel-image ${index === current ? "active" : ""}`}
          src={banner.src}
          alt={`메인 배너 ${index + 1}`}
          style={{ cursor: banner.link ? "pointer" : "default" }}
          aria-hidden={index !== current}
        />
      ))}

      <button
        className="hero-banner-arrow hero-banner-arrow-left"
        type="button"
        aria-label="이전 배너"
        onClick={(event) => {
          event.stopPropagation();
          goToSlide(current - 1);
        }}
      >
        ‹
      </button>
      <button
        className="hero-banner-arrow hero-banner-arrow-right"
        type="button"
        aria-label="다음 배너"
        onClick={(event) => {
          event.stopPropagation();
          goToSlide(current + 1);
        }}
      >
        ›
      </button>

      <div className="hero-banner-dots" role="tablist" aria-label="메인 배너 선택">
        {banners.map((banner, index) => (
          <button
            key={`${banner.src}-dot`}
            className={`hero-banner-dot ${index === current ? "active" : ""}`}
            type="button"
            aria-label={`${index + 1}번째 배너 보기`}
            aria-selected={index === current}
            onClick={(event) => {
              event.stopPropagation();
              goToSlide(index);
            }}
          />
        ))}
      </div>
    </div>
  );
}
