const { UnivoraSaavn } = require('univora-saavn');
const saavn = new UnivoraSaavn();

/**
 * Search for songs using JioSaavn API
 */
const searchSongs = async (query, limit = 20) => {
  try {
    const res = await saavn.searchSongs(query);
    const songs = res.results || [];
    return songs.slice(0, limit).map(v => ({
      id: v.id,
      title: v.name,
      artist: v.artists?.primary?.[0]?.name || 'Unknown Artist',
      album: v.album?.name || 'Single',
      thumbnail: (v.image && v.image.length > 0) ? v.image[v.image.length - 1].url : '',
      duration: v.duration || 0,
      source: 'jiosaavn'
    }));
  } catch (error) {
    console.error('JioSaavn search failed:', error.message);
    return [];
  }
};

/**
 * Get JioSaavn song details by ID
 */
const getSongDetails = async (id) => {
  try {
    const res = await saavn.getSongDetails(id);
    const song = res?.[0];
    if (song) {
      return {
        id: song.id,
        title: song.name,
        artist: song.artists?.primary?.[0]?.name || 'Unknown Artist',
        album: song.album?.name || 'Single',
        thumbnail: (song.image && song.image.length > 0) ? song.image[song.image.length - 1].url : '',
        duration: song.duration || 0,
        source: 'jiosaavn'
      };
    }
  } catch (e) {
    console.warn('JioSaavn getSongDetails failed:', e.message);
  }

  return {
    id,
    title: 'Unknown Track',
    artist: 'Unknown Artist',
    album: 'Single',
    thumbnail: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500',
    duration: 180,
    source: 'jiosaavn'
  };
};

/**
 * Resolve direct JioSaavn audio stream URL for a song ID
 */
const getStreamUrl = async (id) => {
  console.log(`[JioSaavnResolver] Extracting audio stream for ID: ${id}`);
  
  try {
    const res = await saavn.getSongDetails(id);
    const song = res?.[0];
    
    if (song && song.downloadUrl) {
      const best = song.downloadUrl.find(u => u.quality === '320kbps') || 
                   song.downloadUrl.find(u => u.quality === '160kbps') || 
                   song.downloadUrl[song.downloadUrl.length - 1];
                   
      if (best && best.url) {
        console.log(`[JioSaavnResolver] Successfully extracted URL: ${best.quality}`);
        return best.url;
      }
    }
  } catch (err) {
    console.warn('[JioSaavnResolver] Failed:', err.message);
  }

  throw new Error(`Unable to extract JioSaavn audio stream for ID: ${id}`);
};

module.exports = {
  searchSongs,
  getSongDetails,
  getStreamUrl
};
