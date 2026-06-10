import { defineConfig, type UserConfigExport } from "@tarojs/cli";
import path from "node:path";

export default defineConfig(async (merge) => {
  const baseConfig: UserConfigExport = {
    projectName: "JapaneseStudyMiniapp",
    date: "2026-06-10",
    designWidth: 750,
    deviceRatio: {
      640: 2.34 / 2,
      750: 1,
      828: 1.81 / 2
    },
    sourceRoot: "src",
    outputRoot: "dist",
    alias: {
      "@": path.resolve(__dirname, "..", "src")
    },
    plugins: [],
    defineConstants: {
      "process.env.TARO_APP_API_BASE_URL": JSON.stringify(
        process.env.TARO_APP_API_BASE_URL ?? "http://localhost:8080/api"
      )
    },
    copy: {
      patterns: [],
      options: {}
    },
    framework: "react",
    compiler: {
      type: "webpack5",
      prebundle: {
        enable: false
      }
    },
    mini: {
      postcss: {
        pxtransform: {
          enable: true,
          config: {}
        },
        cssModules: {
          enable: false,
          config: {
            namingPattern: "module",
            generateScopedName: "[name]__[local]___[hash:base64:5]"
          }
        }
      }
    },
    h5: {}
  };

  if (process.env.NODE_ENV === "development") {
    return merge({}, baseConfig, (await import("./dev")).default);
  }

  return merge({}, baseConfig, (await import("./prod")).default);
});
