const { v4: uuidv4 } = require('uuid');

// Matchmaking queues
let queue = [];

// Room storage (maps roomId -> Room Details)
const activeRooms = new Map();

// Maps socket.id -> active roomId (for quick lookups during disconnect)
const playerToRoom = new Map();

function setupSocketHandlers(io) {
    io.on('connection', (socket) => {
        console.log(`[Socket] Client connected: ${socket.id}`);

        // 1. Join matchmaking queue
        socket.on('joinQueue', (data) => {
            const { username, elo } = data;
            console.log(`[Matchmaking] ${username} (ELO ${elo}) joined the queue.`);

            // Avoid double joining
            queue = queue.filter(p => p.socketId !== socket.id);

            // Check if we can match
            if (queue.length > 0) {
                // Simple FIFO matching
                const opponent = queue.shift();
                const roomId = uuidv4();

                // Determine random colors
                const random = Math.random() < 0.5;
                const whitePlayer = random ? opponent : { socketId: socket.id, username, elo };
                const blackPlayer = random ? { socketId: socket.id, username, elo } : opponent;

                const roomDetails = {
                    roomId,
                    white: whitePlayer,
                    black: blackPlayer,
                    moves: [],
                    status: 'active'
                };

                activeRooms.set(roomId, roomDetails);
                playerToRoom.set(whitePlayer.socketId, roomId);
                playerToRoom.set(blackPlayer.socketId, roomId);

                // Join socket rooms
                const whiteSocket = io.sockets.sockets.get(whitePlayer.socketId);
                const blackSocket = io.sockets.sockets.get(blackPlayer.socketId);

                if (whiteSocket) whiteSocket.join(roomId);
                if (blackSocket) blackSocket.join(roomId);

                // Notify White
                io.to(whitePlayer.socketId).emit('matchFound', {
                    roomId,
                    color: 'WHITE',
                    opponent: blackPlayer.username,
                    opponentElo: blackPlayer.elo
                });

                // Notify Black
                io.to(blackPlayer.socketId).emit('matchFound', {
                    roomId,
                    color: 'BLACK',
                    opponent: whitePlayer.username,
                    opponentElo: whitePlayer.elo
                });

                console.log(`[Matchmaking] Room ${roomId} created. ${whitePlayer.username} vs ${blackPlayer.username}`);
            } else {
                queue.push({
                    socketId: socket.id,
                    username,
                    elo: elo || 1200
                });
            }
        });

        // 2. Play Move
        socket.on('playMove', (data) => {
            const { roomId, from, to, promotionType } = data;
            const room = activeRooms.get(roomId);

            if (room && room.status === 'active') {
                const move = { from, to, promotionType, timestamp: new Date() };
                room.moves.push(move);

                // Broadcast move to the opponent in the room
                socket.to(roomId).emit('moveMade', move);
                console.log(`[Game] Room ${roomId}: Move from ${from} to ${to}`);
            }
        });

        // 3. Resign Game
        socket.on('resignGame', (data) => {
            const { roomId } = data;
            const room = activeRooms.get(roomId);

            if (room && room.status === 'active') {
                room.status = 'ended';
                
                // Find out who resigned
                const resigningColor = socket.id === room.white.socketId ? 'WHITE' : 'BLACK';
                const winnerColor = resigningColor === 'WHITE' ? 'BLACK' : 'WHITE';

                io.to(roomId).emit('gameOver', {
                    reason: 'resignation',
                    winner: winnerColor
                });

                cleanUpRoom(roomId);
                console.log(`[Game] Room ${roomId} ended. Resignation by ${resigningColor}`);
            }
        });

        // 4. Offer / Accept Draw
        socket.on('drawOffer', (data) => {
            const { roomId } = data;
            socket.to(roomId).emit('drawOffered');
        });

        socket.on('drawResponse', (data) => {
            const { roomId, accepted } = data;
            if (accepted) {
                const room = activeRooms.get(roomId);
                if (room) {
                    room.status = 'ended';
                    io.to(roomId).emit('gameOver', {
                        reason: 'drawAgreement',
                        winner: 'DRAW'
                    });
                    cleanUpRoom(roomId);
                    console.log(`[Game] Room ${roomId} ended by draw agreement.`);
                }
            } else {
                socket.to(roomId).emit('drawDeclined');
            }
        });

        // 5. Disconnection
        socket.on('disconnect', () => {
            console.log(`[Socket] Client disconnected: ${socket.id}`);

            // Remove from matchmaking queue
            queue = queue.filter(p => p.socketId !== socket.id);

            // Handle active game recovery
            const roomId = playerToRoom.get(socket.id);
            if (roomId) {
                const room = activeRooms.get(roomId);
                if (room && room.status === 'active') {
                    room.status = 'ended';
                    
                    // Notify the other player
                    socket.to(roomId).emit('gameOver', {
                        reason: 'opponentDisconnect',
                        winner: socket.id === room.white.socketId ? 'BLACK' : 'WHITE'
                    });

                    console.log(`[Game] Room ${roomId} ended because player disconnected.`);
                }
                cleanUpRoom(roomId);
            }
        });
    });
}

function cleanUpRoom(roomId) {
    const room = activeRooms.get(roomId);
    if (room) {
        playerToRoom.delete(room.white.socketId);
        playerToRoom.delete(room.black.socketId);
        activeRooms.delete(roomId);
    }
}

module.exports = setupSocketHandlers;
