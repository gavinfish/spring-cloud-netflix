/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.eureka;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.configuration.SSLContextFactory;
import org.springframework.cloud.configuration.TlsProperties;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@EnableScheduling
public class EurekaTlsDiscoveryClient implements DiscoveryClient {

	private static final Log log = LogFactory.getLog(EurekaTlsDiscoveryClient.class);

	/**
	 * Client description {@link String}.
	 */
	public static final String DESCRIPTION = "Spring Cloud Eureka Tls Discovery Client";

	private EurekaDiscoveryClient discoveryClient;

	private long lastModified;

	@Autowired
	TlsProperties tlsProperties;

	@Autowired
	private AbstractDiscoveryClientOptionalArgs args;

	@Autowired
	private ApplicationInfoManager applicationInfoManager;

	@Autowired
	private EurekaClientConfig config;

	@Scheduled(fixedRate = 3000)
	public void test() {
		Resource keyStore = tlsProperties.getKeyStore();
		log.info("refresh");
		try {
			if (keyStore != null) {
				long modified = keyStore.lastModified();
				if (modified > lastModified) {
					log.info("cert updated");
					SSLContextFactory factory = new SSLContextFactory(tlsProperties);
					args.setSSLContext(factory.createSSLContext());

					EurekaClient client = new com.netflix.discovery.DiscoveryClient(
							applicationInfoManager, config, args);
					discoveryClient = new EurekaDiscoveryClient(client, config);
				}
				lastModified = modified;
			}
		}
		catch (IOException | GeneralSecurityException e) {
			e.printStackTrace();
		}
	}

	public EurekaTlsDiscoveryClient(EurekaDiscoveryClient discoveryClient) {
		this.discoveryClient = discoveryClient;
	}

	@Override
	public String description() {
		return DESCRIPTION;
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		return discoveryClient.getInstances(serviceId);
	}

	@Override
	public List<String> getServices() {
		return discoveryClient.getServices();
	}

}
