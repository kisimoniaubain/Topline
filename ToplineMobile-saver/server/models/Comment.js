import mongoose from "mongoose";

const commentSchema = new mongoose.Schema(
	{
		post: {
			type: mongoose.Schema.Types.ObjectId,
			ref: "Post",
			required: true,
			index: true,
		},
		author: {
			type: mongoose.Schema.Types.ObjectId,
			ref: "User",
			required: true,
		},
		text: {
			type: String,
			default: "",
			trim: true,
			maxlength: 1000,
		},
		image: {
			type: String,
			default: "",
		},
		parentComment: {
			type: mongoose.Schema.Types.ObjectId,
			ref: "Comment",
			default: null,
			index: true,
		},
		likes: [{
			type: mongoose.Schema.Types.ObjectId,
			ref: "User",
		}],
	},
	{ timestamps: true }
);

const Comment = mongoose.model("Comment", commentSchema);

export default Comment;
