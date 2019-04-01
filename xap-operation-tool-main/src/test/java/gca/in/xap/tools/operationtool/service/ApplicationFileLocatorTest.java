package gca.in.xap.tools.operationtool.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

@Slf4j
public class ApplicationFileLocatorTest {

	@Test
	public void should_locate_application_archive_in_delivery_targz() throws IOException {
		ApplicationFileLocator locator = new ApplicationFileLocator();
		File result = locator.locateApplicationFile("src/test/resources/hello-fake-delivery.tar.gz:xap/pu/hello.zip");
		log.info("result = {}", result);
	}

}
