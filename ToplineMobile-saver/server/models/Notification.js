import mongoose from "mongoose";

const notificationSchema = new mongoose.Schema(
  {
    recipient: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
      index: true,
    },
    actor: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      default: null,
    },
    type: {
      type: String,
      enum: ["like", "comment", "follow", "message", "post"],
      required: true,
    },
    entityId: {
      type: String,
      default: "",
    },
    text: {
      type: String,
      required: true,
      maxlength: 500,
    },
    isRead: {
      type: Boolean,
      default: false,
    },
  },
  { timestamps: true }
);

notificationSchema.index({ recipient: 1, createdAt: -1 });
notificationSchema.index(
  { recipient: 1, actor: 1, type: 1, entityId: 1 },
  { unique: true, partialFilterExpression: { actor: { $type: "objectId" } } }
);

const Notification = mongoose.model("Notification", notificationSchema);

export default Notification;