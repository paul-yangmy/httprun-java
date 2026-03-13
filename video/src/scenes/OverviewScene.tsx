import React from "react";
import {
  AbsoluteFill,
  interpolate,
  spring,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { COLORS, FONTS } from "../constants";
import { MatrixRain } from "../components/MatrixRain";
import { Scanlines } from "../components/Scanlines";
import { GradientText } from "../components/GradientText";
import { CountUp } from "../components/CountUp";
import { HudCorners } from "../components/HudCorners";
import { MatrixSideStream } from "../components/MatrixSideStream";
import { StatusBar } from "../components/StatusBar";

const STATS = [
  { val: 100, plus: true, suffix: "+", label: "内置安全策略", color: COLORS.green },
  { val: 2, plus: false, suffix: "种", label: "LOCAL + SSH 执行", color: COLORS.cyan },
  { val: 3, plus: false, suffix: "层", label: "认证 / 鉴权 / 审计", color: COLORS.yellow },
];

export const OverviewScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const headOpacity = interpolate(frame, [0, fps * 0.5], [0, 1], {
    extrapolateRight: "clamp",
  });
  const headSlide = interpolate(headOpacity, [0, 1], [-50, 0]);

  // Description typewriter
  const DESC = "将 Shell 命令封装为安全的 REST API，统一管控本地与远程主机执行。";
  const descChars = Math.floor(
    interpolate(frame, [fps * 0.7, fps * 2.8], [0, DESC.length], {
      extrapolateRight: "clamp",
      extrapolateLeft: "clamp",
    })
  );
  const cursorBlink = Math.floor(frame / 8) % 2 === 0;

  return (
    <AbsoluteFill style={{ background: COLORS.bg }}>
      <MatrixRain opacity={0.07} />
      <MatrixSideStream opacity={0.45} />
      <HudCorners label="PROJECT//OVERVIEW" showCoords />
      <StatusBar />

      <AbsoluteFill
        style={{
          display: "flex",
          flexDirection: "column",
          justifyContent: "center",
          padding: "80px 130px",
        }}
      >
        {/* Section label */}
        <div
          style={{
            opacity: headOpacity,
            transform: `translateX(${headSlide}px)`,
            fontFamily: FONTS.mono,
            fontSize: 18,
            color: COLORS.cyan,
            letterSpacing: 6,
            marginBottom: 18,
            textShadow: `0 0 10px ${COLORS.cyan}`,
          }}
        >
          {"// PROJECT_OVERVIEW.md"}
        </div>

        {/* Gradient headline */}
        <div
          style={{
            opacity: headOpacity,
            transform: `translateX(${headSlide}px)`,
            marginBottom: 12,
          }}
        >
          <GradientText
            text="什么是 HttpRun?"
            fontSize={76}
            colors={["#00FF41", "#00FFFF", "#FFD700"]}
            speed={0.013}
            width={950}
          />
        </div>

        {/* Typewriter description */}
        <div
          style={{
            fontFamily: FONTS.mono,
            fontSize: 30,
            color: COLORS.white,
            lineHeight: 1.7,
            marginBottom: 64,
            minHeight: 76,
          }}
        >
          <span style={{ color: COLORS.cyan }}>{">"} </span>
          {DESC.slice(0, descChars)}
          {descChars < DESC.length && cursorBlink && (
            <span
              style={{
                display: "inline-block",
                width: 14,
                height: 26,
                background: COLORS.green,
                marginLeft: 2,
                verticalAlign: "middle",
              }}
            />
          )}
        </div>

        {/* Stats row */}
        <div style={{ display: "flex", gap: 48 }}>
          {STATS.map((s, i) => {
            const delay = fps * 2.8 + i * fps * 0.28;
            const sp = spring({
              frame: frame - delay,
              fps,
              config: { damping: 12, stiffness: 70 },
              durationInFrames: fps,
            });
            return (
              <div
                key={i}
                style={{
                  opacity: Math.min(1, sp * 2),
                  transform: `translateY(${interpolate(sp, [0, 1], [32, 0])}px)`,
                  textAlign: "center",
                  padding: "22px 44px",
                  background: "rgba(0,0,0,0.7)",
                  border: `1px solid ${s.color}44`,
                  borderBottom: `3px solid ${s.color}`,
                  position: "relative",
                  overflow: "hidden",
                }}
              >
                {/* top glow line */}
                <div
                  style={{
                    position: "absolute",
                    top: 0,
                    left: 0,
                    right: 0,
                    height: 1,
                    background: `linear-gradient(90deg, transparent, ${s.color}, transparent)`,
                    opacity: 0.6,
                  }}
                />
                <div>
                  <CountUp
                    target={s.val}
                    suffix={s.suffix}
                    startFrame={delay}
                    duration={fps * 0.8}
                    fontSize={58}
                    color={s.color}
                  />
                </div>
                <div
                  style={{
                    fontFamily: FONTS.mono,
                    fontSize: 13,
                    color: COLORS.grayLight,
                    marginTop: 6,
                    letterSpacing: 1,
                  }}
                >
                  {s.label}
                </div>
              </div>
            );
          })}
        </div>
      </AbsoluteFill>

      <Scanlines />
    </AbsoluteFill>
  );
};
