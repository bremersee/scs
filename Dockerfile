FROM openjdk:11-jdk-slim
MAINTAINER Christian Bremer <bremersee@googlemail.com>
ARG JAR_FILE
ADD target/${JAR_FILE} /opt/app.jar
COPY docker/entrypoint.sh /opt/entrypoint.sh
RUN chmod 755 /opt/entrypoint.sh
RUN mkdir /opt/log
RUN mkdir /opt/content
COPY docker/index.html /opt/content/index.html
EXPOSE 80
ENTRYPOINT ["/opt/entrypoint.sh"]
