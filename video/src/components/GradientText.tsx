import React from "react";
import { useCurrentFrame } from "remotion";

type Props = {
  text: string;
  fontSize: number;
  fontFamily?: string;
  /** Three color stops that cycle */
  colors?: [string, string, string];
  /** How fast the gradient shifts per frame */
  speed?: number;
  width?: number;
  height?: number;
  fontWeight?: string | number;
  style?: React.CSSProperties;
};

function lerpHex(c1: string, c2: string, t: number): string {
  const r1 = parseInt(c1.slice(1, 3), 16);
  const g1 = parseInt(c1.slice(3, 5), 16);
  const b1 = parseInt(c1.slice(5, 7), 16);
  const r2 = parseInt(c2.slice(1, 3), 16);
  const g2 = parseInt(c2.slice(3, 5), 16);
  const b2 = parseInt(c2.slice(5, 7), 16);
  const r = Math.round(r1 + (r2 - r1) * t);
  const g = Math.round(g1 + (g2 - g1) * t);
  const b = Math.round(b1 + (b2 - b1) * t);
  return `rgb(${r},${g},${b})`;
}

export const GradientText: React.FC<Props> = ({
  text,
  fontSize,
  fontFamily = "'Courier New', monospace",
  colors = ["#00FF41", "#00FFFF", "#FFFFFF"],
  speed = 0.012,
  width = 1200,
  height,
  fontWeight = "bold",
  style,
}) => {
  const frame = useCurrentFrame();
  const h = height ?? Math.ceil(fontSize * 1.45);
  const offset = (frame * speed) % 1;

  const c1 = lerpHex(colors[0], colors[1], offset);
  const c2 = lerpHex(colors[1], colors[2], (offset + 0.33) % 1);
  const c3 = lerpHex(colors[2], colors[0], (offset + 0.66) % 1);

  // Unique id so multiple instances don't share id
  const uid = `gg_${text.replace(/[^a-zA-Z0-9]/g, "").slice(0, 8)}_${Math.abs(
    text.charCodeAt(0) + text.length
  )}`;

  return (
    <svg
      width={width}
      height={h}
      style={{ overflow: "visible", display: "block", ...style }}
    >
      <defs>
        <linearGradient id={uid} x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stopColor={c1} />
          <stop offset="50%" stopColor={c2} />
          <stop offset="100%" stopColor={c3} />
        </linearGradient>
        <filter id={`${uid}_glow`} x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur in="SourceGraphic" stdDeviation="4" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>
      <text
        x={0}
        y={fontSize}
        fontSize={fontSize}
        fontFamily={fontFamily}
        fontWeight={fontWeight}
        fill={`url(#${uid})`}
        filter={`url(#${uid}_glow)`}
      >
        {text}
      </text>
    </svg>
  );
};
