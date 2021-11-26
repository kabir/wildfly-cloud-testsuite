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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;

import io.dekorate.testing.annotation.Inject;
import io.dekorate.testing.annotation.KubernetesIntegrationTest;
import io.dekorate.testing.annotation.Named;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KubernetesIntegrationTest
public class EndpointTestCaseIT {

    @Inject
    private KubernetesClient client;

    @Inject
    private KubernetesList list;


    @Test
    public void shouldRespondWithHelloWorld() throws IOException {
        Assertions.assertNotNull(client);
        Assertions.assertNotNull(list);
        for (Pod p : client.pods().list().getItems()) {
            System.out.println(">>>" + p.getMetadata().getName());
        }
        Pod pod = client.pods().list().getItems().get(0);
        System.out.println("Using pod:" + pod.getMetadata().getName());
        System.out.println("Forwarding port");
        try (LocalPortForward p = client.services().withName("wildfly-cloud-testsuite").portForward(8080)) { //port matches what is configured in properties file
            assertTrue(p.isAlive());
            URL url = new URL("http://localhost:" + p.getLocalPort() + "/");

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().get().url(url).build();
            System.out.println(">>>" + request);
            Response response = client.newCall(request).execute();
            assertEquals(response.body().string(), "Hello world");
        }
    }
}
