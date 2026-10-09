import Notification from "../models/Notification.js";
import { sendPushNotification } from "./sendPushNotification.js";

export const createNotification = async ({ recipient, actor, type, entityId = "", text }) => {
  if (!recipient || !actor || String(recipient) === String(actor)) {
    return;
  }

  try {
    const result = await Notification.updateOne(
      { recipient, actor, type, entityId },
      { $setOnInsert: { recipient, actor, type, entityId, text } },
      { upsert: true }
    );
    if (result.upsertedCount > 0) {
      void sendPushNotification({
        userId: recipient,
        title: "Topline",
        body: text,
        data: {
          notificationType: type,
          entityId: String(entityId),
          notificationId: String(result.upsertedId),
        },
      }).catch((error) => {
        console.error("Send push notification error:", error);
      });
    }
  } catch (error) {
    console.error("Create notification error:", error);
  }
};
