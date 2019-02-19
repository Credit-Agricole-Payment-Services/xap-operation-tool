package gca.in.xap.tools.operationtool;

import gca.in.xap.tools.operationtool.predicates.container.IsEmptyContainerPredicate;
import gca.in.xap.tools.operationtool.predicates.container.StatefulBackupsOnlyPredicate;
import gca.in.xap.tools.operationtool.predicates.container.StatefulPrimariesOnlyPredicate;
import gca.in.xap.tools.operationtool.predicates.pu.IsStatefulProcessingUnitPredicate;
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
import java.util.stream.Collectors;

@Slf4j
@CommandLine.Command(name = "example", version = "XAP application management command-line tool")
public class Tool implements Runnable {

	private static class CommandsListCandidates implements Iterable<String> {
		private CommandsListCandidates() {
		}

		public Iterator<String> iterator() {
			List<String> commands = Arrays.stream(Command.values()).map(Enum::name).collect(Collectors.toList());
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

		final Command commandValue = Command.valueOf(command.replace("_", "-").replace("-", "_"));

		ApplicationArguments applicationArguments = new ApplicationArguments(parameters);
		applicationArguments.printInfo();

		try {
			switch (commandValue) {
				case deploy: {
					DeployTask task = new DeployTask();
					task.executeTask(applicationArguments, wholeMode, restartEmptyContainers);
					break;
				}
				case undeploy: {
					UndeployTask task = new UndeployTask();
					task.executeTask(applicationArguments);
					break;
				}
				case shutdown_host: {
					ShutdownHostTask task = new ShutdownHostTask();
					task.executeTask(applicationArguments);
					break;
				}
				case heapdump: {
					HeapDumpTask task = new HeapDumpTask();
					task.executeTask(applicationArguments);
					break;
				}
				case restart_containers_all: {
					RestartStrategy restartStrategy = new RestartStrategy(Duration.ZERO);
					RestartContainersTask task = new RestartContainersTask(gsc -> true, restartStrategy);
					task.executeTask(applicationArguments);
					break;
				}
				case restart_containers_empty_only: {
					RestartStrategy restartStrategy = new RestartStrategy(Duration.ZERO);
					RestartContainersTask task = new RestartContainersTask(new IsEmptyContainerPredicate(), restartStrategy);
					task.executeTask(applicationArguments);
					break;
				}
				case restart_containers_stateless_only: {
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
				case restart_containers_stateful_backups_only: {
					RestartStrategy restartStrategy = new RestartStrategy(Duration.ofMinutes(1));
					RestartContainersTask task = new RestartContainersTask(new StatefulBackupsOnlyPredicate(), restartStrategy);
					task.executeTask(applicationArguments);
					break;
				}
				case restart_containers_stateful_primaries_only: {
					RestartStrategy restartStrategy = new RestartStrategy(Duration.ofMinutes(1));
					RestartContainersTask task = new RestartContainersTask(new StatefulPrimariesOnlyPredicate(), restartStrategy);
					task.executeTask(applicationArguments);
					break;
				}
				case restart_managers: {
					RestartManagersTask task = new RestartManagersTask();
					task.executeTask(applicationArguments);
					break;
				}
				case trigger_gc: {
					GarbageCollectorTask task = new GarbageCollectorTask();
					task.executeTask(applicationArguments);
					break;
				}
				default:
					List<String> supportedCommands = Arrays.stream(Command.values())
							.map(Enum::name)
							.map(name -> name.replace("_", "-"))
							.collect(Collectors.toList());
					throw new IllegalArgumentException("Unsupported command : " + command + ", supported values are : " + supportedCommands);
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

