package gca.in.xap.tools.operationtool;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

@Slf4j
public class BuildInfo {

	public static void printBuildInformation() {
		try {
			String message = "Version Info : " + findVersionInfo("xap-operation-tool-main");
			log.info(message);
		} catch (IOException e) {
			log.warn("Failed to find build time in MANIFEST.MF", e);
		}
	}

	public static String findVersionInfo(String applicationName) throws IOException {
		Enumeration<URL> resources = Thread.currentThread().getContextClassLoader()
				.getResources("META-INF/MANIFEST.MF");
		while (resources.hasMoreElements()) {
			URL manifestUrl = resources.nextElement();
			Manifest manifest = new Manifest(manifestUrl.openStream());
			Attributes mainAttributes = manifest.getMainAttributes();
			String implementationTitle = mainAttributes.getValue("Implementation-Title");
			if (implementationTitle != null && implementationTitle.equals(applicationName)) {
				String implementationVersion = mainAttributes.getValue("Implementation-Version");
				String buildTime = mainAttributes.getValue("Build-Time");
				String buildAuthor = mainAttributes.getValue("Built-By");
				return implementationVersion + " (" + buildTime + ") builded by '" + buildAuthor + "'";
			}
		}
		return "Unreleased Version (not built by Maven)";
	}

}
