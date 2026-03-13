import { Composition } from "remotion";
import { HackerDeployVideo } from "./HackerDeployVideo";
import { TOTAL_DURATION, FPS } from "./constants";

export const RemotionRoot = () => {
  return (
    <Composition
      id="HackerDeployVideo"
      component={HackerDeployVideo}
      durationInFrames={TOTAL_DURATION}
      fps={FPS}
      width={1920}
      height={1080}
    />
  );
};
