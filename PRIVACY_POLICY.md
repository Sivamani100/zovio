Privacy Policy (Draft)

This Privacy Policy explains what data the Zovio Announcer app (`com.zovio.announcer`) accesses, why, and how it is used.

1. What we access
- Notifications: the app listens to incoming notifications to detect UPI/payment notifications so it can announce payment receipts aloud. The app does not modify or cancel other apps' notifications.
- SMS (optional): the app may read SMS messages when the user grants `RECEIVE_SMS` or `READ_SMS` permissions to detect bank credit messages and announce them.
- Local storage: the app stores minimal payment metadata (amount, sender, timestamp) locally in a Room database for display inside the app.

2. Why we access this data
- To detect incoming payment receipts and announce them via local text-to-speech so users get audible confirmation.
- To provide a transaction history and analytics inside the app.

3. Data retention
- All data is stored locally on the user's device only. We do not send or upload personal data to external servers.
- Users can clear stored payments from the app settings at any time.

4. Opt-out and permissions
- Users can revoke notification access and SMS permissions from system settings at any time.
- The app provides settings to disable announcements or to mute the TTS output.

5. Contact
- For privacy-related questions contact: privacy@example.com

Note: Update the contact email and host this policy at a stable URL; Play Console requires a hosted privacy policy URL when requesting SMS/Notification permissions.
