CREATE TABLE IF NOT EXISTS wallpapers (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    category TEXT NOT NULL,
    image_key TEXT NOT NULL,
    thumbnail_key TEXT NOT NULL,
    tags TEXT DEFAULT '',
    downloads INTEGER NOT NULL DEFAULT 0,
    trending_score REAL NOT NULL DEFAULT 0,
    premium INTEGER NOT NULL DEFAULT 0,
    featured INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_wallpapers_category ON wallpapers(category);
CREATE INDEX IF NOT EXISTS idx_wallpapers_created_at ON wallpapers(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_wallpapers_downloads ON wallpapers(downloads DESC);
CREATE INDEX IF NOT EXISTS idx_wallpapers_trending_score ON wallpapers(trending_score DESC);
