INSERT OR REPLACE INTO wallpapers (
    id,
    title,
    category,
    image_key,
    thumbnail_key,
    tags,
    downloads,
    trending_score,
    premium,
    featured,
    created_at
) VALUES
    (
        '1',
        'Nature Mountain',
        'Nature',
        'full/nature/mountain.jpg',
        'thumbs/nature/mountain.jpg',
        'mountain,forest,nature,green',
        1200,
        75,
        0,
        1,
        '2026-01-01'
    ),
    (
        '2',
        'Neon City Lights',
        'AMOLED',
        'full/amoled/neon-city.jpg',
        'thumbs/amoled/neon-city.jpg',
        'city,night,amoled,neon',
        980,
        68,
        1,
        0,
        '2026-01-03'
    );
