const express = require('express');
const favoriteController = require('../controllers/favoriteController');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

router.use(requireAuth);

router.get('/', favoriteController.getFavorites);
router.post('/:songId', favoriteController.addFavorite);
router.delete('/:songId', favoriteController.removeFavorite);

module.exports = router;
