import { useId } from "react";

interface GhostIconProps {
  className?: string;
}

export default function GhostIcon({ className }: GhostIconProps) {
  const uid = useId();
  const gradId = `ghost-grad-${uid}`;
  const filterId = `ghost-drop-${uid}`;

  return (
    <svg
      viewBox="0 0 80 80"
      className={className}
      xmlns="http://www.w3.org/2000/svg"
    >
      <defs>
        <radialGradient id={gradId} cx="36%" cy="28%" r="65%">
          <stop offset="0%" stopColor="#f1f5f9" />
          <stop offset="55%" stopColor="#94a3b8" />
          <stop offset="100%" stopColor="#475569" />
        </radialGradient>
        <filter id={filterId} x="-25%" y="-25%" width="155%" height="165%">
          <feDropShadow
            dx="1"
            dy="4"
            stdDeviation="3.5"
            floodColor="#0f172a"
            floodOpacity="0.3"
          />
        </filter>
      </defs>

      {/* 몸통 */}
      <path
        d="M 12 35 A 28 28 0 0 0 68 35 L 68 63 Q 59 73 50 63 Q 40 73 30 63 Q 21 73 12 63 Z"
        fill={`url(#${gradId})`}
        filter={`url(#${filterId})`}
        opacity="0.92"
      />

      {/* 상단 광택 */}
      <ellipse
        cx="30"
        cy="18"
        rx="12"
        ry="7.5"
        fill="white"
        fillOpacity="0.48"
        transform="rotate(-20 30 18)"
      />

      {/* 눈 */}
      <ellipse cx="30" cy="30" rx="5.5" ry="6.5" fill="#1e293b" />
      <ellipse cx="50" cy="30" rx="5.5" ry="6.5" fill="#1e293b" />

      <ellipse cx="29" cy="27" rx="2" ry="2.5" fill="#334155" />
      <ellipse cx="49" cy="27" rx="2" ry="2.5" fill="#334155" />
    </svg>
  );
}
