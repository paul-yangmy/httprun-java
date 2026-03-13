import React from "react";
import { useCurrentFrame, useVideoConfig, interpolate } from "remotion";
import { COLORS } from "../constants";

export const Scanlines: React.FC<{ opacity?: number }> = ({ opacity = 0.06 }) => {
  const { width, height } = useVideoConfig();
  const frame = useCurrentFrame();

  // Two scan beams at different speeds
  const scanY1 = (frame * 2.5) % height;
  const scanY2 = (frame * 1.1 + height * 0.55) % height;

  // Occasional glitch flicker (random-looking but deterministic)
  const glitchActive = frame % 47 < 2;
  const glitchY = (frame * 31) % (height - 60);
  const glitchH = 4 + (frame % 13);
  const glitchShift = (frame % 7) - 3;

  return (
    <div
      style={{
        position: "absolute",
        top: 0,
        left: 0,
        width,
        height,
        pointerEvents: "none",
        overflow: "hidden",
        zIndex: 40,
      }}
    >
      {/* CRT scanlines grid */}
      <div
        style={{
          position: "absolute",
          inset: 0,
          backgroundImage: `repeating-linear-gradient(
            to bottom,
            transparent 0px,
            transparent 2px,
            rgba(0,0,0,${opacity * 4}) 2px,
            rgba(0,0,0,${opacity * 4}) 3px
          )`,
        }}
      />

      {/* Primary scan beam */}
      <div
        style={{
          position: "absolute",
          left: 0,
          top: scanY1,
          width: "100%",
          height: 80,
          background: `linear-gradient(to bottom,
            transparent 0%,
            rgba(0,255,65,0.04) 30%,
            rgba(0,255,65,0.09) 50%,
            rgba(0,255,65,0.04) 70%,
            transparent 100%)`,
        }}
      />

      {/* Secondary scan beam */}
      <div
        style={{
          position: "absolute",
          left: 0,
          top: scanY2,
          width: "100%",
          height: 40,
          background: `linear-gradient(to bottom,
            transparent,
            rgba(0,200,255,0.03) 50%,
            transparent)`,
        }}
      />

      {/* Glitch bar */}
      {glitchActive && (
        <div
          style={{
            position: "absolute",
            left: 0,
            top: glitchY,
            width: "100%",
            height: glitchH,
            background: `rgba(0,255,65,0.18)`,
            transform: `translateX(${glitchShift}px)`,
            mixBlendMode: "screen",
          }}
        />
      )}

      {/* Vignette */}
      <div
        style={{
          position: "absolute",
          inset: 0,
          background: `radial-gradient(ellipse at center, transparent 55%, rgba(0,0,0,0.55) 100%)`,
        }}
      />
    </div>
  );
};
