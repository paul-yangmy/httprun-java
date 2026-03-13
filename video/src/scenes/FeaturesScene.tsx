import React from "react";
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { COLORS, FONTS } from "../constants";
import { MatrixRain } from "../components/MatrixRain";
import { Scanlines } from "../components/Scanlines";
import { NeonCard } from "../components/NeonCard";
import { GradientText } from "../components/GradientText";
import { HudCorners } from "../components/HudCorners";
import { MatrixSideStream } from "../components/MatrixSideStream";
import { StatusBar } from "../components/StatusBar";

const FEATURES = [
  { icon: "⚡", title: "双执行模式", desc: "LOCAL 本地 + SSH 远程，一套 API 统一调度", color: COLORS.green },
  { icon: "🔐", title: "JWT 鉴权", desc: "管理员 / 普通双权限，Token 精细管控", color: COLORS.cyan },
  { icon: "🛡️", title: "安全防护", desc: "命令白名单 + 注入检测 + AES-GCM 加密", color: COLORS.yellow },
  { icon: "⬗", title: "参数模板", desc: "{{.variable}} 语法，动态渲染执行参数", color: COLORS.green },
  { icon: "📊", title: "可观测性", desc: "Prometheus 指标 + Actuator + 健康检查", color: COLORS.cyan },
  { icon: "📋", title: "审计日志", desc: "全量请求 & 执行记录，可追溯可审计", color: COLORS.yellow },
];

export const FeaturesScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const headOpacity = interpolate(frame, [0, fps * 0.4], [0, 1], {
    extrapolateRight: "clamp",
  });
  const headSlide = interpolate(headOpacity, [0, 1], [-40, 0]);

  return (
    <AbsoluteFill style={{ background: COLORS.bg }}>
      <MatrixRain opacity={0.09} />
      <MatrixSideStream opacity={0.45} />
      <HudCorners label="CORE//CAPABILITIES" />
      <StatusBar />

      <AbsoluteFill style={{ padding: "52px 90px" }}>
        {/* Header */}
        <div
          style={{
            opacity: headOpacity,
            transform: `translateX(${headSlide}px)`,
            marginBottom: 36,
          }}
        >
          <div
            style={{
              fontFamily: FONTS.mono,
              fontSize: 17,
              color: COLORS.greenDim,
              letterSpacing: 4,
              marginBottom: 10,
            }}
          >
            {"// CORE_CAPABILITIES.md"}
          </div>
          <GradientText
            text="核心特性"
            fontSize={66}
            colors={["#00FF41", "#00FFFF", "#00FF41"]}
            speed={0.01}
            width={500}
          />
        </div>

        {/* NeonCard 2×3 grid */}
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "1fr 1fr",
            gap: "18px 40px",
          }}
        >
          {FEATURES.map((f, i) => (
            <NeonCard
              key={i}
              icon={f.icon}
              title={f.title}
              desc={f.desc}
              delay={fps * 0.35 + i * fps * 0.16}
              accentColor={f.color}
            />
          ))}
        </div>
      </AbsoluteFill>

      <Scanlines />
    </AbsoluteFill>
  );
};


