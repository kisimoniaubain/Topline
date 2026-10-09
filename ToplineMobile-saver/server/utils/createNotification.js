import Notification from "../models/Notification.js";

export const createNotification = async ({ recipient, actor, type, entityId = "", text }) => {
  if (!recipient || !actor || String(recipient) === String(actor)) {
    return;
  }

  try {
    await Notification.updateOne(
      { recipient, actor, type, entityId },
      { $setOnInsert: { recipient, actor, type, entityId, text } },
      { upsert: true }
    );
  } catch (error) {
    console.error("Create notification error:", error);
  }
};
