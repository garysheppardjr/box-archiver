# Box Archiver

Box Archiver provides a web service that, when called, archives files to [Box](https://www.box.com), provided you login with an account that has access to the destination Box folder.

# Installation

1. Login to the [Box Developer Portal](https://developer.box.com/) to register a new app. You will need the app's client ID and client secret later.
  - If you plan to deploy the app as https://host.domain.com/box-archiver:
    - Set the redirect URI to be https://host.domain.com/box-archiver/archive.
    - Add https://host.domain.com to the CORS domains allowed origins.
  - The app needs permission to "Read and write all files and folders stored in Box."
1. Clone this repo.
1. Adjust the initialization parameter values in `src/main/webapp/WEB-INF/web.xml`.
1. Build using Maven: `mvn install`.
1. Deploy the resulting web application to the servlet container of your choice, such as Tomcat.

# Usage

If you deploy the web app as https://host.domain.com/box-archiver, load the following URL in your browser to start archiving:

https://host.domain.com/box-archiver/archive?minagedays=30&preserve=false

The preceding URL will first challenge you for Box credentials and then archive files that are at least 30 days old, deleting them from disk when they are successfully uploaded to Box.

Parameters:

- `minagedays`: Files at least `minagedays` old will be archived to Box. Files newer than that will not. The default value far predates the discovery of practical uses of electricity, let alone computers, so **`minagedays` is virtually required**, and omitting it will likely have no effect.
- `preserve`: If `preserve` is `false`, the files will be deleted from disk after successfully being copied to Box. If `true`, the files will stay on disk and also be copied to Box. **`preserve` is optional, and the default is `false`**.

# Contributions

Contributions are welcome, subject to [this project's license](LICENSE).
