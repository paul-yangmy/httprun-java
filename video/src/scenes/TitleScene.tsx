import React from "react";
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { COLORS, FONTS } from "../constants";
import { GlitchText } from "../components/GlitchText";
import { MatrixRain } from "../components/MatrixRain";
import { Scanlines } from "../components/Scanlines";
import { ParticleBurst } from "../components/ParticleBurst";
import { GradientText } from "../components/GradientText";
import { HudCorners } from "../components/HudCorners";
import { MatrixSideStream } from "../components/MatrixSideStream";
import { StatusBar } from "../components/StatusBar";

export const TitleScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps, width, height } = useVideoConfig();

  // Logo entrance spring
  const scale = spring({
    frame,
    fps,
    config: { damping: 10, stiffness: 70 },
    durationInFrames: fps * 1.5,
  });

  const logoOpacity = interpolate(frame, [0, fps * 0.4], [0, 1], {
    extrapolateRight: "clamp",
  });

  // Particle burst triggers when logo appears (frame 12)
  const BURST_FRAME = 12;

  const subtitleProgress = interpolate(frame, [fps * 0.9, fps * 1.8], [0, 1], {
    extrapolateRight: "clamp",
    extrapolateLeft: "clamp",
  });
  const SUBTITLE = "HTTP API Shell 命令网关";
  const subtitleCharsToShow = Math.floor(subtitleProgress * SUBTITLE.length);

  const badgeOpacity = interpolate(frame, [fps * 1.8, fps * 2.5], [0, 1], {
    extrapolateRight: "clamp",
    extrapolateLeft: "clamp",
  });

  // Vignette pulse
  const vignettePulse = 0.55 + Math.sin(frame * 0.06) * 0.1;

  return (
    <AbsoluteFill style={{ background: "#000" }}>
      <MatrixRain opacity={0.22} />
      <MatrixSideStream opacity={0.6} />
      <HudCorners label="HTTPRUN//SYS" showCoords />
      <StatusBar />

      {/* Radial vignette */}
      <div
        style={{
          position: "absolute",
          inset: 0,
          background: `radial-gradient(ellipse at center, transparent 30%, rgba(0,0,0,${vignettePulse}) 100%)`,
          pointerEvents: "none",
        }}
      />

      {/* Particle burst — fires from screen center when logo pops */}
      <ParticleBurst
        x={width / 2}
        y={height / 2 - 40}
        triggerFrame={BURST_FRAME}
        count={48}
        duration={50}
        colors={["#00FF41", "#00FFFF", "#FFD700", "#FFFFFF"]}
      />

      {/* Center content */}
      <AbsoluteFill
        style={{
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          gap: 20,
        }}
      >
        {/* Box + title */}
        <div
          style={{
            opacity: logoOpacity,
            transform: `scale(${scale})`,
            textAlign: "center",
          }}
        >
          <div
            style={{
              fontFamily: FONTS.mono,
              color: COLORS.greenDim,
              fontSize: 22,
              letterSpacing: 2,
              marginBottom: 8,
              textShadow: `0 0 8px ${COLORS.greenDark}`,
            }}
          >
            ┌────────────────────────────────────────────────────────┐
          </div>

          {/* Glitch title */}
          <div
            style={{
              fontFamily: FONTS.mono,
              fontSize: 96,
              fontWeight: "bold",
              color: COLORS.green,
              textShadow: `0 0 24px ${COLORS.green}, 0 0 48px ${COLORS.greenDim}, 0 0 96px ${COLORS.greenDark}`,
              letterSpacing: 14,
              lineHeight: 1.05,
            }}
          >
            <GlitchText text="HttpRun" intensity={1.2} />
          </div>

          {/* Gradient JAVA subtitle */}
          <div style={{ display: "flex", justifyContent: "center", marginTop: -4, marginBottom: 8 }}>
            <GradientText
              text="JAVA"
              fontSize={54}
              colors={["#00FFFF", "#00FF41", "#00FFFF"]}
              speed={0.018}
              width={320}
              style={{ letterSpacing: 20 } as React.CSSProperties}
            />
          </div>

          <div
            style={{
              fontFamily: FONTS.mono,
              color: COLORS.greenDim,
              fontSize: 22,
              letterSpacing: 2,
              marginTop: 4,
              textShadow: `0 0 8px ${COLORS.greenDark}`,
            }}
          >
            └────────────────────────────────────────────────────────┘
          </div>
        </div>

        {/* Typewriter subtitle */}
        <div
          style={{
            fontFamily: FONTS.mono,
            fontSize: 32,
            color: COLORS.grayLight,
            letterSpacing: 4,
            height: 48,
          }}
        >
          <span style={{ color: COLORS.greenDim }}>{">>> "}</span>
          {SUBTITLE.slice(0, subtitleCharsToShow)}
          {subtitleCharsToShow < SUBTITLE.length && (
            <span
              style={{
                opacity: Math.round(frame / 6) % 2,
                color: COLORS.green,
                textShadow: `0 0 8px ${COLORS.green}`,
              }}
            >
              _
            </span>
          )}
        </div>

        {/* Tech badges */}
        <div
          style={{
            display: "flex",
            gap: 16,
            opacity: badgeOpacity,
            flexWrap: "wrap",
            justifyContent: "center",
            transform: `translateY(${interpolate(badgeOpacity, [0, 1], [16, 0])}px)`,
          }}
        >
          {["Java 17", "Spring Boot 3.2", "REST API", "SSH", "JWT"].map((badge) => (
            <div
              key={badge}
              style={{
                fontFamily: FONTS.mono,
                fontSize: 15,
                color: COLORS.cyan,
                border: `1px solid ${COLORS.cyanDim}`,
                padding: "4px 14px",
                borderRadius: 2,
                background: "rgba(0,255,255,0.05)",
                textShadow: `0 0 6px ${COLORS.cyan}`,
                boxShadow: `0 0 10px rgba(0,255,255,0.18)`,
                letterSpacing: 1,
              }}
            >
              {badge}
            </div>
          ))}
        </div>
      </AbsoluteFill>

      <Scanlines opacity={0.04} />
    </AbsoluteFill>
  );
};
