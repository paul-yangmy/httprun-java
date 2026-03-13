import React from "react";
import { interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { COLORS, FONTS } from "../constants";

type Props = {
  text: string;
  startFrame?: number;
  charsPerFrame?: number;
  style?: React.CSSProperties;
  showCursor?: boolean;
  color?: string;
};

export const TypewriterText: React.FC<Props> = ({
  text,
  startFrame = 0,
  charsPerFrame = 1,
  style,
  showCursor = true,
  color = COLORS.green,
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const elapsed = Math.max(0, frame - startFrame);
  const charsToShow = Math.min(text.length, Math.floor(elapsed * charsPerFrame));
  const displayText = text.slice(0, charsToShow);
  const isComplete = charsToShow >= text.length;

  // Cursor blink every 0.5s
  const cursorOpacity = isComplete
    ? Math.round(frame / (fps * 0.5)) % 2 === 0
      ? 1
      : 0
    : 1;

  return (
    <span
      style={{
        fontFamily: FONTS.mono,
        color,
        ...style,
      }}
    >
      {displayText}
      {showCursor && (
        <span
          style={{
            opacity: cursorOpacity,
            color: COLORS.green,
            textShadow: `0 0 8px ${COLORS.green}`,
          }}
        >
          _
        </span>
      )}
    </span>
  );
};
