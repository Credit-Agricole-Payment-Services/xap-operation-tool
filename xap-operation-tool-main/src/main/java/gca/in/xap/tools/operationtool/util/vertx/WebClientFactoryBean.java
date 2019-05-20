package gca.in.xap.tools.operationtool.util.vertx;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebClientFactoryBean implements FactoryBean<WebClient>, DisposableBean {

	@Autowired
	@Lazy
	private Vertx vertx;

	private WebClient singletonInstance;

	@Override
	public WebClient getObject() {
		singletonInstance = createNewObject();
		return singletonInstance;
	}

	private WebClient createNewObject() {
		log.info("Creating instance of WebClient ...");
		WebClientOptions options = new WebClientOptions()
				.setUserAgent("xap-operation-tool")
				.setKeepAlive(false);
		return WebClient.create(vertx, options);
	}

	@Override
	public Class<WebClient> getObjectType() {
		return WebClient.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void destroy() {
		WebClient object = this.singletonInstance;
		if (object != null) {
			log.info("Closing WebClient ...");
			object.close();
		}
	}

}
