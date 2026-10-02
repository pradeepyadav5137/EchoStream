const axios = require('axios');
const mongoose = require('mongoose');

async function checkDatabase() {
  try {
    console.log('Connecting to local mongodb (which EC2 previously used)...');
    // I can't connect directly from my environment unless it's exposed, which it isn't.
    // But I CAN send requests to the API.

    const API_URL = 'http://16.4.39.184/api';

    console.log('Logging in with a dummy token? No, I must register.');
    const registerRes = await axios.post(`${API_URL}/auth/register`, {
      username: 'orphanUser' + Date.now(),
      email: 'orphan' + Date.now() + '@example.com',
      password: 'password123',
      displayName: 'Orphan User'
    });
    const token = registerRes.data.token;
    console.log('Registered user. Token obtained.');
    
    // Can I delete the user? The API doesn't have a delete user endpoint.
  } catch (err) {
    console.error(err.message);
  }
}
checkDatabase();
