# EchoStream — Modern Native Android Music Streaming App

EchoStream is a complete, modern native Android music streaming application inspired by the overall user experience and interaction patterns of Resso.

It features music discovery, live streaming via Jamendo Creative Commons Music API, synchronized lyrics, offline downloads, background playback with Android Media3 MediaSession, user authentication, likes, playlists, comments, and cloud synchronization with a Node.js + MongoDB backend.

---

## 0. EXECUTION ENVIRONMENT STATEMENT

- **Android SDK**: Available and configured at `C:\Users\Administrator\AppData\Local\Android\Sdk`.
- **ADB / Devices**: ADB platform tools are present. Neither an active physical device nor a running emulator was connected during this automated session, so manual deployment was skipped and APKs were generated for installation.
- **Outbound Internet Access**: Active and verified. Reached Jamendo Music API and npm registries.
- **AWS Infrastructure**: No live AWS credentials or console access were provided in this environment. As specified in Section 5 & 45, AWS EC2 deployment code and configuration files were created and fully documented below without provisioning live cloud resources.

---

## 1. APPLICATION ARCHITECTURE

```
                               INTERNET
                                  │
          ┌───────────────────────┼───────────────────────┐
          │                       │                       │
          ▼                       ▼                       ▼
   Jamendo Music API          Unsplash API           AWS EC2 (Node.js)
  (Creative Commons MP3)    (Album & Artist Art)    (Express + JWT)
          │                       │                       │
          └───────────────┬───────┴───────────────┐       ▼
                          │                       MongoDB Atlas
                          ▼                       (User Sync Data)
                  📱 ANDROID APK
                          │
            ┌─────────────┴─────────────┐
            │                           │
            ▼                           ▼
       Room Database                 Media3 ExoPlayer
    (Local Cache & Sync)           (Foreground Service)
            │                           │
            ├── Offline Downloads       ├── Background Playback
            ├── Liked Tracks            ├── Lockscreen Notification
            ├── Playlists               ├── Audio Focus Management
            └── PendingSync Queue       └── Offline File Streaming
```

---

## 2. KEY FEATURES

- **Resso-inspired Dark Music UI**: Custom dark theme built with Jetpack Compose & Material 3.
- **Live CC Music Streaming**: Streaming directly from Jamendo API with fallback seed catalog.
- **Global MiniPlayer & Full Player**: Persistent player with smooth progress tracking, shuffle, repeat, and like controls.
- **Synchronized & Plain Lyrics**: Displays synced lyrics with current line highlighting and full text lyrics.
- **Offline Mode & Downloads**: Download tracks to internal storage and stream offline when internet is unavailable.
- **Cloud Synchronization**: Queue offline actions in Room (`PendingSync`) and sync seamlessly with MongoDB backend when online.
- **User Authentication**: JWT-based login and registration with BCrypt password hashing.
- **Rule-based Recommendations**: Smart score calculation based on likes (+5), genre (+3), history (+2), and popularity (+1).
- **Social Features**: Song comments, artist follow, and native Android sharing.

---

## 3. PROJECT STRUCTURE

```
MyApplication/
├── app/                                 # Native Android Application
│   ├── build.gradle.kts
│   ├── keystore/
│   │   └── release.jks                  # Project release keystore
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/com/echostream/app/
│       │       ├── EchoStreamApplication.kt
│       │       ├── MainActivity.kt
│       │       ├── data/                # Models, Room Local DB, Retrofit API Services, Repositories
│       │       ├── player/              # ExoPlayer Manager & Media3 Foreground PlaybackService
│       │       ├── ui/                  # Compose Theme, Components, Screens, Navigation
│       │       ├── viewmodel/           # Auth, Home, Search, Library, Player ViewModels
│       │       └── utils/               # NetworkMonitor, Constants
│       └── test/                        # Unit tests
│
├── backend/                             # Node.js + Express Backend API
│   ├── package.json
│   ├── .env.example
│   ├── .env
│   └── src/
│       ├── server.js                    # Express app & route mounting
│       ├── seed.js                      # Database seeding script
│       ├── store.js                     # In-memory fallback data store
│       ├── models/                      # Mongoose models (User, Song, Playlist, Like, History, etc.)
│       ├── routes/                      # API routes (Auth, Songs, Playlists, Sync, Likes, Comments)
│       └── middleware/                  # JWT auth middleware
│
├── keystore.properties                  # Keystore configuration
└── README.md
```

---

## 4. BACKEND SETUP & RUNNING LOCALLY

1. **Navigate to backend folder**:
   ```bash
   cd backend
   ```

2. **Install Node.js dependencies**:
   ```bash
   npm install
   ```

