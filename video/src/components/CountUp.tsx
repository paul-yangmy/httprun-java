import React from "react";
import { useCurrentFrame, interpolate, Easing } from "remotion";
import { COLORS, FONTS } from "../constants";

type Props = {
  target: number;
  prefix?: string;
  suffix?: string;
  startFrame?: number;
  /** Duration to count up in frames */
  duration?: number;
  fontSize?: number;
  color?: string;
};

export const CountUp: React.FC<Props> = ({
  target,
  prefix = "",
  suffix = "",
  startFrame = 0,
  duration = 60,
  fontSize = 60,
  color = COLORS.green,
}) => {
  const frame = useCurrentFrame();
  const elapsed = Math.max(0, frame - startFrame);

  const value = interpolate(elapsed, [0, duration], [0, target], {
    extrapolateRight: "clamp",
    extrapolateLeft: "clamp",
    easing: Easing.out(Easing.cubic),
  });

  return (
    <span
      style={{
        fontFamily: FONTS.mono,
        fontSize,
        fontWeight: "bold",
        color,
        textShadow: `0 0 20px ${color}, 0 0 40px ${color}55`,
        letterSpacing: 2,
      }}
    >
      {prefix}
      {Math.floor(value).toLocaleString()}
      {suffix}
    </span>
  );
};
