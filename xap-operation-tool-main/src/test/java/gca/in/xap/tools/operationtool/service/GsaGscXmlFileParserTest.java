package gca.in.xap.tools.operationtool.service;

import org.junit.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GsaGscXmlFileParserTest {

	@Test
	public void should_parse_valid_file() {
		GsaGscXmlFileParser parser = new GsaGscXmlFileParser();

		InputStream inputStream = getClass().getResourceAsStream("/gsa_gsc/gsc_LARGE_01.xml");
		String result = parser.extractXPath(inputStream, GsaGscXmlFileParser.expression);

		String expectedResult = "${XAP_GSC_OPTIONS} -Xloggc:${LOG_HOME}/sctinst/gc-log-gsc_LARGE_01.log -Xms5G -Xmx5G -DappInstanceId=gsc_LARGE_01 -Dcom.gs.zones=ZONE_A,DAL,LARGE_HEAP,LARGE_01  -javaagent:/app/in/bin/jmx_prometheus_javaagent.jar=9020:/app/in/etc/jmx-exporter.yml";
		assertEquals(expectedResult, result);
	}

}
