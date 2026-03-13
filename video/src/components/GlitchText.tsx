import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { COLORS, FONTS } from "../constants";

type Props = {
  text: string;
  style?: React.CSSProperties;
  intensity?: number;
};

// Maps glitch frame offset to character shift
const GLITCH_CHARS = "!@#$%^&*<>?/\\|~`";

export const GlitchText: React.FC<Props> = ({ text, style, intensity = 1 }) => {
  const frame = useCurrentFrame();

  // Glitch triggers at specific frames
  const glitchActive = intensity > 0 && (frame % 17 < 2 || frame % 31 < 1);
  const offsetX = glitchActive ? (frame % 7) - 3 : 0;
  const offsetY = glitchActive ? (frame % 5) - 2 : 0;
  const redShift = glitchActive ? 3 : 0;

  // Corrupt random characters during glitch
  const displayText = glitchActive
    ? text
        .split("")
        .map((ch, i) => {
          if ((frame + i) % 11 < 2) {
            return GLITCH_CHARS[(frame + i) % GLITCH_CHARS.length];
          }
          return ch;
        })
        .join("")
    : text;

  return (
    <div style={{ position: "relative", display: "inline-block", ...style }}>
      {/* Red channel */}
      {glitchActive && (
        <span
          style={{
            position: "absolute",
            left: redShift,
            top: 0,
            color: COLORS.red,
            opacity: 0.6,
            fontFamily: FONTS.mono,
            mixBlendMode: "screen",
            pointerEvents: "none",
            whiteSpace: "inherit" as any,
            fontSize: "inherit",
            fontWeight: "inherit",
          }}
          aria-hidden
        >
          {text}
        </span>
      )}
      {/* Cyan channel */}
      {glitchActive && (
        <span
          style={{
            position: "absolute",
            left: -redShift,
            top: 0,
            color: COLORS.cyan,
            opacity: 0.6,
            fontFamily: FONTS.mono,
            mixBlendMode: "screen",
            pointerEvents: "none",
            whiteSpace: "inherit" as any,
            fontSize: "inherit",
            fontWeight: "inherit",
          }}
          aria-hidden
        >
          {text}
        </span>
      )}
      <span
        style={{
          position: "relative",
          fontFamily: FONTS.mono,
          transform: `translate(${offsetX}px, ${offsetY}px)`,
          display: "inline-block",
          color: "inherit",
        }}
      >
        {displayText}
      </span>
    </div>
  );
};
