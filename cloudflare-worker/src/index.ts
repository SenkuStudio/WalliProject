export interface Env {
  DB: D1Database;
  ASSETS: R2Bucket;
  CDN_BASE_URL: string;
}

type WallpaperRow = {
  id: string;
  title: string;
  category: string;
  image_key: string;
  thumbnail_key: string;
  downloads: number;
  created_at: string;
  premium: number;
};

type CategoryRow = {
  name: string;
  thumbnail_key: string | null;
};

const corsHeaders: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "content-type,x-api-key",
  "Access-Control-Allow-Methods": "GET,POST,OPTIONS",
};

const json = (body: unknown, init: ResponseInit = {}) => {
  const headers = new Headers(init.headers);
  headers.set("content-type", "application/json; charset=utf-8");
  Object.entries(corsHeaders).forEach(([key, value]) => headers.set(key, value));
  return new Response(JSON.stringify(body), {
    ...init,
    headers,
  });
};

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value));

const normalizeSort = (sort: string | null): string => {
  switch ((sort || "latest").toLowerCase()) {
    case "popular":
      return "downloads DESC, created_at DESC";
    case "trending":
      return "trending_score DESC, downloads DESC, created_at DESC";
    case "latest":
    default:
      return "created_at DESC";
  }
};

const mapWallpaper = (env: Env, row: WallpaperRow) => ({
  id: row.id,
  title: row.title,
  category: row.category,
  image_url: `${env.CDN_BASE_URL}/${row.image_key}`,
  thumbnail_url: `${env.CDN_BASE_URL}/${row.thumbnail_key}`,
  downloads: row.downloads,
  created_at: row.created_at,
  premium: Boolean(row.premium),
});

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders });
    }

    const url = new URL(request.url);
    const { pathname } = url;

    if (pathname === "/api/v1/health") {
      return json({ ok: true, service: "walli-api" });
    }

    if (pathname === "/api/v1/categories" && request.method === "GET") {
      return handleCategories(env);
    }

    if (pathname === "/api/v1/wallpapers" && request.method === "GET") {
      return handleWallpapers(url, env);
    }

    if (pathname === "/api/v1/wallpapers/day" && request.method === "GET") {
      return handleWallpaperOfTheDay(env);
    }

    const downloadMatch = pathname.match(/^\/api\/v1\/wallpapers\/([^/]+)\/download$/);
    if (downloadMatch && request.method === "POST") {
      return handleDownloadAck(downloadMatch[1], env);
    }

    return json({ error: "Not found" }, { status: 404 });
  },
};

async function handleWallpapers(url: URL, env: Env): Promise<Response> {
  const page = clamp(Number(url.searchParams.get("page") || "1"), 1, 10_000);
  const limit = clamp(Number(url.searchParams.get("limit") || "20"), 1, 50);
  const category = url.searchParams.get("category")?.trim();
  const query = url.searchParams.get("query")?.trim();
  const sortSql = normalizeSort(url.searchParams.get("sort"));
  const offset = (page - 1) * limit;

  let sql = `
    SELECT id, title, category, image_key, thumbnail_key, downloads, created_at, premium
    FROM wallpapers
    WHERE 1 = 1
  `;
  const params: (string | number)[] = [];

  if (category && category !== "All") {
    sql += ` AND category = ?`;
    params.push(category);
  }

  if (query) {
    sql += ` AND (title LIKE ? OR category LIKE ? OR tags LIKE ?)`;
    const like = `%${query}%`;
    params.push(like, like, like);
  }

  sql += ` ORDER BY ${sortSql} LIMIT ? OFFSET ?`;
  params.push(limit + 1, offset);

  const result = await env.DB.prepare(sql).bind(...params).all<WallpaperRow>();
  const rows = result.results ?? [];
  const hasNext = rows.length > limit;
  const items = rows.slice(0, limit).map((row) => mapWallpaper(env, row));

  return json(
    {
      items,
      page,
      limit,
      hasNext,
    },
    {
      headers: {
        "Cache-Control": "public, max-age=60, s-maxage=60, stale-while-revalidate=300",
      },
    },
  );
}

async function handleCategories(env: Env): Promise<Response> {
  const result = await env.DB.prepare(`
    SELECT category as name, MIN(thumbnail_key) as thumbnail_key
    FROM wallpapers
    GROUP BY category
    ORDER BY category ASC
  `).all<CategoryRow>();

  const categories = (result.results ?? []).map((row) => ({
    name: row.name,
    cover_url: row.thumbnail_key ? `${env.CDN_BASE_URL}/${row.thumbnail_key}` : null,
  }));

  return json(categories, {
    headers: {
      "Cache-Control": "public, max-age=300, s-maxage=300",
    },
  });
}

async function handleWallpaperOfTheDay(env: Env): Promise<Response> {
  const featured = await env.DB.prepare(`
    SELECT id, title, category, image_key, thumbnail_key, downloads, created_at, premium
    FROM wallpapers
    WHERE featured = 1
    ORDER BY created_at DESC
    LIMIT 1
  `).first<WallpaperRow>();

  const row = featured ?? await env.DB.prepare(`
    SELECT id, title, category, image_key, thumbnail_key, downloads, created_at, premium
    FROM wallpapers
    ORDER BY created_at DESC
    LIMIT 1
  `).first<WallpaperRow>();

  if (!row) {
    return json({ error: "No wallpapers found" }, { status: 404 });
  }

  return json(mapWallpaper(env, row), {
    headers: {
      "Cache-Control": "public, max-age=300, s-maxage=300",
    },
  });
}

async function handleDownloadAck(wallpaperId: string, env: Env): Promise<Response> {
  await env.DB.prepare(`
    UPDATE wallpapers
    SET downloads = downloads + 1,
        trending_score = trending_score + 1
    WHERE id = ?
  `).bind(wallpaperId).run();

  return json({ ok: true });
}
