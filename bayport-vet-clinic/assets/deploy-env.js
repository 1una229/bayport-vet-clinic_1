/**
 * API base URL for hosted static sites (Netlify / Vercel).
 * Netlify build overwrites this via scripts/write-deploy-env.js.
 */
(function () {
  if (typeof window !== "undefined" && window.desktopBridge && window.desktopBridge.apiBase) {
    window.BAYPORT_API_BASE = window.desktopBridge.apiBase;
    window.USE_API = true;
    try {
      localStorage.setItem("bayport_api_base", window.desktopBridge.apiBase);
    } catch (_) {}
  }
  if (!window.BAYPORT_API_BASE) {
    try {
      const h = (window.location.hostname || "").toLowerCase();
      if (h.includes("netlify.app") || h.includes("vercel.app")) {
        window.BAYPORT_API_BASE = "https://bayport-api.onrender.com/api";
        window.USE_API = true;
      }
    } catch (_) {}
  }
  window.BAYPORT_API_BASE = window.BAYPORT_API_BASE || "";
})();
