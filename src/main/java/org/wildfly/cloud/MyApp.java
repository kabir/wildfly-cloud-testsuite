/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.cloud;

import static io.dekorate.kubernetes.annotation.ImagePullPolicy.Always;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import io.dekorate.docker.annotation.DockerBuild;
import io.dekorate.kubernetes.annotation.Env;
import io.dekorate.kubernetes.annotation.KubernetesApplication;
import io.dekorate.kubernetes.annotation.Port;

@KubernetesApplication(
        ports = {
                @Port(name = "web", containerPort = 8080),
                @Port(name = "admin", containerPort = 9990)
        },
        envVars = {
                @Env(name = "SERVER_PUBLIC_BIND_ADDRESS", value = "0.0.0.0"),
                @Env(name = "WILDFLY_OVERRIDING_ENV_VARS", value = "1"),
                @Env(name = "SUBSYSTEM_LOGGING_ROOT_LOGGER_ROOT__LEVEL", value = "DEBUG")
        },
        imagePullPolicy = Always)
// The properties set here are guesses
@DockerBuild(image = "wildfly/simple-test-case", enabled = false, autoPushEnabled = true, autoBuildEnabled = true)
@ApplicationPath("")
public class MyApp extends Application {

}
