/**
 * ================================================================
 * MC VOICE TRAINING — Patch thumbnailUrl for existing lessons
 * ================================================================
 * Updates thumbnailUrl for ALL lessons that have no thumbnail,
 * based on their category. Does NOT delete or modify content.
 *
 * Chạy:
 *   mongosh "mongodb+srv://<user>:<pass>@<host>/voice-tranning?..." \
 *     --file seed-data/patch_thumbnails.js
 * ================================================================
 */

use("voice-tranning");

// Unsplash pool per category — 16:9, 800x450, free
const THUMBS = {
  WEDDING: [
    "https://images.unsplash.com/photo-1519225421980-715cb0215aed?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1606800052052-a08af7148866?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1583939003579-730e3918a45a?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1465495976277-4387d4b0b4c6?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1511795409834-ef04bbd61622?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1591604466107-ec97de577aff?w=800&h=450&fit=crop&auto=format",
  ],
  GALA: [
    "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1511578314322-379afb476865?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1505373877841-8d25f7d46678?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1569263979104-865ab7cd8d13?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?w=800&h=450&fit=crop&auto=format",
  ],
  CORPORATE: [
    "https://images.unsplash.com/photo-1591115765373-5207764f72e7?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1475721027785-f74eccf877e2?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1515187029135-18ee286d815b?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1560523159-4a9692d222f9?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1542744173-8e7e53415bb0?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1453738773917-9c3eff1db985?w=800&h=450&fit=crop&auto=format",
  ],
  TALKSHOW: [
    "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1478737270197-2468169085b0?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1612872087720-bb876e2e67d1?w=800&h=450&fit=crop&auto=format",
  ],
  CEREMONY: [
    "https://images.unsplash.com/photo-1532712938310-34cb3982ef74?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1524178232363-1fb2b075b655?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1523580494863-6f3031224c94?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1498243691581-b145c3f54a5a?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1548504769-900b70ed122e?w=800&h=450&fit=crop&auto=format",
  ],
  GENERAL: [
    "https://images.unsplash.com/photo-1560523159-4a9692d222f9?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1546953304-5d96f43c2e94?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1589903308904-1010c2294adc?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1588681664899-f142ff2dc9b1?w=800&h=450&fit=crop&auto=format",
  ],
};

// Fallback for any unknown category
const FALLBACK = [
  "https://images.unsplash.com/photo-1560523159-4a9692d222f9?w=800&h=450&fit=crop&auto=format",
  "https://images.unsplash.com/photo-1475721027785-f74eccf877e2?w=800&h=450&fit=crop&auto=format",
  "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800&h=450&fit=crop&auto=format",
];

// Track index per category so no two consecutive lessons in same category get same image
const counters = {};
function pickThumb(category) {
  const pool = THUMBS[category] || FALLBACK;
  if (!counters[category]) counters[category] = 0;
  const url = pool[counters[category] % pool.length];
  counters[category]++;
  return url;
}

print("🔍 Fetching all lessons without thumbnailUrl...");

// Fetch lessons missing thumbnail (null, empty string, or field absent)
const lessons = db["voice_lessons"].find({
  $or: [
    { thumbnailUrl: { $exists: false } },
    { thumbnailUrl: null },
    { thumbnailUrl: "" },
  ]
}).toArray();

print(`Found ${lessons.length} lessons needing thumbnails.`);

if (lessons.length === 0) {
  print("✅ All lessons already have thumbnails. Nothing to do.");
} else {
  let updated = 0;
  lessons.forEach((lesson) => {
    const cat = lesson.category || "GENERAL";
    const thumb = pickThumb(cat);
    db["voice_lessons"].updateOne(
      { _id: lesson._id },
      { $set: { thumbnailUrl: thumb, updatedAt: new Date() } }
    );
    print(`  ✓ [${cat}] "${lesson.title || lesson._id}" → ${thumb.substring(0, 60)}...`);
    updated++;
  });

  print(`\n✅ Patched ${updated} lessons with thumbnailUrl.`);
}

// Summary
print("\n📊 Final thumbnail coverage:");
const total     = db["voice_lessons"].countDocuments({});
const withThumb = db["voice_lessons"].countDocuments({ thumbnailUrl: { $ne: null, $exists: true, $gt: "" } });
print(`  With thumbnail: ${withThumb} / ${total}`);
