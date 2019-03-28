package gca.in.xap.tools.operationtool.util;

import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;

@Slf4j
public class ZipUtil {

	public static void unzip(File archiveFile, File destinationDirectory) {
		try {
			log.info("Unzipping file {} to {} ...", archiveFile.getAbsolutePath(), destinationDirectory.getAbsolutePath());
			ZipFile zipFile = new ZipFile(archiveFile);
			zipFile.extractAll(destinationDirectory.getPath());
		} catch (ZipException e) {
			throw new RuntimeException("Exception while unzipping file", e);
		}
	}

}
