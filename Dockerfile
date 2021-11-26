FROM quay.io/jfdenise/wildfly-runtime-jdk11:latest
COPY --chown=jboss:root target/server $JBOSS_HOME
RUN chmod -R ug+rwX $JBOSS_HOME