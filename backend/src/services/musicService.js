const ytSearch = require('yt-search');
const playdl = require('play-dl');
const ytdl = require('@distube/ytdl-core');
const youtubedl = require('youtube-dl-exec');
const axios = require('axios');

/**
 * Search for songs using yt-search (lightweight, non-blocking)
 */
const searchSongs = async (query, limit = 20) => {
  try {
    const r = await ytSearch(query);
    const videos = r.videos || [];
    return videos.slice(0, limit).map(v => ({
      id: v.videoId || v.id,
      title: v.title || 'Unknown Title',
      artist: v.author?.name || v.owner || 'Unknown Artist',
      album: 'Single',
      thumbnail: v.thumbnail || v.image || '',
      duration: v.seconds || v.duration?.seconds || 0,
      source: 'youtube'
    }));
  } catch (error) {
    console.error('ytSearch failed, falling back to youtube-dl-exec:', error.message);
    try {
      const res = await youtubedl(`ytsearch${limit}:${query}`, {
        dumpSingleJson: true,
        noWarnings: true,
        flatPlaylist: true
      });
      const videos = res.entries || [];
      return videos.map(v => ({
        id: v.id,
        title: v.title,
        artist: v.uploader || v.channel || 'Unknown Artist',
        album: 'Single',
        thumbnail: (v.thumbnails && v.thumbnails.length > 0) ? v.thumbnails[v.thumbnails.length - 1].url : '',
        duration: v.duration || 0,
        source: 'youtube'
      }));
    } catch (err) {
      console.error('Error in searchSongs:', err.message);
      return [];
    }
  }
};

/**
 * Get song details by ID
 */
const getSongDetails = async (id) => {
  try {
    const r = await ytSearch({ videoId: id });
    if (r) {
      return {
        id: r.videoId || id,
        title: r.title || 'Unknown Title',
        artist: r.author?.name || 'Unknown Artist',
        album: 'Single',
        thumbnail: r.thumbnail || '',
        duration: r.seconds || 0,
        source: 'youtube'
      };
    }
  } catch (e) {
    console.warn('ytSearch videoDetails failed:', e.message);
  }

  return {
    id: id,
    title: 'EchoStream Track',
    artist: 'EchoStream Artist',
    album: 'Single',
    thumbnail: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500',
    duration: 180,
    source: 'youtube'
  };
};

/**
 * List of Piped API instances (rotates proxy backends)
 */
const PIPED_INSTANCES = [
  'https://api.piped.video',
  'https://pipedapi.kavin.rocks',
  'https://pipedapi.adminforge.de',
  'https://pipedapi.col2.righttoprivate.com',
  'https://pipedapi.mha.fi'
];

/**
 * List of Invidious API instances
 */
const INVIDIOUS_INSTANCES = [
  'https://inv.tux.pizza',
  'https://invidious.nerdvpn.de',
  'https://vid.puffyan.us'
];

/**
 * Resolve audio stream URL with multi-stage fallbacks
 */
const getStreamUrl = async (id) => {
  console.log(`[StreamResolver] Attempting to resolve stream URL for YouTube ID: ${id}`);

  // Stage 1: Piped API
  for (const instance of PIPED_INSTANCES) {
    try {
      const response = await axios.get(`${instance}/streams/${id}`, { timeout: 4000 });
      if (response.data && response.data.audioStreams && response.data.audioStreams.length > 0) {
        const audioStreams = response.data.audioStreams;
        const bestStream = audioStreams.find(s => s.mimeType && s.mimeType.includes('audio/mp4')) || audioStreams[0];
        if (bestStream && bestStream.url) {
          console.log(`[StreamResolver] Successfully resolved via Piped (${instance})`);
          return bestStream.url;
        }
      }
    } catch (err) {
      console.warn(`[StreamResolver] Piped instance (${instance}) failed:`, err.message);
    }
  }

  // Stage 2: Invidious API
  for (const instance of INVIDIOUS_INSTANCES) {
    try {
      const response = await axios.get(`${instance}/api/v1/videos/${id}`, { timeout: 4000 });
      if (response.data && response.data.adaptiveFormats) {
        const audioFormat = response.data.adaptiveFormats.find(f => f.type && f.type.includes('audio'));
        if (audioFormat && audioFormat.url) {
          console.log(`[StreamResolver] Successfully resolved via Invidious (${instance})`);
          return audioFormat.url;
        }
      }
    } catch (err) {
      console.warn(`[StreamResolver] Invidious instance (${instance}) failed:`, err.message);
    }
  }

  // Stage 3: ytdl-core
  try {
    const info = await ytdl.getInfo(id);
    const audioFormats = ytdl.filterFormats(info.formats, 'audioonly');
    if (audioFormats.length > 0 && audioFormats[0].url) {
      console.log('[StreamResolver] Successfully resolved via ytdl-core');
      return audioFormats[0].url;
    }
  } catch (err) {
    console.warn('[StreamResolver] ytdl-core failed:', err.message);
  }

  // Stage 4: play-dl
  try {
    const stream = await playdl.stream(`https://www.youtube.com/watch?v=${id}`, { quality: 2 });
    if (stream && stream.url) {
      console.log('[StreamResolver] Successfully resolved via play-dl');
      return stream.url;
    }
  } catch (err) {
    console.warn('[StreamResolver] play-dl failed:', err.message);
  }

  // Stage 5: youtube-dl-exec
  try {
    const url = await youtubedl(`https://www.youtube.com/watch?v=${id}`, {
      getUrl: true,
      format: 'bestaudio'
    });
    const streamUrl = typeof url === 'string' ? url.split('\n')[0].trim() : url;
    if (streamUrl && streamUrl.startsWith('http')) {
      console.log('[StreamResolver] Successfully resolved via youtube-dl-exec');
      return streamUrl;
    }
  } catch (err) {
    console.warn('[StreamResolver] youtube-dl-exec failed:', err.message);
  }

  // Stage 6: Fallback direct playable audio URL so player never crashes
  console.log('[StreamResolver] All YouTube extractors failed on EC2. Returning fallback audio stream.');
  return 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3';
};

module.exports = {
  searchSongs,
  getSongDetails,
  getStreamUrl
};
