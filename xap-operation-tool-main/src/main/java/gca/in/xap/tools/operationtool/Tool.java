package gca.in.xap.tools.operationtool;

import com.google.common.collect.Lists;
import gca.in.xap.tools.operationtool.predicates.AndPredicate;
import gca.in.xap.tools.operationtool.predicates.container.IsEmptyContainerPredicate;
import gca.in.xap.tools.operationtool.predicates.container.StatefulBackupsOnlyPredicate;
import gca.in.xap.tools.operationtool.predicates.container.StatefulPrimariesOnlyPredicate;
import gca.in.xap.tools.operationtool.predicates.pu.*;
import gca.in.xap.tools.operationtool.service.RestartStrategy;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import picocli.CommandLine;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

import java.time.Duration;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

@Slf4j
@CommandLine.Command(name = "example", mixinStandardHelpOptions = false, version = "XAP application management command-line tool")
public class Tool implements Runnable {

	private static class CommandsListCandidates implements Iterable<String> {
		private CommandsListCandidates() {
		}

		public Iterator<String> iterator() {
			List<String> commands = Lists.newArrayList("deploy", "undeploy");
			return commands.iterator();
		}
	}

	@CommandLine.Option(names = {"--help", "-h"}, usageHelp = true, description = "display this help message")
	private boolean usageHelpRequested;

	@CommandLine.Option(names = {"--version", "-v"}, versionHelp = true, description = "Show version info and exit.")
	private boolean versionInfoRequested;

	@CommandLine.Option(names = {"--whole"}, description = "Upload the application in whole")
	private boolean wholeMode;

	@CommandLine.Option(names = {"--restartEmptyContainers"}, description = "Restart all GSC that have no running Processing Unit, in order to make mitigate any memory leak")
	private boolean restartEmptyContainers;

	@CommandLine.Option(names = "-f", defaultValue = ".", description = "Path to the File or Directory that contains the application.xml descriptor. Default is current working directory.")
	private String descriptorPath;

	@CommandLine.Parameters(index = "0", arity = "1", description = "command", completionCandidates = CommandsListCandidates.class)
	private String command;

	@CommandLine.Parameters(index = "1..*", arity = "0..*", paramLabel = "FILE", description = "File(s) to process.")
	private List<String> parameters;

	public Tool() {
	}

	public void run() {
		if (usageHelpRequested) {
			// nothing to do, picocli will print the usage information and program will exit.
			System.exit(0);
		}
		if (versionInfoRequested) {
			BuildInfo.printBuildInformation();
			System.exit(0);
		}
		log.info("command = {}", command);

		ApplicationArguments applicationArguments = new ApplicationArguments(parameters);
		applicationArguments.printInfo();

		try {
			switch (command) {
				case "deploy": {
					DeployTask task = new DeployTask();
					String archiveFilename = parameters.get(0);
					task.executeTask(archiveFilename, wholeMode, restartEmptyContainers, applicationArguments);
					break;
				}
				case "undeploy": {
					String applicationName = parameters.get(0);
					UndeployTask task = new UndeployTask();
					task.executeTask(applicationArguments, applicationName);
					break;
				}
				case "shutdown-host": {
					ShutdownHostTask task = new ShutdownHostTask();
					task.executeTask(applicationArguments);
					break;
				}
				case "heapdump": {
					HeapDumpTask task = new HeapDumpTask();
					task.executeTask(applicationArguments);
					break;
				}
				case "restart-containers-all": {
					RestartStrategy restartStrategy = new RestartStrategy(Duration.ZERO);
					RestartContainersTask task = new RestartContainersTask(gsc -> true, restartStrategy);
					task.executeTask(applicationArguments);
					break;
				}
				case "restart-containers-empty-only": {
					RestartStrategy restartStrategy = new RestartStrategy(Duration.ZERO);
					RestartContainersTask task = new RestartContainersTask(new IsEmptyContainerPredicate(), restartStrategy);
					task.executeTask(applicationArguments);
					break;
				}
				case "restart-containers-stateless-only": {
					RestartStrategy restartStrategy = new RestartStrategy(Duration.ofMinutes(1));
					RestartContainersTask task = new RestartContainersTask(gsc -> {
						ProcessingUnitInstance[] processingUnitInstances = gsc.getProcessingUnitInstances();
						// if the GSC is not running any PU, then we do not want to restart it
						if (processingUnitInstances.length == 0) {
							return false;
						}
						// if the GSC is running an EmbeddedSpace, then we do not want to restart it
						// else, it means that we are only running stateless PU(s) in this GSC
						return Arrays.stream(processingUnitInstances).noneMatch(new IsStatefulProcessingUnitPredicate());
					}, restartStrategy);
					task.executeTask(applicationArguments);
					break;
				}
				case "restart-containers-stateful-backups-only": {
					RestartStrategy restartStrategy = new RestartStrategy(Duration.ofMinutes(1));
					RestartContainersTask task = new RestartContainersTask(new StatefulBackupsOnlyPredicate(), restartStrategy);
					task.executeTask(applicationArguments);
					break;
				}
				case "restart-containers-stateful-primaries-only": {
					RestartStrategy restartStrategy = new RestartStrategy(Duration.ofMinutes(1));
					RestartContainersTask task = new RestartContainersTask(new StatefulPrimariesOnlyPredicate(), restartStrategy);
					task.executeTask(applicationArguments);
					break;
				}
				case "restart-managers": {
					RestartManagersTask task = new RestartManagersTask();
					task.executeTask(applicationArguments);
					break;
				}
				case "trigger-gc": {
					GarbageCollectorTask task = new GarbageCollectorTask();
					task.executeTask(applicationArguments);
					break;
				}
				default:
					throw new IllegalArgumentException("Unsupported command : " + command);
			}
		} catch (TimeoutException | RuntimeException e) {
			log.error("Exception while procesing command {} : {} : {}", command, e.getClass().getName(), e.getMessage(), e);
			System.exit(1);
		}

		// exit with a zero code, in order to shutdown all daemon threads
		System.exit(0);
	}



	public static void main(String[] args) {
		SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
		BuildInfo.printBuildInformation();
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			log.error("Uncaught Exception in Thread {}", t.getName(), e);
			System.exit(1);
		});
		System.setProperty("com.gigaspaces.unicast.interval", "1000,1000");

		CommandLine.run(new Tool(), args);
	}

}

