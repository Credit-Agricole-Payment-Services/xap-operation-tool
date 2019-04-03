package gca.in.xap.tools.operationtool;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

@Slf4j
public class BuildInfo {

	@Data
	public static class BuildVersionInfo {

		private String implementationVersion;
		private String buildTime;
		private String buildAuthor;

		public String toDisplayString() {
			return implementationVersion + " (" + buildTime + ") builded by '" + buildAuthor + "'";
		}

	}

	public static void printBuildInformation() {
		log.info(findVersionInfoString());
	}

	public static String findVersionInfoString() {
		Optional<BuildVersionInfo> versionInfo = null;
		try {
			versionInfo = findVersionInfo();
		} catch (IOException e) {
			log.warn("Failed to find build time in MANIFEST.MF", e);
			throw new RuntimeException(e);
		}
		String message;
		message = "Version Info : " + versionInfo.map(BuildVersionInfo::toDisplayString).orElse("Unreleased Version (not built by Maven)");
		return message;
	}

	public static Optional<BuildVersionInfo> findVersionInfo() throws IOException {
		return findVersionInfo("xap-operation-tool-main");
	}

	public static Optional<BuildVersionInfo> findVersionInfo(String applicationName) throws IOException {
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
				//
				BuildVersionInfo result = new BuildVersionInfo();
				result.setImplementationVersion(implementationVersion);
				result.setBuildTime(buildTime);
				result.setBuildAuthor(buildAuthor);
				return Optional.of(result);
			}
		}
		return Optional.empty();
	}

}
