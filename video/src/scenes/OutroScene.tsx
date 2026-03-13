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

const ACCESS_LINKS = [
  { label: "管理界面", url: "http://localhost:8081/admin", icon: "⬡" },
  { label: "API 文档", url: "http://localhost:8081/swagger/index.html", icon: "📄" },
  { label: "健康检查", url: "http://localhost:8081/api/health", icon: "💚" },
  { label: "Prometheus", url: "http://localhost:8081/actuator/prometheus", icon: "📊" },
];

const SECURITY_TIPS = [
  "生产环境必须修改 JWT_SECRET",
  "优先使用 HTTPS 加密传输",
  "配置 IP 白名单与限流策略",
  "定期轮换 Token 防止泄漏",
  "避免在日志中输出敏感凭据",
];

export const OutroScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const titleProgress = spring({
    frame,
    fps,
    config: { damping: 14, stiffness: 70 },
    durationInFrames: fps * 1.2,
  });

  const titleOpacity = interpolate(frame, [0, fps * 0.4], [0, 1], {
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill style={{ background: COLORS.bg }}>
      <MatrixRain opacity={0.18} />
      <MatrixSideStream opacity={0.55} />
      <HudCorners label="SYSTEM//READY" showCoords />
      <StatusBar />

      {/* Particle celebration at scene start */}
      <ParticleBurst x={960} y={120} triggerFrame={8} count={40} duration={55} colors={["#00FF41", "#FFD700", "#00FFFF", "#FF0040"]} />
      <ParticleBurst x={300} y={540} triggerFrame={12} count={24} duration={48} colors={["#00FFFF", "#00FF41"]} />
      <ParticleBurst x={1620} y={540} triggerFrame={12} count={24} duration={48} colors={["#FFD700", "#00FF41"]} />

      <AbsoluteFill style={{ padding: "50px 80px" }}>
        {/* Header */}
        <div
          style={{
            opacity: titleOpacity,
            transform: `translateY(${interpolate(titleProgress, [0, 1], [30, 0])}px)`,
            marginBottom: 32,
          }}
        >
          <div
            style={{
              fontFamily: FONTS.mono,
              fontSize: 17,
              color: COLORS.greenDim,
              letterSpacing: 4,
              marginBottom: 8,
            }}
          >
            {"// ACCESS_URLS.md"}
          </div>
          <GradientText
            text="快速访问"
            fontSize={66}
            colors={["#00FF41", "#00FFFF", "#FFD700"]}
            speed={0.015}
            width={480}
          />
        </div>

        <div style={{ display: "flex", gap: 50 }}>
          {/* Access links */}
          <div style={{ flex: 1 }}>
            {ACCESS_LINKS.map((link, i) => {
              const delay = fps * 0.3 + i * fps * 0.2;
              const progress = spring({
                frame: frame - delay,
                fps,
                config: { damping: 16 },
                durationInFrames: fps * 0.8,
              });
              return (
                <div
                  key={i}
                  style={{
                    opacity: Math.min(1, progress * 2),
                    transform: `translateX(${interpolate(progress, [0, 1], [-30, 0])}px)`,
                    display: "flex",
                    alignItems: "center",
                    gap: 16,
                    marginBottom: 20,
                    padding: "14px 20px",
                    background: "rgba(0,255,65,0.04)",
                    border: `1px solid rgba(0,255,65,${0.08 + i * 0.05})`,
                    borderLeft: `3px solid ${COLORS.green}`,
                    borderRadius: 2,
                  }}
                >
                  <span style={{ fontSize: 26 }}>{link.icon}</span>
                  <div>
                    <div
                      style={{
                        fontFamily: FONTS.mono,
                        fontSize: 22,
                        color: COLORS.cyan,
                        fontWeight: "bold",
                        textShadow: `0 0 8px ${COLORS.cyan}`,
                        marginBottom: 4,
                      }}
                    >
                      {link.label}
                    </div>
                    <div
                      style={{
                        fontFamily: FONTS.mono,
                        fontSize: 18,
                        color: COLORS.green,
                        textShadow: `0 0 6px ${COLORS.greenDark}`,
                      }}
                    >
                      {link.url}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Security tips */}
          <div style={{ width: 400 }}>
            <div
              style={{
                fontFamily: FONTS.mono,
                fontSize: 24,
                color: COLORS.red,
                textShadow: `0 0 10px ${COLORS.red}`,
                marginBottom: 16,
                fontWeight: "bold",
              }}
            >
              🛡️ 安全建议
            </div>
            <div
              style={{
                background: "rgba(255,0,64,0.03)",
                border: `1px solid rgba(255,0,64,0.2)`,
                borderRadius: 4,
                padding: "16px 20px",
              }}
            >
              {SECURITY_TIPS.map((tip, i) => {
                const delay = fps * 0.6 + i * fps * 0.18;
                const opacity = interpolate(frame, [delay, delay + fps * 0.25], [0, 1], {
                  extrapolateRight: "clamp",
                  extrapolateLeft: "clamp",
                });
                return (
                  <div
                    key={i}
                    style={{
                      opacity,
                      fontFamily: FONTS.mono,
                    fontSize: 18,
                      color: COLORS.grayLight,
                      marginBottom: 10,
                      display: "flex",
                      gap: 10,
                    }}
                  >
                    <span style={{ color: COLORS.red, flexShrink: 0 }}>▶</span>
                    <span>{tip}</span>
                  </div>
                );
              })}
            </div>

            {/* License and footer */}
            <div
              style={{
                marginTop: 24,
                fontFamily: FONTS.mono,
                fontSize: 13,
                color: COLORS.grayLight,
                textAlign: "center",
                lineHeight: 1.8,
              }}
            >
              <div
                style={{
                  opacity: interpolate(frame, [fps * 2.5, fps * 3], [0, 1], {
                    extrapolateRight: "clamp",
                  }),
                }}
              >
                <div style={{ color: COLORS.greenDim, marginBottom: 4 }}>[ MIT LICENSE ]</div>
                <GlitchText
                  text="github.com/raojinlin/httprun-java"
                  style={{
                    display: "block",
                    color: COLORS.green,
                    fontSize: 14,
                    textShadow: `0 0 8px ${COLORS.green}`,
                  }}
                  intensity={0.5}
                />
              </div>
            </div>
          </div>
        </div>
      </AbsoluteFill>

      <Scanlines />
    </AbsoluteFill>
  );
};
