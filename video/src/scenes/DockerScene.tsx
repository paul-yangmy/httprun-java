import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { COLORS, FONTS } from "../constants";
import { MatrixRain } from "../components/MatrixRain";
import { Scanlines } from "../components/Scanlines";
import { Terminal } from "../components/Terminal";
import { HudCorners } from "../components/HudCorners";
import { MatrixSideStream } from "../components/MatrixSideStream";
import { StatusBar } from "../components/StatusBar";
import { GradientText } from "../components/GradientText";

export const DockerScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const headerOpacity = interpolate(frame, [0, fps * 0.4], [0, 1], {
    extrapolateRight: "clamp",
  });

  const envOpacity = interpolate(frame, [fps * 0.5, fps * 1], [0, 1], {
    extrapolateRight: "clamp",
  });

  const ENV_VARS = [
    { name: "SPRING_PROFILES_ACTIVE", req: false, desc: "运行环境（默认 dev）" },
    { name: "DB_HOST", req: false, desc: "PostgreSQL 主机" },
    { name: "DB_PORT", req: false, desc: "端口（默认 5432）" },
    { name: "DB_USER", req: false, desc: "用户名（默认 httprun）" },
    { name: "DB_PASSWORD", req: false, desc: "数据库密码" },
    { name: "JWT_SECRET", req: true, desc: "JWT 密钥（至少 32 字符）" },
    { name: "INIT_ADMIN_TOKEN", req: false, desc: "初始化管理员 Token" },
  ];

  return (
    <AbsoluteFill style={{ background: COLORS.bg }}>
      <MatrixRain opacity={0.09} />
      <MatrixSideStream opacity={0.45} />
      <HudCorners label="DOCKER//DEPLOY" />
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
            {"// DOCKER_DEPLOY.sh"}
          </div>
          <GradientText
            text="Docker 部署"
            fontSize={64}
            colors={["#00FF41", "#00FFFF", "#FFD700"]}
            speed={0.012}
            width={680}
          />
        </div>

        <div style={{ display: "flex", gap: 40 }}>
          {/* Left - Terminal */}
          <div style={{ flex: 1 }}>
            <Terminal
              title="docker-compose"
              startFrame={fps * 0.4}
              charsPerFrame={3}
              lines={[
                { type: "comment", text: "# 一键启动所有服务" },
                { type: "prompt", text: "docker-compose up -d" },
                { type: "output", text: "Creating network httprun_default", color: COLORS.cyan, delay: 6 },
                { type: "output", text: "Creating httprun-db  ... done", color: COLORS.green, delay: 4 },
                { type: "output", text: "Creating httprun-app ... done", color: COLORS.green, delay: 4 },
                { type: "blank", text: "" },
                { type: "comment", text: "# 查看实时日志" },
                { type: "prompt", text: "docker-compose logs -f httprun" },
                { type: "blank", text: "" },
                { type: "comment", text: "# 健康检查" },
                {
                  type: "prompt",
                  text: "curl http://localhost:8081/api/health",
                },
                {
                  type: "output",
                  text: '{"status":"UP","version":"1.0.0"}',
                  color: COLORS.green,
                  delay: 8,
                },
              ]}
              style={{ fontSize: 15 }}
            />
          </div>

          {/* Right - Env vars table */}
          <div style={{ width: 480, opacity: envOpacity }}>
            <div
              style={{
                fontFamily: FONTS.mono,
                fontSize: 18,
                color: COLORS.cyan,
                marginBottom: 14,
                textShadow: `0 0 8px ${COLORS.cyan}`,
              }}
            >
              关键环境变量
            </div>
            <div
              style={{
                background: "rgba(0,255,65,0.03)",
                border: `1px solid ${COLORS.greenDark}`,
                borderRadius: 4,
                overflow: "hidden",
              }}
            >
              {/* Header row */}
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "16px 1fr 2fr",
                  padding: "10px 16px",
                  borderBottom: `1px solid ${COLORS.greenDark}`,
                  background: "rgba(0,255,65,0.07)",
                  fontFamily: FONTS.mono,
                  fontSize: 15,
                  color: COLORS.grayLight,
                  gap: 8,
                }}
              >
                <span></span>
                <span>变量名</span>
                <span>说明</span>
              </div>
              {ENV_VARS.map((v, i) => {
                const delay = fps * 0.8 + i * fps * 0.1;
                const opacity = interpolate(frame, [delay, delay + fps * 0.2], [0, 1], {
                  extrapolateRight: "clamp",
                  extrapolateLeft: "clamp",
                });
                return (
                  <div
                    key={i}
                    style={{
                      opacity,
                      display: "grid",
                      gridTemplateColumns: "16px 1fr 2fr",
                      padding: "10px 16px",
                      borderBottom:
                        i < ENV_VARS.length - 1 ? `1px solid rgba(0,255,65,0.07)` : "none",
                      fontFamily: FONTS.mono,
                      fontSize: 15,
                      gap: 8,
                      alignItems: "center",
                    }}
                  >
                    <span
                      style={{
                        color: v.req ? COLORS.red : COLORS.grayLight,
                        textShadow: v.req ? `0 0 6px ${COLORS.red}` : "none",
                        fontSize: 10,
                      }}
                    >
                      {v.req ? "★" : "○"}
                    </span>
                    <span
                      style={{
                        color: COLORS.yellow,
                        wordBreak: "break-all",
                        fontSize: 12,
                      }}
                    >
                      {v.name}
                    </span>
                    <span style={{ color: COLORS.grayLight }}>{v.desc}</span>
                  </div>
                );
              })}
            </div>
            <div
              style={{
                fontFamily: FONTS.mono,
                fontSize: 12,
                color: COLORS.red,
                marginTop: 8,
                textShadow: `0 0 4px ${COLORS.red}`,
              }}
            >
              ★ 生产环境必填
            </div>
          </div>
        </div>
      </AbsoluteFill>

      <Scanlines />
    </AbsoluteFill>
  );
};
