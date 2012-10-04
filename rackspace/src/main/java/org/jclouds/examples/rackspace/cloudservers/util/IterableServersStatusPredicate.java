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
package org.jclouds.examples.rackspace.cloudservers.util;

import java.util.HashMap;
import java.util.Map;

import org.jclouds.logging.Logger;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.v2_0.domain.Resource;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

/**
 * Tests to see if ALL servers have reached status. This class is most useful when paired with a RetryablePredicate as
 * in the code below. These classes are being used to block execution until all Server statuses have reached ACTIVE.
 * This is useful when your Servers need to be 100% ready before you can continue with execution.
 * 
 * <pre>
 * {@code
 * Set<Resource> servers = new HashSet<Resource>();
 * 
 * for (int i = 0; i < 3; i++) {
 *     ServerCreated server = serverApi.create(serverName, imageId, flavorId);
 *     servers.add(server);
 * }
 *	
 * RetryablePredicate<Iterable<Resource>> blockUntilActive = new RetryablePredicate<Iterable<Resource>>(
 *     new IterableServersStatusPredicate(serverApi, Server.Status.ACTIVE), 600, 10, 10, TimeUnit.SECONDS);
 *
 * if (!blockUntilActive.apply(servers))
 *     throw new TimeoutException("timeout waiting for servers to run: " + servers);
 * }
 * </pre>
 * 
 * @author Everett Toews
 */
public class IterableServersStatusPredicate implements Predicate<Iterable<Resource>> {
   private final ServerApi serverApi;
   private final Server.Status status; 

   @javax.annotation.Resource
   protected Logger logger = Logger.NULL;

   public IterableServersStatusPredicate(ServerApi serverApi, Server.Status status) {
      this.serverApi = serverApi;
      this.status = status;
   }

   /**
    * @param servers Works with an Iterable set of Server or ServerCreated objects
    * @return boolean Return true when ALL servers reach status, false otherwise
    */
   public boolean apply(Iterable<Resource> servers) {
     FluentIterable<? extends Server> serversUpdated = serverApi.listInDetail().concat();     
     Map<String, Server> serversUpdatedMap = new HashMap<String, Server>();
     
     for (Server serverUpdated: serversUpdated) {
        serversUpdatedMap.put(serverUpdated.getId(), serverUpdated);
     }
     
     for (Resource server: servers) {
        Server serverUpdated = serversUpdatedMap.get(server.getId());
        
        logger.trace("looking for server: %s status: %s current: %s",
                     server.getId(), status, serverUpdated.getStatus());

        if (!status.equals(serverUpdated.getStatus())) {
           return false;
        }
     }
     
     return true;
   }
}
