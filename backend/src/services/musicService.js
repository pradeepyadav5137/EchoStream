const ytSearch = require('yt-search');
const playdl = require('play-dl');
const ytdl = require('@distube/ytdl-core');
const youtubedl = require('youtube-dl-exec');
const axios = require('axios');

const USER_AGENT = 'Mozilla/5.0 (Linux; Android 12; Pixel 6 Build/SQ3A.220705.004) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36';

/**
 * Search for songs using YouTube Search (yt-search)
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
    console.error('ytSearch failed:', error.message);
    return [];
  }
};

/**
 * Get YouTube song details by ID
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
    title: 'YouTube Track',
    artist: 'YouTube Artist',
    album: 'Single',
    thumbnail: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500',
    duration: 180,
    source: 'youtube'
  };
};

// Verified active YouTube stream extractors (Piped / Invidious)
const PIPED_INSTANCES = [
  'https://api.piped.video',
  'https://pipedapi.kavin.rocks',
  'https://pipedapi.adminforge.de',
  'https://pipedapi.tokhmi.xyz',
  'https://pipedapi.col2.righttoprivate.com',
  'https://pipedapi.privacy.com.de'
];

const INVIDIOUS_INSTANCES = [
  'https://inv.tux.pizza',
  'https://invidious.nerdvpn.de',
  'https://invidious.no-bo.fr',
  'https://invidious.io.lol'
];

/**
 * Resolve direct YouTube audio stream URL for a video ID
 */
const getStreamUrl = async (id) => {
  console.log(`[YouTubeStreamResolver] Extracting YouTube audio stream for video ID: ${id}`);

  // Method 1: yt-dlp with Mobile Android/iOS Player Client Bypasses
  try {
    const url = await youtubedl(`https://www.youtube.com/watch?v=${id}`, {
      getUrl: true,
      format: 'bestaudio/best',
      extractorArgs: 'youtube:player_client=mweb,android,ios',
      noWarnings: true,
      noCheckCertificates: true,
      preferFreeFormats: true
    });
    const streamUrl = typeof url === 'string' ? url.split('\n')[0].trim() : url;
    if (streamUrl && streamUrl.startsWith('http')) {
      console.log('[YouTubeStreamResolver] Successfully extracted via yt-dlp (Mobile Client)');
      return streamUrl;
    }
  } catch (err) {
    console.warn('[YouTubeStreamResolver] yt-dlp Mobile Client failed:', err.message);
  }

  // Method 2: Piped API YouTube Extractor
  for (const instance of PIPED_INSTANCES) {
    try {
      const response = await axios.get(`${instance}/streams/${id}`, {
        timeout: 4000,
        headers: { 'User-Agent': USER_AGENT }
      });
      if (response.data && response.data.audioStreams && response.data.audioStreams.length > 0) {
        const audioStreams = response.data.audioStreams;
        const bestStream = audioStreams.find(s => s.mimeType && s.mimeType.includes('audio/mp4')) || audioStreams[0];
        if (bestStream && bestStream.url) {
          console.log(`[YouTubeStreamResolver] Successfully extracted via Piped (${instance})`);
          return bestStream.url;
        }
      }
    } catch (err) {
      console.warn(`[YouTubeStreamResolver] Piped (${instance}) failed:`, err.message);
    }
  }

  // Method 3: Invidious API YouTube Extractor
  for (const instance of INVIDIOUS_INSTANCES) {
    try {
      const response = await axios.get(`${instance}/api/v1/videos/${id}`, {
        timeout: 4000,
        headers: { 'User-Agent': USER_AGENT }
      });
      if (response.data && response.data.adaptiveFormats) {
        const audioFormat = response.data.adaptiveFormats.find(f => f.type && f.type.includes('audio'));
        if (audioFormat && audioFormat.url) {
          console.log(`[YouTubeStreamResolver] Successfully extracted via Invidious (${instance})`);
          return audioFormat.url;
        }
      }
    } catch (err) {
      console.warn(`[YouTubeStreamResolver] Invidious (${instance}) failed:`, err.message);
    }
  }

  // Method 4: Cobalt YouTube Audio Extractor
  try {
    const cobaltRes = await axios.post('https://api.cobalt.tools/', {
      url: `https://www.youtube.com/watch?v=${id}`
    }, {
      timeout: 5000,
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'User-Agent': USER_AGENT
      }
    });
    if (cobaltRes.data && (cobaltRes.data.url || cobaltRes.data.picker)) {
      const streamUrl = cobaltRes.data.url || (cobaltRes.data.picker && cobaltRes.data.picker[0]?.url);
      if (streamUrl) {
        console.log(`[YouTubeStreamResolver] Successfully extracted via Cobalt API`);
        return streamUrl;
      }
    }
  } catch (err) {
    console.warn('[YouTubeStreamResolver] Cobalt API failed:', err.message);
  }

  // Method 5: @distube/ytdl-core with Android Mobile User-Agent
  try {
    const info = await ytdl.getInfo(id, {
      requestOptions: {
        headers: { 'User-Agent': USER_AGENT }
      }
    });
    const audioFormats = ytdl.filterFormats(info.formats, 'audioonly');
    if (audioFormats.length > 0 && audioFormats[0].url) {
      console.log('[YouTubeStreamResolver] Successfully extracted via ytdl-core');
      return audioFormats[0].url;
    }
  } catch (err) {
    console.warn('[YouTubeStreamResolver] ytdl-core failed:', err.message);
  }

  // Method 6: play-dl Stream Extractor
  try {
    const stream = await playdl.stream(`https://www.youtube.com/watch?v=${id}`, { quality: 2 });
    if (stream && stream.url) {
      console.log('[YouTubeStreamResolver] Successfully extracted via play-dl');
      return stream.url;
    }
  } catch (err) {
    console.warn('[YouTubeStreamResolver] play-dl failed:', err.message);
  }

  throw new Error(`Unable to extract YouTube audio stream for video ID: ${id}`);
};

module.exports = {
  searchSongs,
  getSongDetails,
  getStreamUrl
};
