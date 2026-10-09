import dotenv from "dotenv";
import path from "path";
import { fileURLToPath } from "url";

// Define __filename and __dirname once at the top
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Load .env from root folder before importing other modules
dotenv.config({ path: path.join(__dirname, "..", ".env") });

import express from "express";
import mongoose from "mongoose";
import cors from "cors";

import authRoutes from "./routes/auth.js";

const app = express();

app.use(cors());
app.use(express.json());

/* =========================================
   API ROUTES
========================================= */

// Auto-remove stories older than 24h
setInterval(() => console.log("Cleanup 24h stories"), 86400000);

app.get("/api/health", (req, res) => {
  res.json({
    success: true,
    message: "Topline server is running",
  });
});

app.get("/", (_req, res) => {
  res.json({
    success: true,
    message: "Topline API is live. Use /api/health to check service health.",
  });
});

app.use("/api/user", (await import("./routes/user.js")).default);
app.use("/api/auth", authRoutes);
app.use("/api/posts", (await import("./routes/posts.js")).default);
app.use("/api/comments", (await import("./routes/comments.js")).default);
app.use("/api/likes", (await import("./routes/likes.js")).default);
app.use("/api/friends", (await import("./routes/friends.js")).default);
app.use("/api/messages", (await import("./routes/messages.js")).default);
app.use("/api/notifications", (await import("./routes/notifications.js")).default);
/* =========================================
   SERVE REACT / VITE FRONTEND
========================================= */

const distPath = path.join(__dirname, "..", "dist");

app.use(express.static(distPath));

/*
  React Router fallback.
  API routes are handled above.
  All other routes return the React app.
*/

app.use((req, res, next) => {
  if (req.path.startsWith("/api/")) {
    return next();
  }

  res.sendFile(path.join(distPath, "index.html"));
});

/* =========================================
   START SERVER
========================================= */

const PORT = process.env.PORT || 5000;

const startServer = async () => {
  try {
    await mongoose.connect(process.env.MONGODB_URI);

    console.log("✅ Connected to MongoDB");

    app.listen(PORT, "0.0.0.0", () => {
      console.log(
        `🚀 Topline server running on port ${PORT}`
      );
    });
  } catch (error) {
    console.error("❌ MongoDB connection failed:");
    console.error(error.message);

    process.exit(1);
  }
};

startServer();
