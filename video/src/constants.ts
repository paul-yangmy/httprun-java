// Hacker theme constants
export const COLORS = {
  bg: "#000000",
  bgAlt: "#0a0a0a",
  green: "#00FF41",
  greenDim: "#00CC33",
  greenDark: "#004400",
  cyan: "#00FFFF",
  cyanDim: "#00CCCC",
  red: "#FF0040",
  yellow: "#FFD700",
  white: "#FFFFFF",
  gray: "#333333",
  grayLight: "#888888",
};

export const FONTS = {
  mono: "'Courier New', 'Lucida Console', Monaco, monospace",
};

export const FPS = 30;

// Scene timing in frames
export const SCENES = {
  TITLE_START: 0,
  TITLE_DURATION: 90,       // 3s

  INTRO_START: 90,
  INTRO_DURATION: 150,      // 5s

  FEATURES_START: 240,
  FEATURES_DURATION: 210,   // 7s

  TECH_START: 450,
  TECH_DURATION: 150,       // 5s

  BUILD_START: 600,
  BUILD_DURATION: 240,      // 8s

  DEPLOY_START: 840,
  DEPLOY_DURATION: 180,     // 6s

  DOCKER_START: 1020,
  DOCKER_DURATION: 150,     // 5s

  API_START: 1170,
  API_DURATION: 210,        // 7s

  OUTRO_START: 1380,
  OUTRO_DURATION: 120,      // 4s
};

export const TOTAL_DURATION = 1180; // ~39s: 8 scenes - 7 transitions×20f
