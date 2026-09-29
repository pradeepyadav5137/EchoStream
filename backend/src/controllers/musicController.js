const axios = require('axios');
const musicService = require('../services/musicService');
const History = require('../models/History');
const Favorite = require('../models/Favorite');
const Follow = require('../models/Follow');
const mongoose = require('mongoose');

const search = async (req, res) => {
  try {
    const { q, page = 1, limit = 20 } = req.query;
    if (!q) {
      return res.status(400).json({ success: false, message: 'Query parameter q is required' });
    }
    
    const allResults = await musicService.searchSongs(q, 100);
    const startIndex = (Number(page) - 1) * Number(limit);
    const endIndex = startIndex + Number(limit);
    
    const results = allResults.slice(startIndex, endIndex);
    const hasMore = endIndex < allResults.length;
    
    res.json({
      success: true,
      query: q,
      page: Number(page),
      limit: Number(limit),
      hasMore,
      results
    });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to search songs', error: error.message });
  }
};

// Categorized search - returns songs, artists, albums
const searchAll = async (req, res) => {
  try {
    const { q, limit = 30 } = req.query;
    if (!q) {
      return res.status(400).json({ success: false, message: 'Query parameter q is required' });
    }

    const results = await musicService.searchAll(q, Number(limit));

    res.json({
      success: true,
      query: q,
      songs: results.songs,
      artists: results.artists,
      albums: results.albums
    });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to search', error: error.message });
  }
};

// Get songs by artist name (for Artist Radio / Play All)
const getArtistSongs = async (req, res) => {
  try {
    const { name } = req.query;
    if (!name) {
      return res.status(400).json({ success: false, message: 'Artist name parameter is required' });
    }

    const songs = await musicService.searchArtistSongs(name, 50);

    // Filter to prioritize songs actually by this artist
    const normalizedName = name.toLowerCase().trim();
    const artistSongs = songs.filter(s => 
      s.artist.toLowerCase().includes(normalizedName) ||
      normalizedName.includes(s.artist.toLowerCase())
    );

    // If we got filtered results, use them; otherwise use all search results
    const finalSongs = artistSongs.length > 0 ? artistSongs : songs;

    res.json({
      success: true,
      artist: name,
      songs: finalSongs
    });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to get artist songs', error: error.message });
  }
};

const getSong = async (req, res) => {
  try {
    const { id } = req.params;
    const song = await musicService.getSongDetails(id);
    res.json({ success: true, song });
  } catch (error) {
    res.status(404).json({ success: false, message: 'Song not found', error: error.message });
  }
};

const getStream = async (req, res) => {
  try {
    const { id } = req.params;
    const rawUrl = await musicService.getStreamUrl(id);
    const host = req.get('host');
    const protocol = req.protocol;
    const proxyUrl = `${protocol}://${host}/api/music/proxy-stream/${id}`;

    res.json({ success: true, url: proxyUrl, rawUrl });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to resolve stream URL', error: error.message });
  }
};

const proxyStream = async (req, res) => {
  try {
    const { id } = req.params;
    const rawUrl = await musicService.getStreamUrl(id);

    const headers = {
      'User-Agent': 'Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36'
    };

    if (req.headers.range) {
      headers['Range'] = req.headers.range;
    }

    const audioResponse = await axios({
      method: 'get',
      url: rawUrl,
      responseType: 'stream',
      headers: headers,
      timeout: 10000
    });

    if (audioResponse.headers['content-type']) {
      res.setHeader('Content-Type', audioResponse.headers['content-type']);
    } else {
      res.setHeader('Content-Type', 'audio/mp4');
    }

    if (audioResponse.headers['content-length']) {
      res.setHeader('Content-Length', audioResponse.headers['content-length']);
    }

    if (audioResponse.headers['content-range']) {
      res.setHeader('Content-Range', audioResponse.headers['content-range']);
      res.status(206);
    } else {
      res.status(200);
    }

    res.setHeader('Accept-Ranges', 'bytes');
    audioResponse.data.pipe(res);
  } catch (error) {
    console.error(`[ProxyStream] Error streaming audio for ${req.params.id}:`, error.message);
    if (!res.headersSent) {
      res.status(500).json({ success: false, message: 'Proxy stream failed', error: error.message });
    }
  }
};

