import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { COLORS, FONTS } from "../constants";
import { MatrixRain } from "../components/MatrixRain";
import { Scanlines } from "../components/Scanlines";
import { Terminal } from "../components/Terminal";
import { ProgressBar } from "../components/ProgressBar";
import { GradientText } from "../components/GradientText";
import { HudCorners } from "../components/HudCorners";
import { MatrixSideStream } from "../components/MatrixSideStream";
import { StatusBar } from "../components/StatusBar";

export const BuildScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const headerOpacity = interpolate(frame, [0, fps * 0.4], [0, 1], {
    extrapolateRight: "clamp",
  });

  const stepsOpacity = interpolate(frame, [fps * 0.3, fps * 0.8], [0, 1], {
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill style={{ background: COLORS.bg }}>
      <MatrixRain opacity={0.09} />
      <MatrixSideStream opacity={0.45} />
      <HudCorners label="BUILD//PROCESS" />
      <StatusBar />

      <AbsoluteFill style={{ padding: "50px 90px" }}>
        {/* Header */}
        <div style={{ opacity: headerOpacity, marginBottom: 28 }}>
          <div
            style={{
              fontFamily: FONTS.mono,
              fontSize: 17,
              color: COLORS.greenDim,
              letterSpacing: 4,
              marginBottom: 8,
            }}
          >
            {"// BUILD_PROCESS.sh"}
          </div>
          <GradientText
            text="构建项目"
            fontSize={62}
            colors={["#00FF41", "#FFD700", "#00FF41"]}
            speed={0.012}
            width={440}
          />
        </div>

        <div style={{ display: "flex", gap: 40, alignItems: "flex-start" }}>
          {/* Left - Build steps with progress bars */}
          <div style={{ width: 360, opacity: stepsOpacity }}>
            <div
              style={{
                fontFamily: FONTS.mono,
                fontSize: 15,
                color: COLORS.grayLight,
                marginBottom: 22,
                lineHeight: 1.6,
              }}
            >
              Maven 自动化构建流程:
            </div>
            <ProgressBar label="安装 Node.js & npm (v24)" startFrame={fps * 0.5} duration={fps * 0.9} />
            <ProgressBar label="安装前端依赖 (npm install)" startFrame={fps * 1.4} duration={fps * 0.8} color={COLORS.cyan} />
            <ProgressBar label="构建前端 (Vite → dist/)" startFrame={fps * 2.2} duration={fps * 1.0} color={COLORS.cyan} />
            <ProgressBar label="复制静态资源 → classes/static/" startFrame={fps * 3.2} duration={fps * 0.6} />
            <ProgressBar label="编译 Java → Fat Jar" startFrame={fps * 3.8} duration={fps * 1.2} color={COLORS.yellow} />

            {/* Build complete indicator */}
            {frame > fps * 5.2 && (
              <div
                style={{
                  marginTop: 20,
                  fontFamily: FONTS.mono,
                  fontSize: 18,
                  color: COLORS.green,
                  textShadow: `0 0 12px ${COLORS.green}`,
                  letterSpacing: 2,
                  opacity: interpolate(frame, [fps * 5.2, fps * 5.6], [0, 1], { extrapolateRight: "clamp" }),
                }}
              >
                ✓ BUILD SUCCESS
              </div>
            )}
          </div>

          {/* Right - Terminal */}
          <div style={{ flex: 1 }}>
            <Terminal
              title="Maven Build"
              startFrame={fps * 0.5}
              charsPerFrame={3}
              lines={[
                { type: "comment", text: "# 完整构建（推荐）- 包含前后端" },
                { type: "prompt", text: "mvn clean package -DskipTests" },
                { type: "output", text: "[INFO] BUILD SUCCESS", color: COLORS.green, delay: 8 },
                { type: "blank", text: "" },
                { type: "comment", text: "# 仅构建后端（跳过前端）" },
                { type: "prompt", text: "mvn clean package -DskipTests -DskipFrontend=true" },
                { type: "output", text: "[INFO] BUILD SUCCESS", color: COLORS.green, delay: 8 },
                { type: "blank", text: "" },
                { type: "comment", text: "# 构建产物" },
                { type: "output", text: "target/httprun-java-1.0.0.jar  ✓  完整应用" },
              ]}
              style={{
                fontSize: 16,
              }}
            />
          </div>
        </div>
      </AbsoluteFill>

      <Scanlines />
    </AbsoluteFill>
  );
};
