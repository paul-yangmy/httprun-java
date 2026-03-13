import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { COLORS, FONTS } from "../constants";
import { MatrixRain } from "../components/MatrixRain";
import { Scanlines } from "../components/Scanlines";
import { Terminal } from "../components/Terminal";
import { GradientText } from "../components/GradientText";
import { ParticleBurst } from "../components/ParticleBurst";
import { HudCorners } from "../components/HudCorners";
import { MatrixSideStream } from "../components/MatrixSideStream";
import { StatusBar } from "../components/StatusBar";

export const DeployScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const headerOpacity = interpolate(frame, [0, fps * 0.4], [0, 1], {
    extrapolateRight: "clamp",
  });

  // Success flash: fires around frame fps*5.5 (when server starts)
  const SUCCESS_FRAME = Math.floor(fps * 5.8);
  const flashOpacity = interpolate(
    frame,
    [SUCCESS_FRAME, SUCCESS_FRAME + 4, SUCCESS_FRAME + 18],
    [0, 0.55, 0],
    { extrapolateRight: "clamp", extrapolateLeft: "clamp" }
  );

  // Pulsing "ONLINE" dot after server starts
  const onlinePulse = frame > SUCCESS_FRAME
    ? 0.5 + Math.sin((frame - SUCCESS_FRAME) * 0.18) * 0.5
    : 0;

  return (
    <AbsoluteFill style={{ background: COLORS.bg }}>
      <MatrixRain opacity={0.09} />
      <MatrixSideStream opacity={0.45} />
      <HudCorners label="DEPLOY//PRODUCTION" showCoords />
      <StatusBar />

      {/* Green success flash */}
      {flashOpacity > 0 && (
        <div
          style={{
            position: "absolute",
            inset: 0,
            background: `rgba(0,255,65,${flashOpacity})`,
            pointerEvents: "none",
            zIndex: 10,
          }}
        />
      )}

      {/* Particle burst on server start */}
      <ParticleBurst
        x={960}
        y={580}
        triggerFrame={SUCCESS_FRAME}
        count={30}
        duration={40}
        colors={["#00FF41", "#FFFFFF", "#00FFFF"]}
      />

      <AbsoluteFill style={{ padding: "50px 80px" }}>
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
            {"// DEPLOY_PRODUCTION.sh"}
          </div>
          <GradientText
            text="生产部署（Jar）"
            fontSize={62}
            colors={["#FF0040", "#FFD700", "#00FF41"]}
            speed={0.01}
            width={600}
          />
        </div>

        <div style={{ display: "flex", gap: 40 }}>
          {/* Step 1 - Init Token */}
          <div style={{ flex: 1 }}>
            <div
              style={{
                fontFamily: FONTS.mono,
                fontSize: 20,
                color: COLORS.cyan,
                marginBottom: 12,
                textShadow: `0 0 8px ${COLORS.cyan}`,
              }}
            >
              STEP 1: 首次启动（初始化管理员 Token）
            </div>
            <Terminal
              title="init-admin-token"
              startFrame={fps * 0.4}
              charsPerFrame={3}
              lines={[
                { type: "prompt", text: "java \\" },
                { type: "output", text: "  -DSPRING_PROFILES_ACTIVE=prod \\" },
                { type: "output", text: "  -DDB_HOST=your-pg-host \\" },
                { type: "output", text: "  -DDB_PORT=5432 \\" },
                { type: "output", text: "  -DDB_USER=httprun \\" },
                { type: "output", text: "  -DDB_PASSWORD=your-db-password \\" },
                { type: "output", text: "  -DJWT_SECRET=your-32+chars-secret \\" },
                { type: "output", text: "  -jar target/httprun-java-1.0.0.jar \\" },
                {
                  type: "output",
                  text: "  --httprun.init-admin-token=true",
                  color: COLORS.yellow,
                },
                { type: "blank", text: "" },
                {
                  type: "output",
                  text: ">>> ADMIN TOKEN: eyJhbGciOi...<SAVE THIS!>",
                  color: COLORS.red,
                  delay: 12,
                },
              ]}
              style={{ fontSize: 14 }}
            />
          </div>

          {/* Step 2 - Normal start */}
          <div style={{ flex: 1 }}>
            <div
              style={{
                fontFamily: FONTS.mono,
                fontSize: 20,
                color: COLORS.cyan,
                marginBottom: 12,
                textShadow: `0 0 8px ${COLORS.cyan}`,
              }}
            >
              STEP 2: 正式启动
            </div>
            <Terminal
              title="production-start"
              startFrame={fps * 2.5}
              charsPerFrame={3}
              lines={[
                { type: "prompt", text: "java \\" },
                { type: "output", text: "  -DSPRING_PROFILES_ACTIVE=prod \\" },
                { type: "output", text: "  -DDB_HOST=your-pg-host \\" },
                { type: "output", text: "  -DDB_PORT=5432 \\" },
                { type: "output", text: "  -DDB_USER=httprun \\" },
                { type: "output", text: "  -DDB_PASSWORD=your-db-password \\" },
                { type: "output", text: "  -DJWT_SECRET=your-32+chars-secret \\" },
                { type: "output", text: "  -jar target/httprun-java-1.0.0.jar" },
                { type: "blank", text: "" },
                {
                  type: "output",
                  text: "✓ Listening on port 8081",
                  color: COLORS.green,
                  delay: 12,
                },
              ]}
              style={{ fontSize: 14 }}
            />
          </div>
        </div>

        {/* ONLINE status indicator */}
        {onlinePulse > 0 && (
          <div
            style={{
              marginTop: 28,
              display: "flex",
              alignItems: "center",
              gap: 14,
              opacity: Math.min(1, onlinePulse * 2),
            }}
          >
            <div
              style={{
                width: 14,
                height: 14,
                borderRadius: "50%",
                background: COLORS.green,
                boxShadow: `0 0 ${12 * onlinePulse}px ${COLORS.green}`,
              }}
            />
            <span
              style={{
                fontFamily: FONTS.mono,
                fontSize: 28,
                color: COLORS.green,
                textShadow: `0 0 12px ${COLORS.green}`,
                letterSpacing: 4,
              }}
            >
              SERVER ONLINE — :8081
            </span>
          </div>
        )}
      </AbsoluteFill>

      <Scanlines />
    </AbsoluteFill>
  );
};