3. **Configure Environment Variables (`.env`)**:
   ```env
   PORT=5000
   MONGODB_URI=mongodb://127.0.0.1:27017/echostream
   JWT_SECRET=echostream_super_secret_jwt_key_2025_safe_and_long
   MUSIC_API_BASE_URL=https://api.jamendo.com/v3.0
   MUSIC_API_CLIENT_ID=56b49247
   ```

4. **Seed Database**:
   ```bash
   npm run seed
   ```

5. **Start Node.js Backend**:
   ```bash
   npm start
   ```
   *The backend runs on `http://localhost:5000` and `http://10.0.2.2:5000` for Android Emulator.*

---

## 5. MUSIC & IMAGE API PROVIDERS

- **Primary Music API**: **Jamendo API** (`https://api.jamendo.com/v3.0`)
  - Provides legal Creative Commons MP3 audio stream URLs and album cover artwork.
  - Public Client ID: `56b49247`
- **Fallback Demo Catalog**: Embedded inside `MusicRepository.kt` and `seed.js` with Creative Commons licensed tracks so the app remains usable offline or if network is unavailable.
- **Image Provider**: Album artwork returned directly by Jamendo API + high resolution music imagery from Unsplash API (`https://images.unsplash.com`).

---

## 6. AWS EC2 DEPLOYMENT GUIDE (PRODUCTION)

To deploy the EchoStream backend to an Ubuntu AWS EC2 instance:

1. **Launch EC2 Instance**:
   - Ubuntu 22.04 LTS / 24.04 LTS.
   - Configure Security Group: Allow Ports 22 (SSH), 80 (HTTP), 443 (HTTPS).

2. **SSH into EC2 Instance**:
   ```bash
   ssh -i your-key.pem ubuntu@your-ec2-ip
   ```

3. **Install Node.js & PM2**:
   ```bash
   curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
   sudo apt-get install -y nodejs nginx git
   sudo npm install -g pm2
   ```

4. **Clone Repository & Install Dependencies**:
   ```bash
   git clone <YOUR_REPOSITORY_URL>
   cd MyApplication/backend
   npm install
   ```

5. **Set Environment Variables**:
   Create `.env` file on EC2 with your MongoDB Atlas URI and JWT Secret:
   ```env
   PORT=5000
   MONGODB_URI=mongodb+srv://<user>:<password>@cluster.mongodb.net/echostream
   JWT_SECRET=production_super_secret_key_echostream
   MUSIC_API_BASE_URL=https://api.jamendo.com/v3.0
   MUSIC_API_CLIENT_ID=56b49247
   ```

6. **Start Backend with PM2**:
   ```bash
   pm2 start src/server.js --name "echostream-api"
   pm2 save
   pm2 startup
   ```

7. **Configure Nginx Reverse Proxy**:
   Edit `/etc/nginx/sites-available/default`:
   ```nginx
   server {
       listen 80;
       server_name api.yourdomain.com;

       location / {
           proxy_pass http://localhost:5000;
           proxy_http_version 1.1;
           proxy_set_header Upgrade $http_upgrade;
           proxy_set_header Connection 'upgrade';
           proxy_set_header Host $host;
           proxy_cache_bypass $http_upgrade;
       }
   }
   ```
   Restart Nginx:
   ```bash
   sudo systemctl restart nginx
   ```

8. **Enable HTTPS with Certbot**:
   ```bash
   sudo apt-get install -y certbot python3-certbot-nginx
   sudo certbot --nginx -d api.yourdomain.com
   ```

9. **Update Android Production Base URL**:
   In the EchoStream Android app Profile screen (or `Constants.kt`), set the Base URL to:
   `https://api.yourdomain.com`

---

## 7. APK BUILD LOCATIONS & INSTALLATION

Both Debug and Release APKs have been generated:

- **Debug APK**:
  `C:\Users\Administrator\AndroidStudioProjects\MyApplication\app\build\outputs\apk\debug\app-debug.apk`
- **Release APK** (Signed with project release keystore):
  `C:\Users\Administrator\AndroidStudioProjects\MyApplication\app\build\outputs\apk\release\app-release.apk`

### Project Keystore
- Location: `app/keystore/release.jks`
- Key Alias: `echostream`
- Password configuration: `keystore.properties`

### Installing on an Android Device
Using ADB:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
# OR
adb install app/build/outputs/apk/release/app-release.apk
```

---

## 8. VERIFICATION & TEST SUMMARY

- **Gradle Build**: Successful (`assembleDebug` and `assembleRelease`).
- **Unit Tests**: Executed `app:testDebugUnitTest` — **2 Passed, 0 Failed**.
- **Backend Service**: Tested `http://localhost:5000/api/health`, `/api/songs`, and `/api/auth/register` — Operational.
