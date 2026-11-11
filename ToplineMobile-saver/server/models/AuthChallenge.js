import mongoose from "mongoose";

const authChallengeSchema = new mongoose.Schema(
  {
    purpose: {
      type: String,
      enum: ["password-reset", "google-signup"],
      required: true,
    },
    identifierHash: {
      type: String,
      required: true,
    },
    codeHash: {
      type: String,
      required: true,
    },
    expiresAt: {
      type: Date,
      required: true,
      expires: 0,
    },
    sentAt: {
      type: Date,
      required: true,
    },
    attempts: {
      type: Number,
      default: 0,
    },
    providerSubject: {
      type: String,
      default: "",
    },
    displayName: {
      type: String,
      default: "",
    },
    profilePicture: {
      type: String,
      default: "",
    },
  },
  { timestamps: true }
);

authChallengeSchema.index({ purpose: 1, identifierHash: 1 }, { unique: true });

export default mongoose.model("AuthChallenge", authChallengeSchema);
