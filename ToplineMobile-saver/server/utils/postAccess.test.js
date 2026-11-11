import test from "node:test";
import assert from "node:assert/strict";
import { isPostVisibleToViewer } from "./postAccess.js";

test("public account posts are visible to signed-out and signed-in viewers", () => {
  assert.equal(isPostVisibleToViewer({
    isPrivateAccount: false,
    authorId: "creator",
  }), true);
  assert.equal(isPostVisibleToViewer({
    isPrivateAccount: false,
    authorId: "creator",
    viewerId: "visitor",
  }), true);
});

test("private account posts are visible only to the owner and followers", () => {
  const base = { isPrivateAccount: true, authorId: "creator" };
  assert.equal(isPostVisibleToViewer(base), false);
  assert.equal(isPostVisibleToViewer({ ...base, viewerId: "visitor" }), false);
  assert.equal(isPostVisibleToViewer({ ...base, viewerId: "creator" }), true);
  assert.equal(isPostVisibleToViewer({
    ...base,
    viewerId: "follower",
    isFollowing: true,
  }), true);
});
