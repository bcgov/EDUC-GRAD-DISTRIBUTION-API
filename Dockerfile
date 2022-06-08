FROM docker-remote.artifacts.developer.gov.bc.ca/maven:3-jdk-11 as build
WORKDIR /workspace/app

COPY api/pom.xml .
COPY api/src src
RUN mvn package -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

FROM docker-remote.artifacts.developer.gov.bc.ca/openjdk:11-jdk
RUN useradd -ms /bin/bash spring && mkdir -p /logs && chown -R spring:spring /logs && chmod 755 /logs
RUN --mount=type=secret,id=KNOWN_HOSTS_ENTRY \
    --mount=type=secret,id=BCMAIL_SSH_PRIVATE_KEY \
    --mount=type=secret,id=BCMAIL_SSH_PUBLIC_KEY \
    mkdir /.ssh \
    if [ -z $(cat /run/secrets/KNOWN_HOSTS_ENTRY) && -z $(cat /run/secrets/BCMAIL_SSH_PRIVATE_KEY) && -z $(cat /run/secrets/BCMAIL_SSH_PUBLIC_KEY)]; then \
    echo ${KNOWN_HOSTS_ENTRY} > /.ssh/known_hosts \
    && echo ${BCMAIL_SSH_PRIVATE_KEY} > /.ssh/id_rsa \
    && echo ${BCMAIL_SSH_PUBLIC_KEY} > /.ssh/id_rsa.pub \
    else \
    cat /run/secrets/KNOWN_HOSTS_ENTRY > /.ssh/known_hosts \
    && cat /run/secrets/BCMAIL_SSH_PRIVATE_KEY > /.ssh/id_rsa \
    && cat /run/secrets/BCMAIL_SSH_PUBLIC_KEY > /.ssh/id_rsa.pub \
    fi
RUN chown -R 1002710000:1002710000 /.ssh /.ssh/known_hosts /.ssh/id_rsa /.ssh/id_rsa.pub \
    && chmod 700 /.ssh/id_rsa
EXPOSE 22

USER spring
VOLUME /tmp
ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java","-Duser.name=EDUC_GRAD_DISTRIBUTION_API","-Xms1500m","-Xmx1500m","-noverify","-XX:TieredStopAtLevel=1",\
            "-XX:+UseParallelGC","-XX:MinHeapFreeRatio=20","-XX:MaxHeapFreeRatio=40","-XX:GCTimeRatio=4",\
            "-XX:AdaptiveSizePolicyWeight=90","-XX:MaxMetaspaceSize=300m","-XX:ParallelGCThreads=1",\
            "-Djava.util.concurrent.ForkJoinPool.common.parallelism=1","-XX:CICompilerCount=2",\
            "-XX:+ExitOnOutOfMemoryError","-Djava.security.egd=file:/dev/./urandom",\
            "-Dspring.backgroundpreinitializer.ignore=true","-cp","app:app/lib/*",\
            "ca.bc.gov.educ.api.distribution.EducDistributionApiApplication"]
