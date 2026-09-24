const mongoose = require('mongoose');
const dotenv = require('dotenv');
const store = require('./store');

dotenv.config();

const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://127.0.0.1:27017/echostream';

const seedSongs = [
  {
    id: 'ind_1',
    title: 'Kesariya',
    artist: 'Arijit Singh & Pritam',
    artistId: 'art_arijit',
    album: 'Brahmastra',
    albumId: 'alb_brahmastra',
    genre: 'Bollywood',
    duration: 268,
    audioUrl: 'https://jiotunepreview.jio.com/content/Converted/010910141580615.mp3',
    artworkUrl: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600',
    playCount: 95400,
    likeCount: 28900,
    isTrending: true
  },
  {
    id: 'ind_2',
    title: 'Chaleya',
    artist: 'Arijit Singh & Anirudh Ravichander',
    artistId: 'art_arijit',
    album: 'Jawan',
    albumId: 'alb_jawan',
    genre: 'Bollywood',
    duration: 200,
    audioUrl: 'https://jiotunepreview.jio.com/content/Converted/010910092002187.mp3',
    artworkUrl: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600',
    playCount: 82100,
    likeCount: 24100,
    isTrending: true
  },
  {
    id: 'ind_3',
    title: 'Tum Hi Ho',
    artist: 'Arijit Singh & Mithoon',
    artistId: 'art_arijit',
    album: 'Aashiqui 2',
    albumId: 'alb_aashiqui2',
    genre: 'Bollywood Romantic',
    duration: 262,
    audioUrl: 'https://jiotunepreview.jio.com/content/Converted/010910092419390.mp3',
    artworkUrl: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600',
    playCount: 159800,
    likeCount: 42100,
    isTrending: true
  },
  {
    id: 'ind_4',
    title: 'Apna Bana Le',
    artist: 'Arijit Singh & Sachin-Jigar',
    artistId: 'art_arijit',
    album: 'Bhediya',
    albumId: 'alb_bhediya',
    genre: 'Bollywood',
    duration: 261,
    audioUrl: 'https://jiotunepreview.jio.com/content/Converted/010910441686043.mp3',
    artworkUrl: 'https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600',
    playCount: 78200,
    likeCount: 19200,
    isTrending: false
  },
  {
    id: 'ind_5',
    title: 'Raataan Lambiyan',
    artist: 'Jubin Nautiyal & Asees Kaur',
    artistId: 'art_jubin',
    album: 'Shershaah',
    albumId: 'alb_shershaah',
    genre: 'Bollywood Romantic',
    duration: 230,
    audioUrl: 'https://jiotunepreview.jio.com/content/Converted/010910141318776.mp3',
    artworkUrl: 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600',
    playCount: 111000,
    likeCount: 38900,
    isTrending: true
  },
  {
    id: 'ind_6',
    title: 'Pasoori',
    artist: 'Ali Sethi & Shae Gill',
    artistId: 'art_alisethi',
    album: 'Coke Studio',
    albumId: 'alb_cokestudio',
    genre: 'Indie Punjabi',
    duration: 224,
    audioUrl: 'https://jiotunepreview.jio.com/content/Converted/010912023101540.mp3',
    artworkUrl: 'https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600',
    playCount: 145000,
    likeCount: 51000,
    isTrending: true
  },
  {
    id: 'ind_7',
    title: 'Lover',
    artist: 'Diljit Dosanjh',
    artistId: 'art_diljit',
    album: 'MoonChild Era',
    albumId: 'alb_moonchild',
    genre: 'Punjabi Pop',
    duration: 185,
    audioUrl: 'https://jiotunepreview.jio.com/content/Converted/010910441686043.mp3',
    artworkUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=600',
    playCount: 89000,
    likeCount: 29000,
    isTrending: true
  },
  {
    id: 'ind_8',
    title: 'Kun Faya Kun',
    artist: 'A.R. Rahman, Javed Ali & Mohit Chauhan',
    artistId: 'art_arrahman',
    album: 'Rockstar',
    albumId: 'alb_rockstar',
    genre: 'Sufi Classical',
    duration: 472,
    audioUrl: 'https://jiotunepreview.jio.com/content/Converted/010910090167886.mp3',
    artworkUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=600',
    playCount: 132000,
    likeCount: 48000,
    isTrending: true
  }
];

const seedArtists = [
  {
    id: 'art_arijit',
    name: 'Arijit Singh',
    imageUrl: 'https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600',
    bio: 'India\'s most celebrated playback singer known for soulful Bollywood classics.',
    followersCount: 1250000,
    genres: ['Bollywood', 'Romantic', 'Classical']
  },
  {
    id: 'art_arrahman',
    name: 'A.R. Rahman',
    imageUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=600',
    bio: 'Oscar & Grammy winning composer, music producer, and singer.',
    followersCount: 980000,
    genres: ['Bollywood', 'Sufi', 'Classical', 'Soundtrack']
  },
  {
    id: 'art_diljit',
    name: 'Diljit Dosanjh',
    imageUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=600',
    bio: 'Global Punjabi sensation, actor, and pop icon.',
    followersCount: 890000,
    genres: ['Punjabi Pop', 'Bhangra', 'Urban']
  }
];

const seedAlbums = [
  {
    id: 'alb_brahmastra',
    title: 'Brahmastra',
    artist: 'Pritam & Arijit Singh',
    artistId: 'art_arijit',
    artworkUrl: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600',
    releaseYear: 2022,
    genre: 'Bollywood'
  },
  {
    id: 'alb_jawan',
    title: 'Jawan',
    artist: 'Anirudh Ravichander',
    artistId: 'art_arijit',
    artworkUrl: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600',
    releaseYear: 2023,
    genre: 'Bollywood'
  },
  {
    id: 'alb_aashiqui2',
    title: 'Aashiqui 2',
    artist: 'Mithoon & Arijit Singh',
    artistId: 'art_arijit',
    artworkUrl: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600',
    releaseYear: 2013,
    genre: 'Bollywood Romantic'
  }
];

function populateStore() {
  store.songs = [...seedSongs];
  store.artists = [...seedArtists];
  store.albums = [...seedAlbums];
}

async function seed() {
  console.log('Seeding EchoStream database with Indian music catalog...');
  populateStore();

  try {
    await mongoose.connect(MONGODB_URI, { serverSelectionTimeoutMS: 2000 });
    console.log('Connected to MongoDB. Seeding collections...');
    const Song = require('./models/Song');
    const Artist = require('./models/Artist');
    const Album = require('./models/Album');

    await Song.deleteMany({});
    await Artist.deleteMany({});
    await Album.deleteMany({});

    await Song.insertMany(seedSongs);
    await Artist.insertMany(seedArtists);
    await Album.insertMany(seedAlbums);

    console.log('MongoDB Indian Music Seed successfully populated!');
    await mongoose.disconnect();
  } catch (err) {
    console.log('MongoDB connection skipped/failed, in-memory store successfully seeded!');
  }
}

if (require.main === module) {
  seed().then(() => process.exit(0));
} else {
  populateStore();
}

module.exports = { seed, populateStore };
