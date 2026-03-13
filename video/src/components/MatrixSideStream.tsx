import React, { useMemo } from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { COLORS, FONTS } from "../constants";

/**
 * Vertical hex/binary data streams decorating the left and right margins.
 * Creates the "data pouring down on the sides" Matrix-feel.
 */
export const MatrixSideStream: React.FC<{ opacity?: number }> = ({ opacity = 0.55 }) => {
  const frame = useCurrentFrame();
  const { height } = useVideoConfig();

  const lineH = 19;
  const rows = Math.ceil(height / lineH);

  // Pre-generate deterministic hex bytes for left and right columns
  const leftLines = useMemo(
    () =>
      Array.from({ length: rows * 3 }, (_, i) => {
        const v = Math.floor(Math.abs(Math.sin(i * 17.3 + 1)) * 256);
        return v.toString(16).toUpperCase().padStart(2, "0");
      }),
    [rows]
  );
  const rightLines = useMemo(
    () =>
      Array.from({ length: rows * 3 }, (_, i) => {
        const v = Math.floor(Math.abs(Math.sin(i * 13.7 + 5)) * 256);
        return v.toString(16).toUpperCase().padStart(2, "0");
      }),
    [rows]
  );

  const scrollOffset = (frame * 1.4) % (rows * lineH);

  const renderColumn = (
    lines: string[],
    side: "left" | "right",
    extraCols: { dx: number; speed: number; color: string }[]
  ) => (
    <div
      style={{
        position: "absolute",
        top: 0,
        [side]: 0,
        width: 54,
        height,
        overflow: "hidden",
        opacity,
        pointerEvents: "none",
      }}
    >
      {extraCols.map((col, ci) => {
        const colOffset = (frame * col.speed) % (rows * lineH);
        return (
          <div
            key={ci}
            style={{
              position: "absolute",
              top: -colOffset,
              left: ci === 0 ? col.dx : undefined,
              right: ci !== 0 && side === "right" ? col.dx : undefined,
              display: "flex",
              flexDirection: "column",
            }}
          >
            {Array.from({ length: rows * 2 }, (_, ri) => {
              const idx = (ci * rows + ri) % lines.length;
              const isActive = ri % 7 === (Math.floor(frame / 5) % 7);
              return (
                <span
                  key={ri}
                  style={{
                    fontFamily: FONTS.mono,
                    fontSize: 12,
                    lineHeight: `${lineH}px`,
                    color: isActive ? "#FFFFFF" : col.color,
                    textShadow: isActive ? `0 0 6px ${col.color}` : undefined,
                    opacity: isActive ? 1 : 0.5 + Math.abs(Math.sin(ri * 0.7)) * 0.4,
                    letterSpacing: 1,
                  }}
                >
                  {lines[idx]}
                </span>
              );
            })}
          </div>
        );
      })}
    </div>
  );

  return (
    <>
      {renderColumn(leftLines, "left", [
        { dx: 4, speed: 1.4, color: COLORS.greenDim },
        { dx: 24, speed: 1.0, color: COLORS.greenDark },
      ])}
      {renderColumn(rightLines, "right", [
        { dx: 4, speed: 1.2, color: COLORS.greenDim },
        { dx: 24, speed: 1.6, color: COLORS.greenDark },
      ])}
    </>
  );
};
