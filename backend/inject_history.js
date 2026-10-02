const axios = require('axios');
const jwt = require('jsonwebtoken');

async function injectHistory() {
  try {
    const API_URL = 'http://16.4.39.184/api';
    const JWT_SECRET = 'echostream_super_secret_key_2024'; // Default secret in server.js
    const pradeepId = '6abd3f512fe7ee84ec972a48'; // ObjectId from the screenshot
    const pradeepEmail = 'pradeepnitt24@gmail.com';
    const pradeepUsername = 'Pradeep123';

    console.log(`Generating JWT for user ${pradeepUsername}...`);
    const token = jwt.sign(
      { id: pradeepId, username: pradeepUsername, email: pradeepEmail },
      JWT_SECRET,
      { expiresIn: '30d' }
    );

    console.log('Sending Add History request to EC2 server...');
    const histRes = await axios.post(`${API_URL}/history`, {
      songId: 'inject123',
      title: 'Injected Test Song',
      artist: 'System Diagnostic',
      album: 'Diagnostics',
      thumbnail: 'thumb_diag.jpg',
      duration: 120,
      durationPlayed: 60,
      source: 'diagnostic'
    }, {
      headers: { Authorization: `Bearer ${token}` }
    });
    console.log('History Response:', histRes.data);

    console.log('Fetching History...');
    const getHist = await axios.get(`${API_URL}/history`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    console.log(`Found ${getHist.data.history.length} history records for Pradeep123.`);
    
  } catch (error) {
    if (error.response) {
      console.log('Error:', error.response.status, error.response.data);
    } else {
      console.log('Error:', error.message);
    }
  }
}

injectHistory();
