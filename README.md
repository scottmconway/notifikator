Notifikator
===========

This is an Android application that catches every notification and forwards them to an HTTP endpoint. It uses a freeform editor, and allows you to template fields in a JSON body for notification servers such as [ntfy](https://ntfy.sh).

How to Use
----------

1. Install the application.
2. Enable notification access (a shortcut is provided within).
3. Configure the HTTP endpoint, body format, and authentication if necessary (see next section).
4. *(Optional)* Configure the package denylist to silence noisy packages
5. *(Optional)* Send a test notification.

Package Denylist
---------
To prevent specific applications from having their notifications forwarded, tap **Manage Package Denylist** in the app. This opens a searchable list of all installed applications with checkboxes - check an app to block its notifications from being forwarded.

Freeform Body Editor
---------
The following fields can be used as placeholders to communicate notification info, wrapped by percent signs:
* title - notification title
* text - notification text
* package - the package path of the notifying application (eg. net.kzxiv.notify.client)
* app - the name of the application (eg. Notifickator)
* badge - a base64-encoded badge icon included in the notification
* displaytime - the length of time in ms that the notification is displayed for
* icon - a base64-encoded image icon included in the notification

Placeholders may be used in JSON values for the body or header fields.

For example, for [ntfy.sh](https://ntfy.sh):
```json{
    "topic": "notifikator",
    "message": "%text%",
    "title": "Notification - %app%"
}
```
