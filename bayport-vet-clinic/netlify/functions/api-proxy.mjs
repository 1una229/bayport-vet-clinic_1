/** Same-origin proxy: browser → Netlify → Render (avoids CORS and blocked direct calls). */
const RENDER_BASE = (process.env.BAYPORT_API_BASE || "https://bayport-api.onrender.com/api").replace(
  /\/+$/,
  "",
);

function resolveApiPath(pathname) {
  const path = String(pathname || "");
  const proxyPrefix = "/.netlify/functions/api-proxy";
  if (path.startsWith(proxyPrefix)) {
    const rest = path.slice(proxyPrefix.length);
    return rest.startsWith("/") ? rest : `/${rest || ""}`;
  }
  if (path.startsWith("/api")) {
    const rest = path.slice(4);
    return rest.startsWith("/") ? rest : `/${rest || ""}`;
  }
  return path.startsWith("/") ? path : `/${path}`;
}

export default async (request) => {
  const url = new URL(request.url);
  const apiPath = resolveApiPath(url.pathname);
  const targetUrl = `${RENDER_BASE}${apiPath}${url.search}`;

  const headers = new Headers();
  for (const [key, value] of request.headers.entries()) {
    const lower = key.toLowerCase();
    if (lower === "host" || lower === "connection" || lower === "content-length") continue;
    headers.set(key, value);
  }

  const method = request.method || "GET";
  let body;
  if (method !== "GET" && method !== "HEAD") {
    body = await request.text();
  }

  try {
    const upstream = await fetch(targetUrl, {
      method,
      headers,
      body,
      signal: AbortSignal.timeout(25000),
    });
    const responseBody = await upstream.arrayBuffer();
    const outHeaders = new Headers();
    const contentType = upstream.headers.get("Content-Type");
    if (contentType) outHeaders.set("Content-Type", contentType);
    outHeaders.set("Cache-Control", "no-store");
    return new Response(responseBody, { status: upstream.status, headers: outHeaders });
  } catch (err) {
    return new Response(
      JSON.stringify({
        status: "WAKING",
        message: "Backend is starting. Retry in a few seconds.",
        error: err.message || "upstream unreachable",
      }),
      {
        status: 503,
        headers: {
          "Content-Type": "application/json",
          "Retry-After": "3",
          "Cache-Control": "no-store",
        },
      },
    );
  }
};
