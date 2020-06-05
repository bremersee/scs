# Static Content Server

[![codecov](https://codecov.io/gh/bremersee/scs/branch/develop/graph/badge.svg)](https://codecov.io/gh/bremersee/scs)

The Static Content Server is a Spring Web Flux application that serves static content from the file 
system. The property `bremersee.scs.content-location` specifies the root location of the content.
The default is `/opt/content`:  

```yaml
bremersee:
  scs:
    content-location: ${SCS_CONTENT_LOCATION:/opt/content/}
    default-index: ${SCS_INDEX:index.html}
    pattern: ${SCS_PATTERN:/**}
```

If the request uri is for example `http://localhost/somewhere/over/the/rainbow/colors.html` the
application will serve the file `/opt/content/somewhere/over/the/rainbow/colors.html`. If no file
is present in the request uri (for example `http://localhost/somewhere/over/the/rainbow`), the
application will serve the specified default index (for example 
`/opt/content/somewhere/over/the/rainbow/index.html`). 

You can define a default index for each directory.

```yaml
bremersee:
  scs:
    directory-pattern-index-map:
      [/somewhere/over/the/rainbow/**]: colors.html
      [/somewhere/over/**]: redirect.html
      [/somewhere/**]: choice.html
```

The uri `http://localhost/somewhere` will then return `/opt/content/somewhere/choice.html`.

The property `bremersee.scs.pattern` defines a request path prefix to listen on. It is stripped 
from the resource location. This will be useful, if you are running the application behind a reverse 
proxy. If the pattern is for example `/demo/**`

```yaml
bremersee:
  scs:
    pattern: /demo/**
```

and the request uri is `http://localhost/demo/somewhere/over/the/rainbow/colors.html`, the 
application will still return the file `/opt/content/somewhere/over/the/rainbow/colors.html` 
(without `/demo`).

### Docker

There is a docker image of this application available on docker hub 
[bremersee/scs](https://hub.docker.com/r/bremersee/scs). 

```bash
docker run -v /var/www/html:/opt/content -p 8080:8080 bremersee/scs:latest
```

The image can also be used as a base image of your angular application for example.

The `Dockerfile` may look like this:

```dockerfile
# build image
FROM node:12.11.1 as build

# set working directory
WORKDIR /app

# install angular cli
RUN npm install -g @angular/cli@9.1.7

# add app
COPY . /app

# install node modules
RUN npm install

# generate build
ARG NG_CONFIG=""
ARG NG_BASE_HREF=""
RUN ng build --configuration=$NG_CONFIG --baseHref $NG_BASE_HREF --output-path dist

# base image
FROM bremersee/scs:latest

# copy artifact build from the 'build environment'
COPY --from=build /app/dist /opt/content
ARG APP_NAME="app"
ARG APP_PREFIX="/**"
RUN echo "$APP_NAME" > /opt/app.name.conf
RUN echo "$APP_PREFIX" > /opt/app.prefix.conf
```

The image can be build with:

```bash
docker build -f Dockerfile --build-arg NG_CONFIG="dev" --build-arg NG_BASE_HREF="/demo/" --build-arg APP_NAME="demo" --build-arg APP_PREFIX="/demo/**" -t demo:latest .
docker run -p 4200:8080 demo:latest 
```

The angular application is then available under `http://localhost:4200/demo`.

### Maven Site

- [Release](https://bremersee.github.io/scs/index.html)

- [Snapshot](https://nexus.bremersee.org/repository/maven-sites/scs/1.0.1-SNAPSHOT/index.html)

