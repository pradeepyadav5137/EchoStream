const ytSearch = require('yt-search');
const playdl = require('play-dl');
const ytdl = require('@distube/ytdl-core');
const youtubedl = require('youtube-dl-exec');
const axios = require('axios');

const USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36';

/**
 * Search for songs using yt-search
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

const PIPED_INSTANCES = [
  'https://api.piped.video',
  'https://pipedapi.kavin.rocks',
  'https://pipedapi.adminforge.de',
  'https://pipedapi.col2.righttoprivate.com',
  'https://pipedapi.mha.fi'
];

const INVIDIOUS_INSTANCES = [
  'https://inv.tux.pizza',
  'https://invidious.nerdvpn.de',
  'https://vid.puffyan.us',
  'https://invidious.drgns.space'
];

/**
 * Resolve audio stream URL for a song ID
 */
const getStreamUrl = async (id) => {
  console.log(`[StreamResolver] Attempting to resolve stream URL for YouTube ID: ${id}`);

  // Stage 1: Piped API with Browser User-Agent
  for (const instance of PIPED_INSTANCES) {
    try {
      const response = await axios.get(`${instance}/streams/${id}`, {
        timeout: 5000,
        headers: { 'User-Agent': USER_AGENT }
      });
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

  // Stage 2: Invidious API with Browser User-Agent
  for (const instance of INVIDIOUS_INSTANCES) {
    try {
      const response = await axios.get(`${instance}/api/v1/videos/${id}`, {
        timeout: 5000,
        headers: { 'User-Agent': USER_AGENT }
      });
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

  // Stage 3: Cobalt API
  try {
    const cobaltRes = await axios.post('https://api.cobalt.tools/', {
      url: `https://www.youtube.com/watch?v=${id}`,
      downloadMode: 'audio',
      audioFormat: 'mp3'
    }, {
      timeout: 6000,
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'User-Agent': USER_AGENT
      }
    });
    if (cobaltRes.data && (cobaltRes.data.url || cobaltRes.data.picker)) {
      const streamUrl = cobaltRes.data.url || (cobaltRes.data.picker && cobaltRes.data.picker[0]?.url);
      if (streamUrl) {
        console.log(`[StreamResolver] Successfully resolved via Cobalt API`);
        return streamUrl;
      }
    }
  } catch (err) {
    console.warn(`[StreamResolver] Cobalt API failed:`, err.message);
  }

  // Stage 4: ytdl-core
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

  // Stage 5: play-dl
  try {
    const stream = await playdl.stream(`https://www.youtube.com/watch?v=${id}`, { quality: 2 });
    if (stream && stream.url) {
      console.log('[StreamResolver] Successfully resolved via play-dl');
      return stream.url;
    }
  } catch (err) {
    console.warn('[StreamResolver] play-dl failed:', err.message);
  }

  // Stage 6: youtube-dl-exec
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

  // Stage 7: Jamendo Exact Track Search Match Fallback
  try {
    const details = await getSongDetails(id);
    if (details && details.title && details.title !== 'Unknown Title') {
      const cleanTitle = details.title.replace(/[\(\)\[\]]/g, '').trim();
      console.log(`[StreamResolver] Searching Jamendo for track match: '${cleanTitle}'`);
      const jamendoRes = await axios.get(`https://api.jamendo.com/v3.0/tracks/?client_id=56b49247&format=json&limit=1&namesearch=${encodeURIComponent(cleanTitle)}`, { timeout: 5000 });
      if (jamendoRes.data && jamendoRes.data.results && jamendoRes.data.results.length > 0) {
        const jamendoTrack = jamendoRes.data.results[0];
        if (jamendoTrack.audio) {
          console.log(`[StreamResolver] Successfully resolved matching Jamendo track audio for '${cleanTitle}'`);
          return jamendoTrack.audio;
        }
      }
    }
  } catch (err) {
    console.warn('[StreamResolver] Jamendo search match failed:', err.message);
  }

  throw new Error(`Unable to resolve stream URL for YouTube ID: ${id}`);
};

module.exports = {
  searchSongs,
  getSongDetails,
  getStreamUrl
};
