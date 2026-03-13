import React from "react";
import { useCurrentFrame, interpolate, useVideoConfig } from "remotion";
import { COLORS, FONTS } from "../constants";

type Props = {
  /** Corner bracket size in px */
  size?: number;
  thickness?: number;
  color?: string;
  /** Show targeting cross-hair label in top-left */
  label?: string;
  /** Show fake coordinates in corners */
  showCoords?: boolean;
};

export const HudCorners: React.FC<Props> = ({
  size = 40,
  thickness = 2,
  color = COLORS.green,
  label,
  showCoords = true,
}) => {
  const frame = useCurrentFrame();
  const { width, height } = useVideoConfig();

  // Entrance fade
  const entryOpacity = interpolate(frame, [0, 12], [0, 1], {
    extrapolateRight: "clamp",
  });

  // Slow pulse
  const pulse = 0.7 + Math.sin(frame * 0.07) * 0.3;

  // Cycling coordinate spoof
  const coordX = ((frame * 1.3) % 360).toFixed(4);
  const coordY = ((frame * 0.8 + 90) % 180).toFixed(4);
  const alt = (Math.sin(frame * 0.04) * 200 + 800).toFixed(1);

  const cornerStyle = (top: boolean, left: boolean): React.CSSProperties => ({
    position: "absolute",
    top: top ? 18 : undefined,
    bottom: !top ? 18 : undefined,
    left: left ? 18 : undefined,
    right: !left ? 18 : undefined,
    width: size,
    height: size,
    borderTop: top ? `${thickness}px solid ${color}` : undefined,
    borderBottom: !top ? `${thickness}px solid ${color}` : undefined,
    borderLeft: left ? `${thickness}px solid ${color}` : undefined,
    borderRight: !left ? `${thickness}px solid ${color}` : undefined,
    opacity: entryOpacity * pulse,
    boxSizing: "border-box",
  });

  const labelStyle: React.CSSProperties = {
    position: "absolute",
    fontFamily: FONTS.mono,
    fontSize: 11,
    color: `${color}AA`,
    letterSpacing: 1,
    lineHeight: 1.4,
    whiteSpace: "nowrap",
  };

  return (
    <div
      style={{
        position: "absolute",
        inset: 0,
        pointerEvents: "none",
        zIndex: 50,
      }}
    >
      {/* TL */}
      <div style={cornerStyle(true, true)} />
      {/* TR */}
      <div style={cornerStyle(true, false)} />
      {/* BL */}
      <div style={cornerStyle(false, true)} />
      {/* BR */}
      <div style={cornerStyle(false, false)} />

      {/* Top-left label */}
      {label && (
        <div style={{ ...labelStyle, top: 22, left: 22 + size + 8 }}>
          {label}
        </div>
      )}

      {/* Top-right: mode indicator */}
      <div
        style={{
          ...labelStyle,
          top: 22,
          right: 22 + size + 8,
          textAlign: "right",
          opacity: entryOpacity * pulse,
        }}
      >
        <span style={{ color: COLORS.red }}>● </span>REC
        <br />
        SYS/ACTIVE
      </div>

      {/* Bottom-left: coordinates */}
      {showCoords && (
        <div
          style={{
            ...labelStyle,
            bottom: 22,
            left: 22 + size + 8,
            opacity: entryOpacity * (0.6 + Math.sin(frame * 0.11) * 0.2),
          }}
        >
          LAT {coordX}° LON {coordY}°
          <br />
          ALT {alt}m
        </div>
      )}

      {/* Bottom-right: frame counter */}
      <div
        style={{
          ...labelStyle,
          bottom: 22,
          right: 22 + size + 8,
          textAlign: "right",
          opacity: entryOpacity * 0.7,
        }}
      >
        FR:{String(frame).padStart(4, "0")}
        <br />
        {new Date(2026, 2, 13).toISOString().slice(0, 10)}
      </div>

      {/* Center crosshair tick marks (subtle) */}
      {[
        { top: height / 2 - 1, left: 18, width: 12 },
        { top: height / 2 - 1, right: 18, width: 12 },
        { left: width / 2 - 1, top: 18, height: 12 },
        { left: width / 2 - 1, bottom: 18, height: 12 },
      ].map((s, i) => (
        <div
          key={i}
          style={{
            position: "absolute",
            background: color,
            width: s.width ?? thickness,
            height: s.height ?? thickness,
            ...s,
            opacity: entryOpacity * 0.3,
          }}
        />
      ))}
    </div>
  );
};
