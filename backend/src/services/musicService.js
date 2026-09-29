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
      artistId: v.artists?.primary?.[0]?.id || '',
      album: v.album?.name || 'Single',
      albumId: v.album?.id || '',
      genre: v.language || 'Unknown',
      thumbnail: (v.image && v.image.length > 0) ? v.image[v.image.length - 1].url : '',
      duration: v.duration || 0,
      source: 'jiosaavn',
      year: v.year || '',
      playCount: v.playCount || 0
    }));
  } catch (error) {
    console.error('JioSaavn search failed:', error.message);
    return [];
  }
};

/**
 * Search with categories - returns songs, artists, albums
 */
const searchAll = async (query, limit = 30) => {
  try {
    // Normalize query for better matching
    const normalizedQuery = query.trim().toLowerCase();

    // Search songs
    const songResults = await searchSongs(query, limit);

    // Extract unique artists from song results
    const artistMap = new Map();
    songResults.forEach(song => {
      if (song.artist && song.artist !== 'Unknown Artist') {
        const artistKey = song.artist.toLowerCase();
        if (!artistMap.has(artistKey)) {
          artistMap.set(artistKey, {
            id: song.artistId || `artist_${artistKey.replace(/\s+/g, '_')}`,
            name: song.artist,
            imageUrl: song.thumbnail || '',
            songCount: 1
          });
        } else {
          artistMap.get(artistKey).songCount++;
        }
      }
    });

    // Extract unique albums from song results
    const albumMap = new Map();
    songResults.forEach(song => {
      if (song.album && song.album !== 'Single' && song.album !== 'Unknown Album') {
        const albumKey = song.album.toLowerCase();
        if (!albumMap.has(albumKey)) {
          albumMap.set(albumKey, {
            id: song.albumId || `album_${albumKey.replace(/\s+/g, '_')}`,
            title: song.album,
            artist: song.artist,
            artworkUrl: song.thumbnail || '',
            songCount: 1
          });
        } else {
          albumMap.get(albumKey).songCount++;
        }
      }
    });

    // Sort artists by relevance (name match + song count)
    const artists = Array.from(artistMap.values())
      .sort((a, b) => {
        const aMatch = a.name.toLowerCase().includes(normalizedQuery) ? 10 : 0;
        const bMatch = b.name.toLowerCase().includes(normalizedQuery) ? 10 : 0;
        return (bMatch + b.songCount) - (aMatch + a.songCount);
      })
      .slice(0, 10);

    const albums = Array.from(albumMap.values()).slice(0, 10);

    return {
      songs: songResults,
      artists,
      albums
    };
  } catch (error) {
    console.error('Search all failed:', error.message);
    return { songs: [], artists: [], albums: [] };
  }
};

/**
 * Search for artist songs
 */
const searchArtistSongs = async (artistName, limit = 50) => {
  try {
    return await searchSongs(artistName, limit);
  } catch (error) {
    console.error('Artist songs search failed:', error.message);
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
        artistId: song.artists?.primary?.[0]?.id || '',
        album: song.album?.name || 'Single',
        albumId: song.album?.id || '',
        genre: song.language || 'Unknown',
        thumbnail: (song.image && song.image.length > 0) ? song.image[song.image.length - 1].url : '',
        duration: song.duration || 0,
        source: 'jiosaavn',
        year: song.year || '',
        playCount: song.playCount || 0
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
  searchAll,
  searchArtistSongs,
  getSongDetails,
  getStreamUrl
};
