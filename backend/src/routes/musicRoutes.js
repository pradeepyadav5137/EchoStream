const express = require('express');
const musicController = require('../controllers/musicController');
const { authenticateToken } = require('../middleware/auth');

const router = express.Router();

router.get('/search', musicController.search);
router.get('/search-all', musicController.searchAll);
router.get('/artist-songs', musicController.getArtistSongs);
router.get('/song/:id', musicController.getSong);
router.get('/stream/:id', musicController.getStream);
router.get('/proxy-stream/:id', musicController.proxyStream);
router.get('/recommended', authenticateToken, musicController.getRecommended);

module.exports = router;
