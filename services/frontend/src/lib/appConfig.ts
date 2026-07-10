// Deployment info shown in the sidebar footer.
//
// The commit SHA is baked in at build time (VITE_COMMIT_SHA build arg; see
// Dockerfile and .github/workflows/_build-images.yml). The monitoring URLs are
// runtime config: /config.js sets window.__APP_CONFIG__ and is regenerated from
// config.js.template at container start, so one image serves every deployment.

type AppRuntimeConfig = {
  grafanaUrl?: string;
  prometheusUrl?: string;
};

declare global {
  interface Window {
    __APP_CONFIG__?: AppRuntimeConfig;
  }
}

// A ":port" value means "the host serving this page, on that port". That lets
// the compose stack use one value that works for localhost and for a VM
// reached by IP. Empty values hide the corresponding link.
function resolveUrl(value: string | undefined): string {
  if (!value) return "";
  if (value.startsWith(":")) return `${window.location.protocol}//${window.location.hostname}${value}`;
  return value;
}

const runtime: AppRuntimeConfig = (typeof window !== "undefined" && window.__APP_CONFIG__) || {};

const GITHUB_REPO_URL = "https://github.com/AET-DevOps26/team-panic-at-the-console";

const commitSha = import.meta.env.VITE_COMMIT_SHA || "dev";

export const appConfig = {
  commitSha,
  commitUrl: commitSha === "dev" ? undefined : `${GITHUB_REPO_URL}/commit/${commitSha}`,
  grafanaUrl: resolveUrl(runtime.grafanaUrl),
  prometheusUrl: resolveUrl(runtime.prometheusUrl),
};
