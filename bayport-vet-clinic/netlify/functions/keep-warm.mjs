/** Ping Render every 14 minutes so the free-tier API stays warm for login. */
export default async () => {
  const url = "https://bayport-api.onrender.com/api/health";
  try {
    const res = await fetch(url, { cache: "no-store" });
    return new Response(JSON.stringify({ ok: res.ok, status: res.status, at: new Date().toISOString() }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  } catch (err) {
    return new Response(JSON.stringify({ ok: false, error: err.message }), { status: 200 });
  }
};

export const config = {
  schedule: "*/10 * * * *",
};
