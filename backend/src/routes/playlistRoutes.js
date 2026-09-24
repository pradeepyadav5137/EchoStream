const express = require('express');
const playlistController = require('../controllers/playlistController');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

router.use(requireAuth);

router.post('/', playlistController.createPlaylist);
router.get('/', playlistController.getPlaylists);
router.get('/:id', playlistController.getPlaylist);
router.patch('/:id', playlistController.updatePlaylist);
router.delete('/:id', playlistController.deletePlaylist);

router.post('/:id/songs', playlistController.addSong);
router.delete('/:id/songs/:songId', playlistController.removeSong);

module.exports = router;
