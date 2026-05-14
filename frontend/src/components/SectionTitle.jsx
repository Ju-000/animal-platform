export default function SectionTitle({ eyebrow, title, description }) {
  return (
    <div className="section-title">
      {eyebrow ? <p className="eyebrow">{eyebrow}</p> : null}
      <h2>{title}</h2>
      {description ? <p className="section-description">{description}</p> : null}
    </div>
  );
}
