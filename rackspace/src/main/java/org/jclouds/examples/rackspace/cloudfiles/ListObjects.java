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
package org.jclouds.examples.rackspace.cloudfiles;

import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.openstack.swift.CommonSwiftAsyncClient;
import org.jclouds.openstack.swift.CommonSwiftClient;
import org.jclouds.openstack.swift.domain.ObjectInfo;
import org.jclouds.openstack.swift.options.ListContainerOptions;
import org.jclouds.rest.RestContext;

/**
 * List objects in the object storage container from the CreateContainer example.
 *  
 * @author Everett Toews
 */
public class ListObjects {
	private static final String CONTAINER = "jclouds-example";
	
	private BlobStore storage;
	private RestContext<CommonSwiftClient, CommonSwiftAsyncClient> swift;

	/**
	 * To get a username and API key see http://www.jclouds.org/documentation/quickstart/rackspace/
	 * 
	 * The first argument (args[0]) must be your username
	 * The second argument (args[1]) must be your API key
	 */
	public static void main(String[] args) {
		ListObjects listContainers = new ListObjects();
		
		try {
			listContainers.init(args);
			listContainers.listObjects();
			listContainers.listObjectsWithFiltering();
		} 
		finally {
			listContainers.close();
		}
	}

	private void init(String[] args) {
		// The provider configures jclouds to use the Rackspace open cloud
		String provider = "cloudfiles-us";
		
		String username = args[0];
		String apiKey = args[1];
		
		BlobStoreContext context = ContextBuilder.newBuilder(provider)
			.credentials(username, apiKey)
			.buildView(BlobStoreContext.class);		
		storage = context.getBlobStore();
		swift = context.unwrap();
	}

	private void listObjects() {
		System.out.println("List Objects");
		
		Set<ObjectInfo> objects = swift.getApi().listObjects(CONTAINER);
		
		for (ObjectInfo objectInfo: objects) {
			System.out.println("  " + objectInfo);
		}
	}

	private void listObjectsWithFiltering() {
		System.out.println("List Objects With Filtering");
		
		ListContainerOptions filter = ListContainerOptions.Builder.withPrefix("createObjectFromString");
		Set<ObjectInfo> objects = swift.getApi().listObjects(CONTAINER,  filter);
		
		for (ObjectInfo objectInfo: objects) {
			System.out.println("  " + objectInfo);
		}
	}

	/**
	 * Always close your service when you're done with it.
	 */
	private void close() {
		if (storage != null) {
			storage.getContext().close();
		}
	}
}
