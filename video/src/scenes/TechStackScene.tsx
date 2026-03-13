import React from "react";
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";
import { COLORS, FONTS } from "../constants";
import { MatrixRain } from "../components/MatrixRain";
import { Scanlines } from "../components/Scanlines";

const TECH_STACK = [
  { name: "Java 17", version: "LTS", color: COLORS.yellow },
  { name: "Spring Boot", version: "3.2.4", color: COLORS.green },
  { name: "Spring Security", version: "JWT + RBAC", color: COLORS.cyan },
  { name: "Spring Data JPA", version: "ORM Layer", color: COLORS.green },
  { name: "Spring WebSocket", version: "Real-time", color: COLORS.cyanDim },
  { name: "PostgreSQL", version: "16 Production", color: COLORS.cyan },
  { name: "SQLite", version: "Development", color: COLORS.grayLight },
  { name: "Flyway", version: "DB Migration", color: COLORS.yellow },
  { name: "Caffeine", version: "Local Cache", color: COLORS.green },
  { name: "Springdoc OpenAPI", version: "API Docs", color: COLORS.cyan },
];

const ARC = [
  "REST API Gateway",
  "  └── Authentication (JWT)",
  "  └── Command Registry",
  "       └── LOCAL Executor",
  "       └── SSH Executor",
  "            └── Connection Pool",
  "  └── Audit Logger",
  "  └── Prometheus Metrics",
];

export const TechStackScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const headerOpacity = interpolate(frame, [0, fps * 0.3], [0, 1], {
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill style={{ background: COLORS.bg }}>
      <MatrixRain opacity={0.1} />

      <AbsoluteFill style={{ padding: "60px 80px", display: "flex", gap: 60 }}>
        {/* Left - Tech stack list */}
        <div style={{ flex: 1 }}>
          <div style={{ opacity: headerOpacity, marginBottom: 32 }}>
            <div
              style={{
                fontFamily: FONTS.mono,
                fontSize: 13,
                color: COLORS.greenDim,
                letterSpacing: 4,
                marginBottom: 8,
              }}
            >
              {"// TECH_STACK.json"}
            </div>
            <div
              style={{
                fontFamily: FONTS.mono,
                fontSize: 40,
                fontWeight: "bold",
                color: COLORS.green,
                textShadow: `0 0 16px ${COLORS.green}`,
                borderBottom: `1px solid ${COLORS.greenDark}`,
                paddingBottom: 16,
              }}
            >
              技术栈
            </div>
          </div>

          {TECH_STACK.map((tech, i) => {
            const delay = fps * 0.3 + i * fps * 0.1;
            const progress = interpolate(frame, [delay, delay + fps * 0.3], [0, 1], {
              extrapolateRight: "clamp",
              extrapolateLeft: "clamp",
            });
            const bar = interpolate(progress, [0, 1], [0, 100]);

            return (
              <div
                key={i}
                style={{
                  opacity: progress,
                  marginBottom: 10,
                  display: "flex",
                  alignItems: "center",
                  gap: 12,
                }}
              >
                <div
                  style={{
                    fontFamily: FONTS.mono,
                    fontSize: 17,
                    color: tech.color,
                    width: 220,
                    flexShrink: 0,
                    textShadow: `0 0 6px ${tech.color}`,
                  }}
                >
                  {tech.name}
                </div>
                <div
                  style={{
                    flex: 1,
                    height: 4,
                    background: COLORS.gray,
                    borderRadius: 2,
                    overflow: "hidden",
                  }}
                >
                  <div
                    style={{
                      width: `${bar}%`,
                      height: "100%",
                      background: tech.color,
                      boxShadow: `0 0 8px ${tech.color}`,
                      borderRadius: 2,
                    }}
                  />
                </div>
                <div
                  style={{
                    fontFamily: FONTS.mono,
                    fontSize: 13,
                    color: COLORS.grayLight,
                    width: 150,
                    flexShrink: 0,
                    textAlign: "right",
                  }}
                >
                  {tech.version}
                </div>
              </div>
            );
          })}
        </div>

        {/* Right - Architecture tree */}
        <div style={{ width: 340 }}>
          <div style={{ opacity: headerOpacity, marginBottom: 32 }}>
            <div
              style={{
                fontFamily: FONTS.mono,
                fontSize: 13,
                color: COLORS.greenDim,
                letterSpacing: 4,
                marginBottom: 8,
              }}
            >
              {"// ARCHITECTURE"}
            </div>
            <div
              style={{
                fontFamily: FONTS.mono,
                fontSize: 28,
                fontWeight: "bold",
                color: COLORS.cyan,
                textShadow: `0 0 12px ${COLORS.cyan}`,
                borderBottom: `1px solid ${COLORS.greenDark}`,
                paddingBottom: 16,
              }}
            >
              架构概览
            </div>
          </div>

          <div
            style={{
              background: "rgba(0,255,65,0.03)",
              border: `1px solid ${COLORS.greenDark}`,
              borderRadius: 4,
              padding: "20px 24px",
              fontFamily: FONTS.mono,
              fontSize: 15,
              lineHeight: 2,
            }}
          >
            {ARC.map((line, i) => {
              const delay = fps * 0.4 + i * fps * 0.15;
              const opacity = interpolate(frame, [delay, delay + fps * 0.3], [0, 1], {
                extrapolateRight: "clamp",
                extrapolateLeft: "clamp",
              });
              const isRoot = i === 0;
              return (
                <div
                  key={i}
                  style={{
                    opacity,
                    color: isRoot ? COLORS.yellow : line.includes("Executor") || line.includes("Pool")
                      ? COLORS.cyan
                      : COLORS.green,
                    textShadow: isRoot
                      ? `0 0 8px ${COLORS.yellow}`
                      : `0 0 4px ${COLORS.greenDark}`,
                  }}
                >
                  {line}
                </div>
              );
            })}
          </div>
        </div>
      </AbsoluteFill>

      <Scanlines />
    </AbsoluteFill>
  );
};
