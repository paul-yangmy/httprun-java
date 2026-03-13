import React from "react";
import { useCurrentFrame, useVideoConfig, interpolate } from "remotion";
import { COLORS, FONTS } from "../constants";

/**
 * Bottom HUD status bar — fake system metrics in Matrix style.
 * Modeled after cyberpunk/military terminal overlays.
 */
export const StatusBar: React.FC = () => {
  const frame = useCurrentFrame();
  const { width } = useVideoConfig();

  const fadeIn = interpolate(frame, [0, 18], [0, 1], { extrapolateRight: "clamp" });

  // Animated fake metrics
  const cpu = Math.round(18 + Math.abs(Math.sin(frame * 0.07)) * 45);
  const mem = Math.round(42 + Math.abs(Math.sin(frame * 0.04 + 1)) * 28);
  const net = (1.2 + Math.abs(Math.sin(frame * 0.09 + 2)) * 4.8).toFixed(1);
  const threads = Math.round(12 + Math.abs(Math.sin(frame * 0.06 + 3)) * 6);

  // Blinking separator
  const blink = Math.floor(frame / 15) % 2 === 0;

  const sep = (
    <span style={{ color: `${COLORS.greenDark}`, margin: "0 12px" }}>│</span>
  );

  const metric = (label: string, value: string, unit: string, warn = false) => (
    <span>
      <span style={{ color: COLORS.grayLight }}>{label}:</span>
      <span
        style={{
          color: warn ? COLORS.yellow : COLORS.green,
          textShadow: warn ? `0 0 6px ${COLORS.yellow}` : `0 0 6px ${COLORS.green}`,
          marginLeft: 6,
          marginRight: 2,
        }}
      >
        {value}
      </span>
      <span style={{ color: COLORS.grayLight, fontSize: 10 }}>{unit}</span>
    </span>
  );

  return (
    <div
      style={{
        position: "absolute",
        bottom: 0,
        left: 0,
        width,
        height: 32,
        background: "rgba(0,0,0,0.88)",
        borderTop: `1px solid ${COLORS.greenDark}`,
        display: "flex",
        alignItems: "center",
        paddingLeft: 20,
        paddingRight: 20,
        fontFamily: FONTS.mono,
        fontSize: 12,
        letterSpacing: 0.8,
        opacity: fadeIn,
        pointerEvents: "none",
        gap: 0,
        zIndex: 100,
        boxSizing: "border-box",
      }}
    >
      {/* Left: system label */}
      <span style={{ color: COLORS.cyan, textShadow: `0 0 6px ${COLORS.cyan}`, marginRight: 12 }}>
        HTTPRUN//
      </span>
      {sep}
      {metric("CPU", `${cpu}`, "%", cpu > 55)}
      {sep}
      {metric("MEM", `${mem}`, "%", mem > 60)}
      {sep}
      {metric("NET", String(net), "MB/s")}
      {sep}
      {metric("THR", String(threads), "")}
      {sep}
      <span style={{ color: COLORS.green, opacity: blink ? 1 : 0.3 }}>■</span>
      <span style={{ color: COLORS.grayLight, marginLeft: 6 }}>SECURE CHANNEL ACTIVE</span>

      {/* Right spacer + uptime */}
      <div style={{ flex: 1 }} />
      <span style={{ color: COLORS.grayLight }}>
        UPTIME:{" "}
        <span style={{ color: COLORS.greenDim }}>
          {String(Math.floor(frame / 30 / 60)).padStart(2, "0")}:
          {String(Math.floor(frame / 30) % 60).padStart(2, "0")}:
          {String(frame % 30).padStart(2, "0")}
        </span>
      </span>
      {sep}
      <span style={{ color: `${COLORS.green}88` }}>v1.0.0</span>
    </div>
  );
};
