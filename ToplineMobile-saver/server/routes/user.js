import express from "express";
import jwt from "jsonwebtoken";
import User from "../models/User.js";

const router = express.Router();

router.get("/me", async (req, res) => {
  try {
    const token = req.headers.authorization?.split(" ")[1];
    if (!token) return res.status(401).json({ message: "No token" });

    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    const user = await User.findById(decoded.userId).select("-password");

    if (!user) {
      return res.status(404).json({ message: "User not found" });
    }

    res.json({ user });
  } catch (e) {
    res.status(401).json({ message: "Invalid token" });
  }
});

router.put("/me", async (req, res) => {
  try {
    const token = req.headers.authorization?.split(" ")[1];
    if (!token) return res.status(401).json({ message: "No token" });

    const decoded = jwt.verify(token, process.env.JWT_SECRET);

    const allowedFields = {
      name: 1,
      username: 1,
      email: 1,
      phone: 1,
      bio: 1,
      location: 1,
      profilePicture: 1,
    };

    const updates = {};

    for (const key of Object.keys(req.body || {})) {
      if (allowedFields[key] !== undefined && req.body[key] !== undefined && req.body[key] !== null) {
        updates[key] = req.body[key];
      }
    }

    if (!Object.keys(updates).length) {
      return res.status(400).json({ message: "No valid profile fields provided" });
    }

    if (updates.username) {
      updates.username = updates.username.trim().toLowerCase();
    }

    if (updates.email) {
      updates.email = updates.email.trim().toLowerCase();
    }

    if (updates.name) {
      updates.name = updates.name.trim();
    }

    const user = await User.findByIdAndUpdate(
      decoded.userId,
      { $set: updates },
      { new: true, runValidators: true }
    ).select("-password");

    if (!user) {
      return res.status(404).json({ message: "User not found" });
    }

    res.json({
      success: true,
      message: "Profile updated successfully",
      user,
    });
  } catch (e) {
    console.error("Update profile error:", e);
    res.status(400).json({ message: e.message || "Profile update failed" });
  }
});

export default router;
