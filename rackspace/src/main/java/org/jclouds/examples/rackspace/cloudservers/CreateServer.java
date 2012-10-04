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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.examples.rackspace.cloudservers.util.ServerStatusPredicate;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.domain.Flavor;
import org.jclouds.openstack.nova.v2_0.domain.Image;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.ServerCreated;
import org.jclouds.openstack.nova.v2_0.features.FlavorApi;
import org.jclouds.openstack.nova.v2_0.features.ImageApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.v2_0.domain.Resource;
import org.jclouds.predicates.RetryablePredicate;
import org.jclouds.rest.RestContext;

import com.google.common.collect.FluentIterable;

/**
 * This example creates an Ubuntu 12.04 server with 512 MB of RAM on the Rackspace open cloud. 
 *  
 * @author Everett Toews
 */
public class CreateServer {
	private static final String SERVER_NAME = "jclouds-example";
	private static final String ZONE = "DFW";
	
	private ComputeService compute;
	private RestContext<NovaApi, NovaAsyncApi> nova;

	/**
	 * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
	 * 
	 * The first argument (args[0]) must be your username
	 * The second argument (args[1]) must be your API key
	 */
	public static void main(String[] args) {
		CreateServer createServer = new CreateServer();
		
		try {
			createServer.init(args);
			createServer.createServer();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			createServer.close();
		}
	}

	private void init(String[] args) {	
		// The provider configures jclouds to use the Rackspace open cloud (US)
		// to use the Rackspace open cloud (UK) set the provider to "rackspace-cloudservers-uk"
		String provider = "rackspace-cloudservers-us";
		
		String username = args[0];
		String apiKey = args[1];
		
		ComputeServiceContext context = ContextBuilder.newBuilder(provider)
			.credentials(username, apiKey)
			.buildView(ComputeServiceContext.class);
		compute = context.getComputeService();
		nova = context.unwrap();
	}
	
	/**
	 * Create a server and block until it's ACTIVE. 
	 */
	private void createServer() throws TimeoutException {
		String imageId = getImageId();
		String flavorId = getFlavorId();
		
		System.out.println("Create Server");
		
		ServerApi serverApi = nova.getApi().getServerApiForZone(ZONE);
		ServerCreated serverCreated = serverApi.create(SERVER_NAME, imageId, flavorId);		
		RetryablePredicate<Resource> blockUntilActive = new RetryablePredicate<Resource>(
				new ServerStatusPredicate(serverApi, Server.Status.ACTIVE), 600, 10, 10, TimeUnit.SECONDS);

		if (!blockUntilActive.apply(serverCreated))
			throw new TimeoutException("Timeout on server: " + serverCreated);		
		
		Server server = serverApi.get(serverCreated.getId());

		System.out.println("  " + serverCreated);
		System.out.println("  Login IP: " + server.getAccessIPv4() +" Username: root Password: " + serverCreated.getAdminPass());
	}

	/**
	 * This method uses the generic ComputeService.listHardwareProfiles() to find the hardware profile.
	 * 
	 * @return The Flavor Id with 512 MB of RAM
	 */
	private String getFlavorId() {
		System.out.println("Flavors");
		
		FlavorApi flavorApi = nova.getApi().getFlavorApiForZone(ZONE);
		FluentIterable<? extends Flavor> flavors = flavorApi.listInDetail().concat();		
		String result = null;
		
		for (Flavor flavor: flavors) {
			System.out.println("  " + flavor);
			
			if (flavor.getRam() == 512) {
				result = flavor.getId();
			}
		}
		
		if (result == null) {
			System.err.println("Flavor with 512 MB of RAM not found. Using first flavor found.");
			result = flavors.first().get().getId();
		}
		
		return result;
	}

	/**
	 * This method uses the generic ComputeService.listImages() to find the image.
	 * 
	 * @return An Ubuntu 12.04 Image 
	 */
	private String getImageId() {
		System.out.println("Images");
		
		ImageApi imageApi = nova.getApi().getImageApiForZone(ZONE);
		FluentIterable<? extends Image> images = imageApi.listInDetail().concat();
		String result = null;
		
		for (Image image: images) {
			System.out.println("  " + image);
			
			if ("Ubuntu 12.04 LTS (Precise Pangolin)".equals(image.getName())) {
				result = image.getId();
			}
		}
		
		if (result == null) {
			System.err.println("Image with Ubuntu 12.04 operating system not found. Using first image found.");
			result = images.first().get().getId();
		}
		
		return result;
	}

	/**
	 * Always close your service when you're done with it.
	 */
	private void close() {
		if (compute != null) {
			compute.getContext().close();
		}
	}
}
