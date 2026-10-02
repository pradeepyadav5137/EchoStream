const axios = require('axios');

async function test() {
  try {
    const API_URL = 'http://16.4.39.184/api';

    console.log('Registering user...');
    const registerRes = await axios.post(`${API_URL}/auth/register`, {
      username: 'testuser' + Date.now(),
      email: 'testuser' + Date.now() + '@example.com',
      password: 'password123',
      displayName: 'Test User'
    });
    console.log('Register Response:', registerRes.data);
    
    let token = registerRes.data.token;

    console.log('Adding favorite...');
    const favRes = await axios.post(`${API_URL}/favorites/song123`, {
      title: 'Test Song',
      artist: 'Test Artist',
      album: 'Test Album',
      thumbnail: 'thumb.jpg',
      duration: 180,
      source: 'youtube'
    }, {
      headers: { Authorization: `Bearer ${token}` }
    });
    console.log('Favorite Response:', favRes.data);

    console.log('Fetching favorites...');
    const getFavs = await axios.get(`${API_URL}/favorites`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    console.log('Get Favorites Response:', getFavs.data);

  } catch (error) {
    if (error.response) {
      console.log('Error:', error.response.status, error.response.data);
    } else {
      console.log('Error:', error.message);
    }
  }
}

test();
