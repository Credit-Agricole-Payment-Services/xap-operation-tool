package gca.in.xap.tools.operationtool.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * This is the Model to represent a report for a Heap Dump of a GSC (a JVM).
 * The GSC is a JVM that has some characteristics, also, we want to know what Processing Units are running inside the GSC when the Heap Dump was taken
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HeapDumpReport {

	private String gscId;

	private String hostName;

	private String hostAddress;

	private Long pid;

	private String heapDumpFileName;

	private ZonedDateTime startTime;

	private List<String> processingUnitsNames;

}
