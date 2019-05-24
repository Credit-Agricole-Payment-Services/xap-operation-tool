package gca.in.xap.tools.operationtool.util.tempfiles;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

@Slf4j
public class TempFilesUtils {

	@Getter
	public static final TempFilesUtils singleton = new TempFilesUtils();

	private final File outputDirectory;

	public TempFilesUtils() {
		String username = System.getProperty("user.name");
		String outputDirectoryName = "/tmp/xot_" + username;
		outputDirectory = new File(outputDirectoryName);
	}

	public File createTempFile(String prefix, String suffix) {
		boolean outputDirectoryCreated = outputDirectory.mkdirs();
		log.debug("outputDirectoryCreated = {}", outputDirectoryCreated);
		//
		File result;
		try {
			result = File.createTempFile(prefix, suffix, outputDirectory);
		} catch (IOException e) {
			throw new RuntimeException("Failed to create a temp directory in " + outputDirectory.getAbsolutePath(), e);
		}
		result.deleteOnExit();
		return result;
	}

	public File createTempDirectory(String prefix, String suffix) {
		File result = createTempFile(prefix, suffix);
		// we need to remove the file that was created, and create a directory instead
		result.delete();
		result.mkdirs();
		result.deleteOnExit();
		//
		return result;
	}

}
