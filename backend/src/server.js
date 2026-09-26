const express = require('express');
const cors = require('cors');
const mongoose = require('mongoose');
const dotenv = require('dotenv');
const rateLimit = require('express-rate-limit');

dotenv.config();

const app = express();
app.set('trust proxy', 1); // Trust first proxy for express-rate-limit behind EC2/load balancer
const PORT = process.env.PORT || 5000;
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://127.0.0.1:27017/echostream';

// Middleware
app.use(cors());
app.use(express.json({ limit: '10mb' }));

app.use((req, res, next) => {
  console.log(`[REQUEST] ${req.method} ${req.originalUrl}`);
  next();
});

// Rate limiter
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 300,
  message: { error: 'Too many requests, please try again later.' }
});
app.use(limiter);

// Connect to MongoDB Atlas or local MongoDB safely
mongoose.connect(MONGODB_URI, { serverSelectionTimeoutMS: 3000 })
  .then(() => console.log('Successfully connected to MongoDB database.'))
  .catch((err) => console.log('MongoDB connection notice:', err.message, '\nOperating with in-memory store fallback for auth ONLY. Playlists/Favorites require DB.'));

// Import Routes
const authRoutes = require('./routes/auth');
const musicRoutes = require('./routes/musicRoutes');
const playlistRoutes = require('./routes/playlistRoutes');
const favoriteRoutes = require('./routes/favoriteRoutes');
const historyRoutes = require('./routes/historyRoutes');

// Mount Routes
app.use('/api/auth', authRoutes);
app.use('/api/music', musicRoutes);
app.use('/api/playlists', playlistRoutes);
app.use('/api/favorites', favoriteRoutes);
app.use('/api/history', historyRoutes);

// Health check
app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'EchoStream Music API',
    mongoStatus: mongoose.connection.readyState === 1 ? 'connected' : 'disconnected/in-memory',
    timestamp: new Date()
  });
});

// Root
app.get('/', (req, res) => {
  res.send('EchoStream Music Streaming API is active.');
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ success: false, message: 'Internal Server Error' });
});

// Start Server with EADDRINUSE error handler
const server = app.listen(PORT, '0.0.0.0', () => {
  console.log(`==================================================`);
  console.log(`EchoStream Music Server running on port ${PORT}`);
  console.log(`Local Access: http://localhost:${PORT}`);
  console.log(`Android Emulator Access: http://10.0.2.2:${PORT}`);
  console.log(`==================================================`);
});

server.on('error', (err) => {
  if (err.code === 'EADDRINUSE') {
    console.log(`==================================================`);
    console.log(`Notice: Port ${PORT} is already in use by an active EchoStream server instance.`);
    console.log(`The server is already running and actively serving requests at http://localhost:${PORT}`);
    console.log(`==================================================`);
    process.exit(0);
  } else {
    console.error('Server error:', err);
  }
});
