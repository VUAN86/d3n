package de.ascendro.f4m.service.workflow.utils;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.kie.api.io.Resource;

import de.ascendro.f4m.service.workflow.config.WorkflowConfig;

public class DefaultWorkflowResourceProviderTest {

	private WorkflowConfig config;
	private WorkflowResourceProvider provider;

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private File file;
	
	@Before
	public void setUp() throws Exception {
		config = new WorkflowConfig();
		provider = new DefaultWorkflowResourceProvider(config);
		file = new File(folder.getRoot(), "testLoadFromFs.bpmn");
		file.createNewFile();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testLoadFromClasspath() {
		List<Resource> resources = provider.getResources();
		assertThat(resources, hasItems(
				hasProperty("path", equalTo("de/ascendro/f4m/service/workflow/process/testClasspathBpmn.bpmn"))));
		assertThat(resources, not(hasItem(hasProperty("file", equalTo(file.getAbsolutePath())))));
	}
	
	@Test
	public void testLoadFromFs() {
		config.setProperty(WorkflowConfig.WORKFLOW_RESOURCE_FOLDER, folder.getRoot().getAbsolutePath());
		List<Resource> resources = provider.getResources();
		assertThat(resources, hasItem(hasProperty("file", equalTo(file))));
	}

}
