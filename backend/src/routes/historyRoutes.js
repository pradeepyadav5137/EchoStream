const express = require('express');
const historyController = require('../controllers/historyController');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

router.use(requireAuth);

router.get('/', historyController.getHistory);
router.post('/', historyController.addHistory);
router.delete('/', historyController.clearHistory);

module.exports = router;
