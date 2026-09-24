const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const mongoose = require('mongoose');
const User = require('../models/User');
const store = require('../store');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();
const JWT_SECRET = process.env.JWT_SECRET || 'echostream_super_secret_jwt_key_2025_safe_and_long';

// Register
router.post('/register', async (req, res) => {
  try {
    const { username, email, password, displayName } = req.body;
    if (!username || !email || !password) {
      return res.status(400).json({ error: 'Username, email and password are required' });
    }

    const hashedPassword = await bcrypt.hash(password, 10);

    let userObj;
    if (mongoose.connection.readyState === 1) {
      const existingUser = await User.findOne({ $or: [{ email }, { username }] });
      if (existingUser) {
        return res.status(400).json({ error: 'Username or email already exists' });
      }
      const newUser = new User({
        username,
        email,
        password: hashedPassword,
        displayName: displayName || username,
        avatarUrl: `https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500`
      });
      await newUser.save();
      userObj = { id: newUser._id.toString(), username: newUser.username, email: newUser.email, displayName: newUser.displayName, avatarUrl: newUser.avatarUrl };
    } else {
      // In-memory fallback
      const existing = store.users.find(u => u.email === email || u.username === username);
      if (existing) {
        return res.status(400).json({ error: 'Username or email already exists' });
      }
      userObj = {
        id: 'usr_' + Date.now(),
        username,
        email,
        password: hashedPassword,
        displayName: displayName || username,
        avatarUrl: `https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500`
      };
      store.users.push(userObj);
    }

    const token = jwt.sign({ id: userObj.id, username: userObj.username, email: userObj.email }, JWT_SECRET, { expiresIn: '30d' });

    res.status(201).json({
      message: 'User registered successfully',
      token,
      user: {
        id: userObj.id,
        username: userObj.username,
        email: userObj.email,
        displayName: userObj.displayName,
        avatarUrl: userObj.avatarUrl
      }
    });
  } catch (err) {
    console.error('Register error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Login
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }

    let userObj;
    if (mongoose.connection.readyState === 1) {
      userObj = await User.findOne({ email });
      if (!userObj) {
        return res.status(401).json({ error: 'Invalid email or password' });
      }
      const match = await bcrypt.compare(password, userObj.password);
      if (!match) {
        return res.status(401).json({ error: 'Invalid email or password' });
      }
      userObj = { id: userObj._id.toString(), username: userObj.username, email: userObj.email, displayName: userObj.displayName, avatarUrl: userObj.avatarUrl };
    } else {
      const user = store.users.find(u => u.email === email);
      if (!user) {
        return res.status(401).json({ error: 'Invalid email or password' });
      }
      const match = await bcrypt.compare(password, user.password);
      if (!match) {
        return res.status(401).json({ error: 'Invalid email or password' });
      }
      userObj = { id: user.id, username: user.username, email: user.email, displayName: user.displayName, avatarUrl: user.avatarUrl };
    }

    const token = jwt.sign({ id: userObj.id, username: userObj.username, email: userObj.email }, JWT_SECRET, { expiresIn: '30d' });

    res.json({
      message: 'Login successful',
      token,
      user: userObj
    });
  } catch (err) {
    console.error('Login error:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Get current user profile
router.get('/me', requireAuth, async (req, res) => {
  try {
    let userObj;
    if (mongoose.connection.readyState === 1) {
      const u = await User.findById(req.user.id).select('-password');
      if (!u) return res.status(404).json({ error: 'User not found' });
      userObj = { id: u._id.toString(), username: u.username, email: u.email, displayName: u.displayName, avatarUrl: u.avatarUrl };
    } else {
      const u = store.users.find(x => x.id === req.user.id);
      if (!u) return res.status(404).json({ error: 'User not found' });
      userObj = { id: u.id, username: u.username, email: u.email, displayName: u.displayName, avatarUrl: u.avatarUrl };
    }
    res.json(userObj);
  } catch (err) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Logout
router.post('/logout', (req, res) => {
  res.json({ message: 'Logged out successfully' });
});

module.exports = router;
