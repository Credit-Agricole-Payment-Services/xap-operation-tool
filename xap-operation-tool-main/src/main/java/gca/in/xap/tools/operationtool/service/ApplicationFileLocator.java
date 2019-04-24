package gca.in.xap.tools.operationtool.service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.zip.GZIPInputStream;

@Slf4j
@Component
public class ApplicationFileLocator {

	private static final String columnSeparator = ":";

	private File outputDirectory = new File("/tmp/xot/applicationArchives/");

	@NonNull
	public File locateApplicationFile(String applicationFilePath) throws IOException {
		log.info("Searching for Application file in {} ...", applicationFilePath);
		File applicationFile = new File(applicationFilePath);
		if (applicationFilePath.contains(columnSeparator)) {
			String[] filesPath = applicationFilePath.split(columnSeparator);
			if (filesPath.length != 2) {
				throw new FileNotFoundException("File not found : " + applicationFile + ", unsupported depth for embedded archive " + applicationFilePath);
			}
			String archiveFilePath = filesPath[0];
			String entryFilePath = filesPath[1];
			File archiveFile = new File(archiveFilePath);
			if (!archiveFile.exists()) {
				throw new FileNotFoundException("File not found : " + archiveFile.getAbsolutePath());
			}
			return locateApplicationFileInArchive(archiveFile, entryFilePath);
		} else {
			if (applicationFile.exists()) {
				return applicationFile;
			}
			throw new FileNotFoundException("File not found : " + applicationFile);
		}
	}

	@NonNull
	private File locateApplicationFileInArchive(File gzippedTarArchiveFile, String entryFilePath) throws IOException {
		try (TarArchiveInputStream tarInput = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(gzippedTarArchiveFile)))) {
			TarArchiveEntry currentEntry = tarInput.getNextTarEntry();
			while (currentEntry != null) {
				String currentEntryName = currentEntry.getName();
				log.debug("currentEntryName : {}", currentEntryName);
				if (currentEntryName.equals(entryFilePath)) {
					log.info("Found matching Entry in tar.gz : {}", currentEntryName);
					outputDirectory.mkdirs();
					File destinationFile = File.createTempFile("archive_file_", "", outputDirectory);
					destinationFile.deleteOnExit();
					try (FileOutputStream destinationFileOutputStream = new FileOutputStream(destinationFile)) {
						IOUtils.copy(tarInput, destinationFileOutputStream);
					}
					return destinationFile;
				} else {
					IOUtils.copy(tarInput, NullOutputStream.NULL_OUTPUT_STREAM);
				}
				currentEntry = tarInput.getNextTarEntry();
			}
		}
		throw new FileNotFoundException("Entry " + entryFilePath + " not found in archive");
	}

}
