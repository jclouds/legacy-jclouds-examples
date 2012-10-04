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

import static org.jclouds.scriptbuilder.domain.Statements.exec;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.options.RunScriptOptions;
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
import org.jclouds.predicates.InetSocketAddressConnect;
import org.jclouds.predicates.RetryablePredicate;
import org.jclouds.rest.RestContext;
import org.jclouds.scriptbuilder.ScriptBuilder;
import org.jclouds.scriptbuilder.domain.OsFamily;
import org.jclouds.sshj.config.SshjSshClientModule;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HostAndPort;
import com.google.inject.Module;

/**
 * This example will create a server, start a webserver on it, and publish a page on the internet!
 */
public class CloudServersPublish {
	private static final String SERVER_NAME = "jclouds-example-publish";
	private static final String ZONE = "DFW";
	private static final String USER = "root";
	
	private ComputeService compute;
	private RestContext<NovaApi, NovaAsyncApi> nova;

	/**
	 * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
	 * 
	 * The first argument (args[0]) must be your username
	 * The second argument (args[1]) must be your API key
	 */
	public static void main(String[] args) {
		CloudServersPublish cloudServersPublish = new CloudServersPublish();
		
		try {
			cloudServersPublish.init(args);
			Map<String, String> serverInfo = cloudServersPublish.createServer();
			cloudServersPublish.configureAndStartWebserver(serverInfo);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			cloudServersPublish.close();
		}
	}

	private void init(String[] args) {	
		// The provider configures jclouds to use the Rackspace open cloud (US)
		// to use the Rackspace open cloud (UK) set the provider to "rackspace-cloudservers-uk"
		String provider = "rackspace-cloudservers-us";
		
		String username = args[0];
		String apiKey = args[1];

		Iterable<Module> modules = ImmutableSet.<Module> of(
				new SshjSshClientModule());

		ComputeServiceContext context = ContextBuilder.newBuilder(provider)
			.credentials(username, apiKey)
			.modules(modules)
			.buildView(ComputeServiceContext.class);
		compute = context.getComputeService();
		nova = context.unwrap();
	}
	
	/**
	 * Create a server and block until it's ACTIVE. 
	 */
	private Map<String, String> createServer() throws TimeoutException {
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

		System.out.println("  " + server);
		
		return ImmutableMap.<String, String> of(
			"serverId", server.getId(), 
			"ip", server.getAccessIPv4(), 
			"password", serverCreated.getAdminPass());
	}

	/**
	 * Configure and start a web server with a web page on the server. 
	 */
	private void configureAndStartWebserver(Map<String, String> serverInfo) throws TimeoutException {
		System.out.println("Configure And Start Webserver");

		// Wait for ssh to start
		RetryablePredicate<HostAndPort> blockUntilSSHReady = new RetryablePredicate<HostAndPort>(
			new InetSocketAddressConnect(), 300, 1, 1, TimeUnit.SECONDS);

		if (!blockUntilSSHReady.apply(HostAndPort.fromParts(serverInfo.get("ip"), 22)))
			throw new TimeoutException("Timeout on ssh: " + serverInfo.get("ip"));
		
		String script = new ScriptBuilder()
			.addStatement(exec("yum -y install httpd"))
			.addStatement(exec("/usr/sbin/apachectl start"))
			.addStatement(exec("iptables -I INPUT -p tcp --dport 80 -j ACCEPT"))
			.addStatement(exec("echo 'Hello Cloud Servers' > /var/www/html/index.html"))
			.render(OsFamily.UNIX);
		
		RunScriptOptions options = RunScriptOptions.Builder
			.overrideLoginUser(USER)
			.overrideLoginPassword(serverInfo.get("password"))
			.blockOnComplete(true);
		compute.runScriptOnNode(ZONE + "/" + serverInfo.get("serverId"), script, options);
		
		System.out.println("  Login IP: " + serverInfo.get("ip") + " Username: " + USER + " Password: " + serverInfo.get("password"));
		System.out.println("  Go to http://" + serverInfo.get("ip"));
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
			
			if ("CentOS 6.3".equals(image.getName())) {
				result = image.getId();
			}
		}
		
		if (result == null) {
			System.err.println("Image with CentOS 6.3 operating system not found. Using first image found.");
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