// Personalized recommendations
const getRecommended = async (req, res) => {
  try {
    const userId = req.user?.id;
    let likedArtists = new Set();
    let likedGenres = new Set();
    let followedArtists = new Set();
    let recentArtists = new Set();
    let searchQueries = [];

    if (userId && mongoose.connection.readyState === 1) {
      // Get user's liked songs to find preferred artists/genres
      const favorites = await Favorite.find({ userId }).limit(50);
      favorites.forEach(fav => {
        if (fav.artist) likedArtists.add(fav.artist);
      });

      // Get followed artists
      const follows = await Follow.find({ userId }).limit(50);
      follows.forEach(f => {
        if (f.artistId) followedArtists.add(f.artistId);
      });

      // Get recent history
      const history = await History.find({ userId }).sort({ playedAt: -1 }).limit(30);
      history.forEach(h => {
        if (h.artist) recentArtists.add(h.artist);
      });
    }

    // Build search queries based on user preferences
    if (likedArtists.size > 0) {
      searchQueries = Array.from(likedArtists).slice(0, 3);
    }
    if (recentArtists.size > 0) {
      searchQueries = searchQueries.concat(Array.from(recentArtists).slice(0, 2));
    }

    // Fallback for new users with no history
    if (searchQueries.length === 0) {
      searchQueries = ['punjabi trending hits', 'new punjabi releases', 'punjabi popular songs'];
    }

    // Fetch songs for each query and build sections
    const sections = [];
    const seenSongIds = new Set();

    // Made For You - based on liked artists
    if (likedArtists.size > 0) {
      const artistName = Array.from(likedArtists)[0];
      try {
        const songs = await musicService.searchSongs(artistName, 15);
        const filtered = songs.filter(s => !seenSongIds.has(s.id));
        filtered.forEach(s => seenSongIds.add(s.id));
        if (filtered.length > 0) {
          sections.push({ title: `Because You Like ${artistName}`, songs: filtered.slice(0, 10) });
        }
      } catch(e) {}
    }

    // Based on listening
    if (recentArtists.size > 0) {
      const artistName = Array.from(recentArtists)[0];
      try {
        const songs = await musicService.searchSongs(artistName, 15);
        const filtered = songs.filter(s => !seenSongIds.has(s.id));
        filtered.forEach(s => seenSongIds.add(s.id));
        if (filtered.length > 0) {
          sections.push({ title: 'Based On Your Listening', songs: filtered.slice(0, 10) });
        }
      } catch(e) {}
    }

    // Trending / Popular
    try {
      const trending = await musicService.searchSongs('punjabi trending hits 2024', 20);
      const filtered = trending.filter(s => !seenSongIds.has(s.id));
      filtered.forEach(s => seenSongIds.add(s.id));
      if (filtered.length > 0) {
        sections.push({ title: 'Punjabi Trending Now', songs: filtered.slice(0, 10) });
      }
    } catch(e) {}

    // New releases
    try {
      const newReleases = await musicService.searchSongs('latest punjabi songs 2024', 15);
      const filtered = newReleases.filter(s => !seenSongIds.has(s.id));
      filtered.forEach(s => seenSongIds.add(s.id));
      if (filtered.length > 0) {
        sections.push({ title: 'New Punjabi Releases', songs: filtered.slice(0, 10) });
      }
    } catch(e) {}

    // Popular
    try {
      const popular = await musicService.searchSongs('top punjabi hits', 15);
      const filtered = popular.filter(s => !seenSongIds.has(s.id));
      filtered.forEach(s => seenSongIds.add(s.id));
      if (filtered.length > 0) {
        sections.push({ title: 'Popular Punjabi This Week', songs: filtered.slice(0, 10) });
      }
    } catch(e) {}

    res.json({
      success: true,
      sections
    });
  } catch (error) {
    console.error('Recommendations error:', error.message);
    res.status(500).json({ success: false, message: 'Failed to get recommendations', error: error.message });
  }
};

module.exports = {
  search,
  searchAll,
  getArtistSongs,
  getSong,
  getStream,
  proxyStream,
  getRecommended
};
