Notifikator
===========

This is an Android application that catches every notification and forwards them to an HTTP endpoint.

It natively supports talking to the [Kodi](https://kodi.tv/) JSON-RPC interface, [Notifications for Android TV](https://play.google.com/store/apps/details?id=de.cyberdream.androidtv.notifications.google), [Gotify](https://gotify.net/), as well as sending plain JSON.

How to Use
----------

1. Install the application.
2. Enable notification access (a shortcut is provided within).
3. Configure the HTTP endpoint, protocol and authentication if necessary (see next section).
4. *(Optional)* Configure the package denylist to silence noisy packages
5. *(Optional)* Send a test notification.

Package Denylist
---------
In order to deny specific applications from having their notifications forwarded, first get a list of your packages. This is simply obtained from running `adb shell pm list packages`. Applications' package names are also shown under the application's settings in the system settings application.

Format your package names in a text file with one package name per line. Remove the `package:` prefix if copy-pasting from the output of `pm list packages`. If generating this on a computer, `adb push` can be used to send this text file to your android device.

Click `Package Denylist File` in the app and select your denylist file.

Changes to the content of this file won't be automatically detected by the app - you must reload the file for any changes to take effect.

The packages specified in the denylist need not be installed on the device - they simply won't do anything if the given package never sends a notification.

Protocols
---------

### Kodi

This protocol sends a call to show a notification through the Kodi JSON-RPC interface.

In order for this to work, the "Allow remote control via HTTP" setting must be turned on in Settings -> Services -> Control in Kodi.

The endpoint URL should look like `http://hostname:8080/jsonrpc`, and authentication should be enabled and matching the username and password configured in Kodi.

### Kodi with Addon

This is a variant on the Kodi protocol. Sadly, the `GUI.ShowNotification` call takes URLs for images, and will not accept `data:` URIs, therefore the notification icon can not be sent that way.

This variant requires you to install the `script.notifikator.zip` addon, which will save the received image as a temporary file, and then show the notification.

Otherwise, the endpoint configuration should be the same as with the Kodi protocol.

**Privacy issue:** Every received notification icon will stay stored in `~/.kodi/temp/notifikator` forever. This may include profile pictures or other private images.

### Notifications for Android TV

This protocol forwards the notification to the server part of the [Notifications for Android TV](https://play.google.com/store/apps/details?id=de.cyberdream.androidtv.notifications.google) application. No customization options are available, unlike in the real application.

The endpoint URL should look like `http://hostname:7676/`, and authentication should not be enabled.

### Gotify

This protocol simply POSTs a formatted JSON to the supplied URL. It expects a Gotify server's URL in this format: `https://gotify.example.com/message?token=$APP_TOKEN`. As of now, the `title` parameter is hard-coded to `Notifikator`, and `priority` to `5`.

### JSON

This protocol will simply `POST` a JSON containing the notification title, text, small and large icons to an arbitrary endpoint. This is designed for people who want to write their own server code to handle it.

The JSON format is intentionally similar to the parameters of the [`ServiceWorkerRegistration.showNotification`](https://developer.mozilla.org/en-US/docs/Web/API/ServiceWorkerRegistration/showNotification) API, so in theory the contents could be used as-is. In practice, the combined size of the text and icon image data will likely exceed the maximum WebPush size and not go through.

Here is an example JSON, note that any of the fields not present in the original notification will not be present in the JSON. Additionally, an empty `options` object will be omitted.

```json
{
    "title": "Notification Title",
    "options": {
        "body": "Notification text.",
        "badge": "data:image/png;base64,...",
        "icon": "data:image/png;base64,..."
    }
}
```
