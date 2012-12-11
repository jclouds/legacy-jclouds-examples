/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.examples.rackspace.cloudservers;

import static com.google.common.io.Closeables.closeQuietly;
import static org.jclouds.compute.predicates.NodePredicates.inGroup;

import java.io.Closeable;
import java.util.Properties;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.config.ComputeServiceProperties;
import org.jclouds.compute.domain.NodeMetadata;

/**
 * This example destroys the server created in the CreateServer example. 
 *  
 * @author Everett Toews
 */
public class DeleteServer implements Closeable {
   private ComputeService compute;

   /**
    * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
    * 
    * The first argument (args[0]) must be your username
    * The second argument (args[1]) must be your API key
    */
   public static void main(String[] args) {
      DeleteServer deleteServer = new DeleteServer();

      try {
         deleteServer.init(args);
         deleteServer.deleteServer();
      }
      finally {
         deleteServer.close();
      }
   }

   private void init(String[] args) {
      // The provider configures jclouds to use the Rackspace open cloud (US)
      // to use the Rackspace open cloud (UK) set the provider to "rackspace-cloudservers-uk"
      String provider = "rackspace-cloudservers-us";

      String username = args[0];
      String apiKey = args[1];

      // These properties control how often jclouds polls for a status udpate
      Properties overrides = new Properties();
      overrides.setProperty(ComputeServiceProperties.POLL_INITIAL_PERIOD, Constants.POLL_PERIOD_TWENTY_SECONDS);
      overrides.setProperty(ComputeServiceProperties.POLL_MAX_PERIOD, Constants.POLL_PERIOD_TWENTY_SECONDS);

      ComputeServiceContext context = ContextBuilder.newBuilder(provider)
            .credentials(username, apiKey)
            .overrides(overrides)
            .buildView(ComputeServiceContext.class);
      compute = context.getComputeService();
   }

   /**
    * This will delete all servers in group {@link Constants.NAME}
    */
   private void deleteServer() {
      System.out.println("Delete Server");

      // This method will continue to poll for the server status and won't return until this server is DELETED
      // If you want to know what's happening during the polling, enable logging. See
      // /jclouds-exmaple/rackspace/src/main/java/org/jclouds/examples/rackspace/Logging.java
      Set<? extends NodeMetadata> servers = compute.destroyNodesMatching(inGroup(Constants.NAME));

      for (NodeMetadata nodeMetadata: servers) {
         System.out.println("  " + nodeMetadata);
      }
   }

   /**
    * Always close your service when you're done with it.
    */
   public void close() {
      closeQuietly(compute.getContext());
   }
}
