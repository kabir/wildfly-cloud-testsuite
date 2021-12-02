package org.wildfly.cloud.management;/*
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jboss.dmr.ModelNode;

public class TestHelper {
    static boolean waitUntilWildFlyIsReady(KubernetesClient k8sClient, String podName, String containerName, long delay) {
        long start = System.currentTimeMillis();
        try {
            try (LocalPortForward p = k8sClient.services().withName("wildfly-cloud-testsuite").portForward(9990)) { //port matches what is configured in properties file
                assertTrue(p.isAlive());
                URL url = new URL("http://localhost:" + p.getLocalPort() + "/health/ready");

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().get().url(url)
                        .header("Connection", "close")
                        .build();
                Response response = client.newCall(request).execute();
                if (response.code() == 200) {
                    String log = k8sClient.pods().withName(podName).inContainer(containerName).getLog();
                    if (log.contains("WFLYSRV0025")) {
                        return true;
                    }
                }
                long spent = System.currentTimeMillis() - start;
                if (spent < delay) {
                    try {
                        Thread.sleep(delay / 10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
        }
        return false;
    }

    static ModelNode executeCLICommands(KubernetesClient client, String podName, String containerName, String... commands) {
        String bashCmdTemplate = String.format("$JBOSS_HOME/bin/jboss-cli.sh  -c --commands=\"%s\"", Arrays.stream(commands).collect(Collectors.joining(",")));
        final CountDownLatch execLatch = new CountDownLatch(1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AtomicBoolean errorDuringExecution = new AtomicBoolean(false);
        client.pods().withName(podName).inContainer(containerName)
                //.readingInput(System.in)
                .writingOutput(out)
                .writingError(System.err)
                //..withTTY()
                .usingListener(new ExecListener() {
                    @Override
                    public void onOpen(Response response) {
                    }

                    @Override
                    public void onFailure(Throwable throwable, Response response) {
                        errorDuringExecution.set(true);
                        execLatch.countDown();
                    }

                    @Override
                    public void onClose(int i, String s) {
                        execLatch.countDown();
                    }
                }).exec( "bash", "-c", bashCmdTemplate);
        try {
            boolean ok = execLatch.await(10, TimeUnit.SECONDS);
            assertTrue(ok, "CLI Commands timed out");
            assertFalse(errorDuringExecution.get());
        } catch (InterruptedException e) {
        }
        ModelNode result = ModelNode.fromString(out.toString());
        return result;
    }

    static ModelNode checkOperation(boolean mustSucceed, ModelNode result) {
        assertEquals(mustSucceed, "success".equals(result.get("outcome").asString()));
        return result.get("result");
    }
}
