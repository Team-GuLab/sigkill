import { useId, useMemo } from "react";

interface SlimeIconProps {
  className?: string;
}

const PASTEL_PALETTES = [
  {
    light: "#bfdbfe",
    mid: "#3b82f6",
    dark: "#1e3a8a",
    shadow: "#1e40af",
    eye: "#1e3a8a",
  },
  {
    light: "#fce7f3",
    mid: "#f472b6",
    dark: "#9d174d",
    shadow: "#be185d",
    eye: "#9d174d",
  },
  {
    light: "#d1fae5",
    mid: "#34d399",
    dark: "#065f46",
    shadow: "#059669",
    eye: "#065f46",
  },
  {
    light: "#ede9fe",
    mid: "#a78bfa",
    dark: "#4c1d95",
    shadow: "#5b21b6",
    eye: "#4c1d95",
  },
  {
    light: "#ffedd5",
    mid: "#fb923c",
    dark: "#9a3412",
    shadow: "#c2410c",
    eye: "#9a3412",
  },
  {
    light: "#fef9c3",
    mid: "#facc15",
    dark: "#854d0e",
    shadow: "#a16207",
    eye: "#78350f",
  },
] as const;

export default function SlimeIcon({ className }: SlimeIconProps) {
  const uid = useId();
  const gradId = `slime-grad-${uid}`;
  const filterId = `slime-drop-${uid}`;

  const palette = useMemo(
    () => PASTEL_PALETTES[Math.floor(Math.random() * PASTEL_PALETTES.length)],
    [],
  );

  return (
    <svg
      viewBox="0 0 85 85"
      className={className}
      xmlns="http://www.w3.org/2000/svg"
    >
      <defs>
        <radialGradient id={gradId} cx="33%" cy="26%" r="70%">
          <stop offset="0%" stopColor={palette.light} />
          <stop offset="50%" stopColor={palette.mid} />
          <stop offset="100%" stopColor={palette.dark} />
        </radialGradient>
        <filter id={filterId} x="-25%" y="-25%" width="155%" height="165%">
          <feDropShadow
            dx="1"
            dy="3"
            stdDeviation="4"
            floodColor={palette.shadow}
            floodOpacity="0.45"
          />
        </filter>
      </defs>

      {/* 몸통 */}
      <ellipse
        cx="40"
        cy="47"
        rx="32"
        ry="26"
        fill={`url(#${gradId})`}
        filter={`url(#${filterId})`}
      />

      {/* 메인 광택 - 좌상단 타원 */}
      <ellipse
        cx="27"
        cy="30"
        rx="15"
        ry="9"
        fill="white"
        fillOpacity="0.38"
        transform="rotate(-25 27 30)"
      />

      {/* 보조 광택 - 작은 점 */}
      <circle cx="20" cy="41" r="4" fill="white" fillOpacity="0.15" />

      {/* 눈 흰자 */}
      <circle cx="30" cy="47" r="8.5" fill="white" />
      <circle cx="50" cy="47" r="8.5" fill="white" />

      {/* 눈동자 */}
      <circle cx="31.5" cy="48" r="5" fill={palette.eye} />
      <circle cx="51.5" cy="48" r="5" fill={palette.eye} />

      {/* 눈동자 하이라이트 */}
      <circle cx="33.5" cy="45.5" r="2" fill="white" />
      <circle cx="53.5" cy="45.5" r="2" fill="white" />
    </svg>
  );
}
