import Friend from "../models/Friend.js";
import User from "../models/User.js";

export const isPostVisibleToViewer = ({
  isPrivateAccount,
  authorId,
  viewerId,
  isFollowing = false,
}) =>
  !isPrivateAccount ||
  String(authorId) === String(viewerId) ||
  (Boolean(viewerId) && isFollowing);

export const canViewPost = async (post, viewerId) => {
  const authorId = post?.author?._id || post?.author;
  if (!authorId) return false;

  const author = await User.findById(authorId).select("_id privateAccount").lean();
  if (!author) return false;
  const isFollowing = author.privateAccount && viewerId
    ? Boolean(await Friend.exists({ follower: viewerId, following: author._id }))
    : false;
  return isPostVisibleToViewer({
    isPrivateAccount: author.privateAccount,
    authorId: author._id,
    viewerId,
    isFollowing,
  });
};
