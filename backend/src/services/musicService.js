const ytSearch = require('yt-search');
const playdl = require('play-dl');

/**
 * Normalize song data
 */
const normalizeSong = (video) => {
  return {
    id: video.videoId || video.id,
    title: video.title || 'Unknown Title',
    artist: video.channel?.name || video.author?.name || 'Unknown Artist',
    album: 'Unknown Album',
    thumbnail: video.thumbnails && video.thumbnails.length > 0 ? video.thumbnails[0].url : (video.thumbnail || ''),
    duration: video.durationInSec || video.seconds || 0,
    source: 'youtube'
  };
};

const youtubedl = require('youtube-dl-exec');

/**
 * Search for songs
 */
const searchSongs = async (query, limit = 20) => {
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
      album: 'Unknown Album',
      thumbnail: (v.thumbnails && v.thumbnails.length > 0) ? v.thumbnails[v.thumbnails.length - 1].url : '',
      duration: v.duration || 0,
      source: 'youtube'
    }));
  } catch (error) {
    console.error('Error in searchSongs:', error);
    throw error;
  }
};

/**
 * Get song details by ID
 */
const getSongDetails = async (id) => {
  try {
    const res = await youtubedl(`https://www.youtube.com/watch?v=${id}`, {
      dumpSingleJson: true,
      noWarnings: true
    });
    return {
      id: res.id,
      title: res.title,
      artist: res.uploader || res.channel || 'Unknown Artist',
      album: 'Unknown Album',
      thumbnail: (res.thumbnails && res.thumbnails.length > 0) ? res.thumbnails[res.thumbnails.length - 1].url : '',
      duration: res.duration || 0,
      source: 'youtube'
    };
  } catch (error) {
    console.error('Error in getSongDetails:', error);
    throw error;
  }
};


/**
 * Resolve audio stream URL
 */
const getStreamUrl = async (id) => {
  try {
    const url = await youtubedl(`https://www.youtube.com/watch?v=${id}`, {
      getUrl: true,
      format: 'bestaudio'
    });
    // youtubedl might return multiple lines if multiple formats are matched or stdout contains extra info.
    // Usually with getUrl and format it returns one line. We take the first line just in case.
    const streamUrl = typeof url === 'string' ? url.split('\n')[0].trim() : url;
    return streamUrl;
  } catch (error) {
    console.error('Error in getStreamUrl:', error);
    throw error;
  }
};

module.exports = {
  searchSongs,
  getSongDetails,
  getStreamUrl
};
