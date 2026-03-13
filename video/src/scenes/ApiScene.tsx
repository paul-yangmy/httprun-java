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

export const ApiScene: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const headerOpacity = interpolate(frame, [0, fps * 0.4], [0, 1], {
    extrapolateRight: "clamp",
  });

  return (
    <AbsoluteFill style={{ background: COLORS.bg }}>
      <MatrixRain opacity={0.08} />
      <MatrixSideStream opacity={0.45} />
      <HudCorners label="API//EXAMPLES" />
      <StatusBar />

      <AbsoluteFill style={{ padding: "50px 90px" }}>
        {/* Header */}
        <div style={{ opacity: headerOpacity, marginBottom: 24 }}>
          <div
            style={{
              fontFamily: FONTS.mono,
              fontSize: 17,
              color: COLORS.greenDim,
              letterSpacing: 4,
              marginBottom: 8,
            }}
          >
            {"// API_EXAMPLES.sh"}
          </div>
          <GradientText
            text="常用 API 示例"
            fontSize={64}
            colors={["#00FFFF", "#00FF41", "#FFD700"]}
            speed={0.012}
            width={800}
          />
        </div>

        <div style={{ display: "flex", gap: 30, height: "calc(100% - 140px)" }}>
          {/* API 1 - Create Token */}
          <div style={{ flex: 1 }}>
            <div
              style={{
                fontFamily: FONTS.mono,
                fontSize: 19,
                color: COLORS.cyan,
                marginBottom: 10,
                textShadow: `0 0 8px ${COLORS.cyan}`,
              }}
            >
              ① 创建 API Token（需管理员权限）
            </div>
            <Terminal
              title="create-token"
              startFrame={fps * 0.3}
              charsPerFrame={4}
              lines={[
                {
                  type: "prompt",
                  text: "curl -X POST http://localhost:8081/api/admin/token \\",
                },
                {
                  type: "output",
                  text: '  -H "Authorization: Bearer <admin_jwt>" \\',
                },
                { type: "output", text: '  -H "Content-Type: application/json" \\' },
                { type: "output", text: "  -d '{" },
                { type: "output", text: '    "name": "runner-token",' },
                { type: "output", text: '    "subject": "*",' },
                { type: "output", text: '    "isAdmin": false,' },
                { type: "output", text: '    "expiresIn": 24' },
                { type: "output", text: "  }'" },
                {
                  type: "output",
                  text: '{"token":"eyJhbGciOiJIUzI1NiJ9..."}',
                  color: COLORS.green,
                  delay: 10,
                },
              ]}
              style={{ fontSize: 13 }}
            />
          </div>

          {/* API 2 - Create & Run Command */}
          <div style={{ flex: 1 }}>
            <div
              style={{
                fontFamily: FONTS.mono,
                fontSize: 19,
                color: COLORS.cyan,
                marginBottom: 10,
                textShadow: `0 0 8px ${COLORS.cyan}`,
              }}
            >
              ② 创建并执行命令
            </div>
            <Terminal
              title="create-and-run"
              startFrame={fps * 1.5}
              charsPerFrame={4}
              lines={[
                {
                  type: "prompt",
                  text: "curl -X POST .../api/admin/command \\",
                },
                { type: "output", text: "  -d '{" },
                { type: "output", text: '    "name": "hello",' },
                {
                  type: "output",
                  text: '    "commandTemplate": "echo Hello, {{.name}}!",' ,
                },
                {
                  type: "output",
                  text: '    "executionMode": "LOCAL"',
                },
                { type: "output", text: "  }'" },
                { type: "blank", text: "" },
                {
                  type: "prompt",
                  text: "curl -X POST .../api/run/hello \\",
                },
                { type: "output", text: '  -H "Authorization: Bearer <api_token>" \\' },
                {
                  type: "output",
                  text: "  -d '{\"params\":{\"name\":\"World\"},\"async\":false}'",
                },
                {
                  type: "output",
                  text: '{"output":"Hello, World!","exitCode":0}',
                  color: COLORS.green,
                  delay: 10,
                },
              ]}
              style={{ fontSize: 13 }}
            />
          </div>
        </div>
      </AbsoluteFill>

      <Scanlines />
    </AbsoluteFill>
  );
};
