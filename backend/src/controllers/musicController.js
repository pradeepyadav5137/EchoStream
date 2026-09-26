const axios = require('axios');
const musicService = require('../services/musicService');

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

module.exports = {
  search,
  getSong,
  getStream,
  proxyStream
};
