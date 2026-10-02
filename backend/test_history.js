const axios = require('axios');

async function testHistory() {
  try {
    const API_URL = 'http://16.4.39.184/api';

    console.log('Registering user to test History...');
    const registerRes = await axios.post(`${API_URL}/auth/register`, {
      username: 'historyTester' + Date.now(),
      email: 'history' + Date.now() + '@example.com',
      password: 'password123',
      displayName: 'History Tester'
    });
    const token = registerRes.data.token;
    console.log('Registered user ID:', registerRes.data.user.id);

    console.log('Adding History...');
    const histRes = await axios.post(`${API_URL}/history`, {
      songId: 'song999',
      title: 'History Song',
      artist: 'History Artist',
      album: 'History Album',
      thumbnail: 'thumb2.jpg',
      duration: 200,
      durationPlayed: 150,
      source: 'jiosaavn'
    }, {
      headers: { Authorization: `Bearer ${token}` }
    });
    console.log('History Response:', histRes.data);

    console.log('Fetching History...');
    const getHist = await axios.get(`${API_URL}/history`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    console.log('Get History Response:', getHist.data);
    
  } catch (error) {
    if (error.response) {
      console.log('Error:', error.response.status, error.response.data);
    } else {
      console.log('Error:', error.message);
    }
  }
}

testHistory();
