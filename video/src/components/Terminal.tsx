import React from "react";
import { interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { COLORS, FONTS } from "../constants";
import { TypewriterText } from "./TypewriterText";

type TerminalLine = {
  type: "prompt" | "output" | "comment" | "blank";
  text: string;
  color?: string;
  delay?: number; // frames after terminal appears
};

type Props = {
  title?: string;
  lines: TerminalLine[];
  startFrame?: number;
  style?: React.CSSProperties;
  charsPerFrame?: number;
};

export const Terminal: React.FC<Props> = ({
  title = "bash",
  lines,
  startFrame = 0,
  style,
  charsPerFrame = 2,
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const elapsed = frame - startFrame;

  const fadeIn = interpolate(elapsed, [0, fps * 0.3], [0, 1], {
    extrapolateRight: "clamp",
    extrapolateLeft: "clamp",
  });

  // Determine which lines are visible and how many chars to show
  let accumulatedFrames = fps * 0.3; // initial delay
  const lineStates = lines.map((line) => {
    const lineDelay = line.delay ?? 0;
    const lineStart = accumulatedFrames + lineDelay;
    const lineElapsed = Math.max(0, elapsed - lineStart);
    const totalChars = Math.floor(lineElapsed * charsPerFrame);
    const visible = elapsed >= lineStart;
    if (line.type !== "blank") {
      accumulatedFrames = lineStart + lineDelay + line.text.length / charsPerFrame + fps * 0.2;
    } else {
      accumulatedFrames = lineStart + fps * 0.1;
    }
    return { line, lineStart, totalChars, visible };
  });

  return (
    <div
      style={{
        opacity: fadeIn,
        background: "rgba(0,0,0,0.9)",
        border: `1px solid ${COLORS.green}`,
        borderRadius: 4,
        fontFamily: FONTS.mono,
        fontSize: 18,
        boxShadow: `0 0 20px rgba(0,255,65,0.3), inset 0 0 30px rgba(0,0,0,0.5)`,
        overflow: "hidden",
        ...style,
      }}
    >
      {/* Title bar */}
      <div
        style={{
          background: "#111",
          borderBottom: `1px solid ${COLORS.greenDark}`,
          padding: "8px 16px",
          display: "flex",
          alignItems: "center",
          gap: 8,
        }}
      >
        <span style={{ color: COLORS.red, fontSize: 12 }}>●</span>
        <span style={{ color: COLORS.yellow, fontSize: 12 }}>●</span>
        <span style={{ color: COLORS.green, fontSize: 12 }}>●</span>
        <span
          style={{
            color: COLORS.grayLight,
            fontSize: 13,
            marginLeft: 8,
            fontFamily: FONTS.mono,
          }}
        >
          {title}
        </span>
      </div>
      {/* Content */}
      <div style={{ padding: "16px 20px", lineHeight: 1.7 }}>
        {lineStates.map((state, i) => {
          if (!state.visible) return null;
          const { line, totalChars } = state;

          if (line.type === "blank") {
            return <div key={i} style={{ height: 6 }} />;
          }

          const displayText = line.text.slice(0, totalChars);
          const isLastLine = i === lineStates.length - 1;
          const showCursor = isLastLine && totalChars < line.text.length;

          const color =
            line.color ??
            (line.type === "prompt"
              ? COLORS.green
              : line.type === "comment"
              ? COLORS.grayLight
              : COLORS.white);

          const prefix =
            line.type === "prompt" ? (
              <span style={{ color: COLORS.cyan }}>$ </span>
            ) : line.type === "comment" ? (
              <span style={{ color: COLORS.grayLight }}>  </span>
            ) : (
              <span style={{ color: "transparent" }}>  </span>
            );

          return (
            <div key={i} style={{ display: "flex", alignItems: "flex-start" }}>
              {prefix}
              <span style={{ color, textShadow: color === COLORS.green ? `0 0 6px ${COLORS.green}` : "none" }}>
                {displayText}
                {showCursor && (
                  <span
                    style={{
                      opacity: Math.round(frame / 8) % 2,
                      color: COLORS.green,
                    }}
                  >
                    _
                  </span>
                )}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
};
