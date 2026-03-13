import React, { useMemo } from "react";
import { useCurrentFrame, interpolate, Easing } from "remotion";

type Particle = {
  angle: number;
  speed: number;
  size: number;
  color: string;
  delay: number;
};

type Props = {
  /** Center position in px */
  x: number;
  y: number;
  /** Frame at which the burst triggers */
  triggerFrame?: number;
  count?: number;
  /** Duration of each particle in frames */
  duration?: number;
  colors?: string[];
};

export const ParticleBurst: React.FC<Props> = ({
  x,
  y,
  triggerFrame = 0,
  count = 36,
  duration = 45,
  colors = ["#00FF41", "#00FFFF", "#FFFFFF", "#FFD700"],
}) => {
  const frame = useCurrentFrame();

  const particles = useMemo<Particle[]>(
    () =>
      Array.from({ length: count }, (_, i) => ({
        angle: (i / count) * Math.PI * 2 + Math.sin(i * 1.7) * 0.4,
        speed: 60 + Math.abs(Math.sin(i * 3.1)) * 220,
        size: 2 + Math.abs(Math.sin(i * 7.3)) * 5,
        color: colors[i % colors.length],
        delay: Math.floor(Math.abs(Math.sin(i * 5.3)) * 5),
      })),
    [count, colors]
  );

  const elapsed = frame - triggerFrame;
  if (elapsed < 0) return null;

  return (
    <svg
      style={{
        position: "absolute",
        top: 0,
        left: 0,
        width: "100%",
        height: "100%",
        pointerEvents: "none",
        overflow: "visible",
      }}
    >
      {particles.map((p, i) => {
        const pElapsed = Math.max(0, elapsed - p.delay);
        const progress = interpolate(pElapsed, [0, duration], [0, 1], {
          extrapolateRight: "clamp",
          extrapolateLeft: "clamp",
          easing: Easing.out(Easing.cubic),
        });
        const opacity = interpolate(pElapsed, [0, duration * 0.25, duration], [0, 1, 0], {
          extrapolateRight: "clamp",
          extrapolateLeft: "clamp",
        });
        const px = x + Math.cos(p.angle) * p.speed * progress;
        const py = y + Math.sin(p.angle) * p.speed * progress;
        const r = p.size * (1 - progress * 0.6);

        return (
          <circle
            key={i}
            cx={px}
            cy={py}
            r={Math.max(0.5, r)}
            fill={p.color}
            opacity={opacity}
            style={{ filter: `drop-shadow(0 0 ${r * 1.5}px ${p.color})` }}
          />
        );
      })}
    </svg>
  );
};
