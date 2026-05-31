# Chessiq Real-Time Game Server

This directory contains the lightweight Express and Socket.io server facilitating ELO-based matchmaking and real-time multiplayer chess room sessions.

## 🚀 Getting Started

### Prerequisites
- Node.js (version 16 or later)
- npm (Node Package Manager)

### Installation
From the root of the project, navigate to the `backend` directory and install dependencies:
```bash
cd backend
npm install
```

### Running the Server
Start the server in production mode:
```bash
npm start
```
Or start in development mode with live hot-reloads (using nodemon):
```bash
npm run dev
```

The server runs on port **3000** by default (http://localhost:3000).

---

## ⚡ WebSocket Events

The socket endpoint handles several real-time channels:

### 1. Matchmaking Queues
- **Client Emits**: `joinQueue` with payload `{ username: string, elo: number }`
- **Server Emits**: `matchFound` with payload `{ roomId: string, color: 'WHITE' | 'BLACK', opponent: string }`

### 2. Live Room Sync
- **Client Emits**: `playMove` with payload `{ roomId: string, from: string, to: string, promotionType?: string }`
- **Server Emits**: `moveMade` (broadcasted to opponent) with payload `{ from: string, to: string, promotionType?: string }`
- **Client Emits**: `resignGame` with payload `{ roomId: string }`
- **Server Emits**: `gameOver` with payload `{ reason: 'checkmate' | 'stalemate' | 'draw' | 'resignation', winner: 'WHITE' | 'BLACK' | 'DRAW' }`
