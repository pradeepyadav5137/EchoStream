const express = require('express');
const musicController = require('../controllers/musicController');

const router = express.Router();

router.get('/search', musicController.search);
router.get('/song/:id', musicController.getSong);
router.get('/stream/:id', musicController.getStream);

module.exports = router;
