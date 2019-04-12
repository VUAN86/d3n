package de.ascendro.f4m.service.workflow.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.kie.api.io.Resource;
import org.kie.internal.io.ResourceFactory;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;

import de.ascendro.f4m.service.workflow.config.WorkflowConfig;

/**
 * Default implementation of {@link WorkflowResourceProvider}. Will load all resources from classpath
 * de.ascendro.f4m.service.workflow.process + all resources from filesystem folder configured with
 * workflow.resourceFolder configuration parameter.
 */
public class DefaultWorkflowResourceProvider implements WorkflowResourceProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultWorkflowResourceProvider.class);

	private static final String WORKFLOW_RESOURCE_CLASSPATH = "de.ascendro.f4m.service.workflow.process";

	private final WorkflowConfig config;

	@Inject
	public DefaultWorkflowResourceProvider(WorkflowConfig config) {
		this.config = config;
	}

	@Override
	public List<Resource> getResources() {
		List<Resource> result = new ArrayList<>();
		loadFromClasspath(result);
		loadFromFilesystem(result);
		return result;
	}

	/**
	 * Load resources from classpath.
	 */
	private void loadFromClasspath(List<Resource> result) {
		Reflections reflections = new Reflections(WORKFLOW_RESOURCE_CLASSPATH, new ResourcesScanner());
		reflections.getResources(Predicates.alwaysTrue())
				.forEach(resource -> result.add(ResourceFactory.newClassPathResource(resource)));
	}

	/**
	 * Load resources from filesystem.
	 */
	private void loadFromFilesystem(List<Resource> result) {
		String fsFolder = config.getProperty(WorkflowConfig.WORKFLOW_RESOURCE_FOLDER);
		if (StringUtils.isNotBlank(fsFolder)) {
			try {
				File folder = new File(fsFolder);
				Reflections reflections = new Reflections(new ConfigurationBuilder().addUrls(folder.toURI().toURL()).addScanners(new ResourcesScanner()));
				reflections.getResources(Predicates.alwaysTrue())
						.forEach(resource -> result.add(ResourceFactory.newFileResource(new File(folder, resource).getAbsolutePath())));
			} catch (MalformedURLException e) {
				LOGGER.error("Invalid path specified", e);
			}
		} else {
			LOGGER.debug("No filesystem resource folder configured - skipping.");
		}
	}
	
}
