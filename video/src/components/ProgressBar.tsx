import React from "react";
import { useCurrentFrame, interpolate, Easing } from "remotion";
import { COLORS, FONTS } from "../constants";

type Props = {
  label: string;
  startFrame: number;
  /** Frames until 100% */
  duration: number;
  color?: string;
};

export const ProgressBar: React.FC<Props> = ({
  label,
  startFrame,
  duration,
  color = COLORS.green,
}) => {
  const frame = useCurrentFrame();
  const elapsed = Math.max(0, frame - startFrame);

  const progress = interpolate(elapsed, [0, duration], [0, 1], {
    extrapolateRight: "clamp",
    extrapolateLeft: "clamp",
    easing: Easing.inOut(Easing.quad),
  });

  const pct = Math.round(progress * 100);
  const done = progress >= 0.99;

  // Shimmer effect on the fill
  const shimmer = interpolate(
    ((frame - startFrame) % 20) / 20,
    [0, 0.5, 1],
    [0.7, 1, 0.7]
  );

  return (
    <div style={{ marginBottom: 16 }}>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          fontFamily: FONTS.mono,
          fontSize: 13,
          color: done ? color : COLORS.grayLight,
          marginBottom: 5,
          textShadow: done ? `0 0 8px ${color}` : "none",
        }}
      >
        <span>
          {done ? "✓ " : "  "}
          {label}
        </span>
        <span>{pct}%</span>
      </div>
      <div
        style={{
          height: 6,
          background: "rgba(255,255,255,0.04)",
          border: `1px solid rgba(0,255,65,0.18)`,
          borderRadius: 2,
          overflow: "hidden",
        }}
      >
        <div
          style={{
            height: "100%",
            width: `${progress * 100}%`,
            background: done
              ? `linear-gradient(90deg, ${color}99, ${color}, #FFFFFF88, ${color})`
              : `linear-gradient(90deg, ${color}66, ${color})`,
            opacity: done ? 1 : shimmer,
            boxShadow: `0 0 10px ${color}`,
          }}
        />
      </div>
    </div>
  );
};
