const axios = require('axios');

async function testFullAndroidFlow() {
  try {
    const API_URL = 'http://16.4.39.184/api';

    console.log('1. Registering user...');
    const registerRes = await axios.post(`${API_URL}/auth/register`, {
      username: 'androidSim' + Date.now(),
      email: 'androidsim' + Date.now() + '@example.com',
      password: 'password123',
      displayName: 'Android Sim'
    });
    const token = registerRes.data.token;
    console.log('Token:', token ? 'Received' : 'None');

    console.log('2. Adding Favorite exactly like Android...');
    const favPayload = {
      title: 'Android Song',
      artist: 'Android Artist',
      album: 'Android Album',
      thumbnail: 'android_thumb.jpg',
      duration: 180,
      source: 'youtube'
    };
    try {
      const favRes = await axios.post(`${API_URL}/favorites/androidSong123`, favPayload, {
        headers: { Authorization: `Bearer ${token}` }
      });
      console.log('Fav Response:', favRes.data.success);
    } catch (e) { console.error('Fav Error:', e.response?.data || e.message); }

    console.log('3. Adding History exactly like Android...');
    const histPayload = {
      songId: 'androidSong123',
      title: 'Android Song',
      artist: 'Android Artist',
      album: 'Android Album',
      thumbnail: 'android_thumb.jpg',
      duration: 180,
      durationPlayed: 0
    };
    try {
      const histRes = await axios.post(`${API_URL}/history`, histPayload, {
        headers: { Authorization: `Bearer ${token}` }
      });
      console.log('Hist Response:', histRes.data.success);
    } catch (e) { console.error('Hist Error:', e.response?.data || e.message); }

    console.log('4. Creating Playlist exactly like Android...');
    const playlistPayload = {
      name: 'My New Playlist',
      description: '',
      isPublic: true,
      artworkUrl: 'https://images.unsplash.com/photo-1614680376573-df3480f0c6ff?w=500'
    };
    try {
      const plRes = await axios.post(`${API_URL}/playlists`, playlistPayload, {
        headers: { Authorization: `Bearer ${token}` }
      });
      console.log('Playlist Response:', plRes.data.success);
    } catch (e) { console.error('Playlist Error:', e.response?.data || e.message); }

  } catch (error) {
    console.error('Fatal Error:', error.response?.data || error.message);
  }
}

testFullAndroidFlow();
