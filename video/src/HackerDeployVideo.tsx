import React from "react";
import { AbsoluteFill } from "remotion";
import { TransitionSeries, linearTiming, springTiming } from "@remotion/transitions";
import { slide } from "@remotion/transitions/slide";
import { wipe } from "@remotion/transitions/wipe";
import { fade } from "@remotion/transitions/fade";
import { COLORS } from "./constants";
import { TitleScene } from "./scenes/TitleScene";
import { OverviewScene } from "./scenes/OverviewScene";
import { FeaturesScene } from "./scenes/FeaturesScene";
import { BuildScene } from "./scenes/BuildScene";
import { DeployScene } from "./scenes/DeployScene";
import { DockerScene } from "./scenes/DockerScene";
import { ApiScene } from "./scenes/ApiScene";
import { OutroScene } from "./scenes/OutroScene";

const T = 20; // transition duration in frames

export const HackerDeployVideo: React.FC = () => {
  return (
    <AbsoluteFill style={{ background: COLORS.bg }}>
      <TransitionSeries>
        {/* 1. Title — 4s */}
        <TransitionSeries.Sequence durationInFrames={120}>
          <TitleScene />
        </TransitionSeries.Sequence>

        <TransitionSeries.Transition
          presentation={slide({ direction: "from-right" })}
          timing={springTiming({ durationInFrames: T, config: { damping: 200 } })}
        />

        {/* 2. Overview — 5s */}
        <TransitionSeries.Sequence durationInFrames={150}>
          <OverviewScene />
        </TransitionSeries.Sequence>

        <TransitionSeries.Transition
          presentation={wipe({ direction: "from-left" })}
          timing={linearTiming({ durationInFrames: T })}
        />

        {/* 3. Features — 5s */}
        <TransitionSeries.Sequence durationInFrames={150}>
          <FeaturesScene />
        </TransitionSeries.Sequence>

        <TransitionSeries.Transition
          presentation={slide({ direction: "from-bottom" })}
          timing={springTiming({ durationInFrames: T, config: { damping: 200 } })}
        />

        {/* 4. Build — 7s */}
        <TransitionSeries.Sequence durationInFrames={210}>
          <BuildScene />
        </TransitionSeries.Sequence>

        <TransitionSeries.Transition
          presentation={wipe({ direction: "from-right" })}
          timing={linearTiming({ durationInFrames: T })}
        />

        {/* 5. Deploy — 8s */}
        <TransitionSeries.Sequence durationInFrames={240}>
          <DeployScene />
        </TransitionSeries.Sequence>

        <TransitionSeries.Transition
          presentation={fade()}
          timing={linearTiming({ durationInFrames: T })}
        />

        {/* 6. Docker — 5s */}
        <TransitionSeries.Sequence durationInFrames={150}>
          <DockerScene />
        </TransitionSeries.Sequence>

        <TransitionSeries.Transition
          presentation={slide({ direction: "from-left" })}
          timing={springTiming({ durationInFrames: T, config: { damping: 200 } })}
        />

        {/* 7. API Examples — 6s */}
        <TransitionSeries.Sequence durationInFrames={180}>
          <ApiScene />
        </TransitionSeries.Sequence>

        <TransitionSeries.Transition
          presentation={wipe({ direction: "from-bottom" })}
          timing={linearTiming({ durationInFrames: T })}
        />

        {/* 8. Outro — 4s */}
        <TransitionSeries.Sequence durationInFrames={120}>
          <OutroScene />
        </TransitionSeries.Sequence>
      </TransitionSeries>
    </AbsoluteFill>
  );
};
