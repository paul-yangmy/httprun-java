import React from "react";
import { useCurrentFrame, spring, interpolate, useVideoConfig } from "remotion";
import { COLORS, FONTS } from "../constants";

type Props = {
  icon: string;
  title: string;
  desc: string;
  /** Frame offset from scene start to begin entrance animation */
  delay?: number;
  accentColor?: string;
};

export const NeonCard: React.FC<Props> = ({
  icon,
  title,
  desc,
  delay = 0,
  accentColor = COLORS.green,
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const progress = spring({
    frame: frame - delay,
    fps,
    config: { damping: 14, stiffness: 90 },
    durationInFrames: fps,
  });

  const opacity = interpolate(progress, [0, 0.3], [0, 1], { extrapolateRight: "clamp" });
  const translateY = interpolate(progress, [0, 1], [28, 0]);
  const scale = interpolate(progress, [0, 1], [0.88, 1]);

  // Pulsing glow
  const pulse = 0.6 + Math.sin(frame * 0.09 + delay * 0.05) * 0.4;

  return (
    <div
      style={{
        opacity,
        transform: `translateY(${translateY}px) scale(${scale})`,
        padding: "16px 20px",
        background: "rgba(0,0,0,0.85)",
        border: `1px solid ${accentColor}55`,
        borderLeft: `3px solid ${accentColor}`,
        borderRadius: 3,
        boxShadow: `0 0 ${14 * pulse}px ${accentColor}${Math.round(pulse * 50).toString(16).padStart(2, "0")}, inset 0 0 24px rgba(0,0,0,0.6)`,
        position: "relative",
        overflow: "hidden",
      }}
    >
      {/* Animated scan line */}
      <div
        style={{
          position: "absolute",
          top: 0,
          left: 0,
          right: 0,
          height: 1,
          background: `linear-gradient(90deg, transparent, ${accentColor}, transparent)`,
          opacity: 0.5 * pulse,
        }}
      />
      <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 6 }}>
        <span style={{ fontSize: 22, lineHeight: 1 }}>{icon}</span>
        <span
          style={{
            fontFamily: FONTS.mono,
            fontSize: 15,
            fontWeight: "bold",
            color: accentColor,
            textShadow: `0 0 8px ${accentColor}`,
          }}
        >
          {title}
        </span>
      </div>
      <div
        style={{
          fontFamily: FONTS.mono,
          fontSize: 13,
          color: COLORS.grayLight,
          lineHeight: 1.55,
          paddingLeft: 34,
        }}
      >
        {desc}
      </div>
    </div>
  );
};
