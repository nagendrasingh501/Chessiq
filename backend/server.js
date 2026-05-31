const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const setupSocketHandlers = require('./socketHandler');

const app = express();
const port = process.env.PORT || 3000;

// Enable CORS for frontend clients
app.use(cors({
    origin: '*', // Allow all origins for dynamic mobile/web client matching
    methods: ['GET', 'POST']
}));

app.use(express.json());

// Health Check Endpoint
app.get('/health', (req, res) => {
    res.status(200).json({
        status: 'OK',
        timestamp: new Date(),
        uptime: process.uptime()
    });
});

// Root Info Endpoint
app.get('/', (req, res) => {
    res.status(200).send('♟ Chessiq WebSocket Server version 1.0.0 is active.');
});

// Create HTTP server
const server = http.createServer(app);

// Initialize Socket.io
const io = new Server(server, {
    cors: {
        origin: '*',
        methods: ['GET', 'POST']
    }
});

// Attach socket handlers
setupSocketHandlers(io);

// Start listening
server.listen(port, () => {
    console.log(`[Server] Chessiq server running on port ${port}`);
});
