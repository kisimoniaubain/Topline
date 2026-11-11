import mongoose from "mongoose";

const userSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: true,
      trim: true,
    },

    username: {
      type: String,
      required: true,
      trim: true,
      lowercase: true,
    },

    email: {
      type: String,
      required: true,
      trim: true,
      lowercase: true,
    },

    phone: {
      type: String,
      default: "",
      trim: true,
    },

    privateAccount: {
      type: Boolean,
      default: false,
    },

    lastActiveAt: {
      type: Date,
      default: null,
    },

    password: {
      type: String,
      required: false,
    },

    dateOfBirth: {
      type: Date,
      required: false,
      default: null,
    },

    federatedIdentities: {
      type: [{
        provider: { type: String, enum: ["google", "facebook"], required: true },
        subject: { type: String, required: true },
      }],
      default: [],
    },

    gender: {
      type: String,
      default: "",
      trim: true,
    },

    profilePicture: {
      type: String,
      default: "",
    },

    bio: {
      type: String,
      default: "",
    },

    location: {
      type: String,
      default: "",
    },
  },
  {
    timestamps: true,
  }
);

userSchema.index(
  { "federatedIdentities.provider": 1, "federatedIdentities.subject": 1 },
  { unique: true, sparse: true },
);

const User = mongoose.model("User", userSchema);

export default User;