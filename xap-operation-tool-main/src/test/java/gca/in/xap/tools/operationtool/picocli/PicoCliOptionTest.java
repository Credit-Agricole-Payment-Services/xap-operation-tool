package gca.in.xap.tools.operationtool.picocli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PicoCliOptionTest {

	@Test
	public void testIssue813() {
		class App {
			@CommandLine.Option(names = "--no-xxx", defaultValue = "true", negatable = true)
			boolean xxx;

			@CommandLine.Option(names = "--yyy", defaultValue = "false", negatable = true)
			boolean yyy;
		}

		App app;

		app = new App();
		new CommandLine(app).parseArgs("--xxx", "--yyy");
		assertTrue(app.xxx);
		assertTrue(app.yyy);

		app = new App();
		new CommandLine(app).parseArgs("--no-xxx", "--no-yyy");
		assertFalse(app.xxx);
		assertFalse(app.yyy);

		app = new App();
		new CommandLine(app).parseArgs("--no-xxx");
		assertFalse(app.xxx);
		assertFalse(app.yyy);

		app = new App();
		new CommandLine(app).parseArgs("--xxx");
		assertTrue(app.xxx);
		assertFalse(app.yyy);

		app = new App();
		new CommandLine(app).parseArgs("--no-yyy");
		assertTrue(app.xxx);
		assertFalse(app.yyy);

		app = new App();
		new CommandLine(app).parseArgs("--yyy");
		assertTrue(app.xxx);
		assertTrue(app.yyy);

	}

	@Test
	public void testIssue813_you_probably_should_not_use_it_this_way() {
		// see doc : Negatable options that are true by default
		class AppDont {
			@CommandLine.Option(names = "--zzz", defaultValue = "true", negatable = true)
			boolean zzz;
		}

		AppDont app;

		app = new AppDont();
		new CommandLine(app).parseArgs();
		assertTrue(app.zzz);

		app = new AppDont();
		new CommandLine(app).parseArgs("--zzz");
		assertFalse(app.zzz); // don't expect true

		app = new AppDont();
		new CommandLine(app).parseArgs("--no-zzz");
		assertTrue(app.zzz); // don't expect false

	}

	@Test
	public void testIssue813_but_you_probably_should_do_this() {
		// see doc : Negatable options that are true by default
		class AppDo {
			@CommandLine.Option(names = "--no-zzz", defaultValue = "true", negatable = true)
			boolean zzz;
		}

		AppDo app;

		app = new AppDo();
		new CommandLine(app).parseArgs();
		assertTrue(app.zzz);

		app = new AppDo();
		new CommandLine(app).parseArgs("--zzz");
		assertTrue(app.zzz);

		app = new AppDo();
		new CommandLine(app).parseArgs("--no-zzz");
		assertFalse(app.zzz);

	}

}
