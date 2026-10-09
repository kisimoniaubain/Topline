import mongoose from "mongoose";

const friendSchema = new mongoose.Schema(
  {
    follower: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },
    following: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },
  },
  {
    timestamps: true,
  }
);

friendSchema.index({ follower: 1, following: 1 }, { unique: true });

const Friend = mongoose.model("Friend", friendSchema);

export default Friend;
