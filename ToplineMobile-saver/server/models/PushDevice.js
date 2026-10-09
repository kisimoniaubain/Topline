import mongoose from "mongoose";

const pushDeviceSchema = new mongoose.Schema(
  {
    user: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
      index: true,
    },
    token: {
      type: String,
      required: true,
      unique: true,
    },
    platform: {
      type: String,
      enum: ["android"],
      required: true,
    },
    lastRegisteredAt: {
      type: Date,
      default: Date.now,
    },
  },
  { timestamps: true }
);

const PushDevice = mongoose.model("PushDevice", pushDeviceSchema);

export default PushDevice;
