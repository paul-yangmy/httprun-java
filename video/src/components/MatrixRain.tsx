import React, { useMemo } from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { COLORS } from "../constants";

// Reduced character set for performance
const CHARS = "アイウエオABCDEF012!@#";

type Column = {
  x: number;
  speed: number;
  length: number;
  chars: string[];
  startFrame: number;
};

// Use SVG-based rendering for far fewer DOM nodes (one <text> per column instead of per char)
export const MatrixRain: React.FC<{ opacity?: number }> = ({ opacity = 0.15 }) => {
  const { width, height, fps } = useVideoConfig();
  const frame = useCurrentFrame();

  // Sparse columns: every 28px → ~68 columns for 1920px
  const colStep = 28;
  const cols = Math.floor(width / colStep);

  const columns = useMemo<Column[]>(() => {
    return Array.from({ length: cols }, (_, i) => {
      const len = 6 + Math.floor(Math.abs(Math.sin(i * 7.3)) * 6);
      const chars = Array.from({ length: len }, (__, j) => {
        const idx = Math.floor(Math.abs(Math.sin(i * 13 + j * 7)) * CHARS.length);
        return CHARS[idx % CHARS.length];
      });
      return {
        x: i * colStep + 4,
        speed: 0.7 + Math.abs(Math.sin(i * 5.1)) * 1.1,
        length: len,
        chars,
        startFrame: Math.floor(Math.abs(Math.sin(i * 2.3)) * fps * 2),
      };
    });
  }, [cols, fps]);

  const charSize = 16;
  const lineH = 20;

  return (
    <svg
      style={{
        position: "absolute",
        top: 0,
        left: 0,
        width,
        height,
        overflow: "hidden",
        opacity,
        pointerEvents: "none",
      }}
      width={width}
      height={height}
    >
      {columns.map((col, ci) => {
        const elapsed = Math.max(0, frame - col.startFrame);
        const yOffset = (elapsed * col.speed) % (height + col.length * lineH);
        const headY = yOffset;

        // Skip if fully offscreen
        if (headY - col.length * lineH > height) return null;

        // Flicker char for head
        const swapIdx = Math.floor(Math.abs(Math.sin(frame * 0.3 + ci * 11)) * CHARS.length);
        const headChar = CHARS[swapIdx % CHARS.length];

        return (
          <g key={ci}>
            {col.chars.map((ch, ri) => {
              const cy = headY + ri * lineH;
              if (cy < -lineH || cy > height + lineH) return null;
              const isHead = ri === 0;
              const tailFade = Math.max(0, 1 - ri / col.length);
              return (
                <text
                  key={ri}
                  x={col.x}
                  y={cy}
                  fontSize={charSize}
                  fontFamily="'Courier New', monospace"
                  fill={isHead ? "#FFFFFF" : ri < 3 ? COLORS.green : COLORS.greenDark}
                  opacity={isHead ? 1 : tailFade}
                  style={{
                    filter: isHead ? `drop-shadow(0 0 6px ${COLORS.green})` : undefined,
                  }}
                >
                  {isHead ? headChar : ch}
                </text>
              );
            })}
          </g>
        );
      })}
    </svg>
  );
};
