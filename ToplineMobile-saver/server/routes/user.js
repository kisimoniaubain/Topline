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
    res.json({ user });
  } catch (e) { res.status(401).json({ message: "Invalid token" }); }
});
export default router;
