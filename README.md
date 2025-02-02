# Response Differ

This Burp Suite extension adds a new tab to each response.
A tab then shows a diff between a user set "base" response and the current response.

## Installation

The project can be build using Gradle.
With a functional Docker installation the following command may be the easiest way to build the project:

```bash
docker run --rm -it -v "$PWD":/home/gradle/project -w /home/gradle/project gradle gradle build
```

If the build was successful, a Burp Suite importable extension is available under the path `build/libs/response-differ.jar`.
