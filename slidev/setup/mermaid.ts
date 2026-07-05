import { defineMermaidSetup } from "@slidev/types";

export default defineMermaidSetup(() => ({
  theme: "base",
  themeVariables: {
    darkMode: true,
    background: "#0a0f1c",
    primaryColor: "#121b30",
    primaryTextColor: "#e6edf7",
    primaryBorderColor: "#2dd4bf",
    lineColor: "#8494b3",
    secondaryColor: "#182338",
    tertiaryColor: "#121b30",
    clusterBkg: "rgba(18, 27, 48, 0.6)",
    clusterBorder: "#24304a",
    edgeLabelBackground: "#0a0f1c",
    fontFamily: "Inter, sans-serif",
    fontSize: "14px",
  },
}));
