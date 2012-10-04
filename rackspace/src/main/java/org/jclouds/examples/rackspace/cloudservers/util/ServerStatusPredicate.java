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

import org.jclouds.logging.Logger;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.v2_0.domain.Resource;

import com.google.common.base.Predicate;

/**
 * Tests to see if server has reached status. This class is most useful when paired with a RetryablePredicate as
 * in the code below. These classes are being used to block execution until the Server status has reached ACTIVE.
 * This is useful when your Server needs to be 100% ready before you can continue with execution.
 *
 * <pre>
 * {@code
 * ServerCreated serverCreated = serverApi.create(serverName, imageId, flavorId);
 * RetryablePredicate<Resource> blockUntilActive = new RetryablePredicate<Resource>(
 *     new ServerStatusPredicate(serverApi, Server.Status.ACTIVE), 600, 10, 10, TimeUnit.SECONDS);
 * 
 * if (!blockUntilActive.apply(serverCreated))
 *     throw new TimeoutException("Timeout on server: " + serverCreated);		
 * }
 * </pre>
 * 
 * @author Everett Toews
 */
public class ServerStatusPredicate implements Predicate<Resource> {
   private final ServerApi serverApi;
   private final Server.Status status;    

   @javax.annotation.Resource
   protected Logger logger = Logger.NULL;

   public ServerStatusPredicate(ServerApi serverApi, Server.Status status) {
      this.serverApi = serverApi;
      this.status = status;
   }

   /**
    * @param server Works with a Server or ServerCreated object
    * @return boolean Return true when the server reaches status, false otherwise
    */
   public boolean apply(Resource server) {
     Server serverUpdated = serverApi.get(server.getId());
     
     logger.trace("looking for server: %s status: %s current: %s",
                  server.getId(), status, serverUpdated.getStatus());
     
     return status.equals(serverUpdated.getStatus());
   }
}
