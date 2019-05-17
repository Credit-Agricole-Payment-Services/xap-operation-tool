package gca.in.xap.tools.operationtool.util.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class VertxFactoryBean implements FactoryBean<Vertx>, BeanNameAware, DisposableBean {

	private static final AtomicInteger instanceCounter = new AtomicInteger(0);

	@Setter
	private Integer workerPoolSize;

	@Setter
	private Integer internalBlockingPoolSize;

	@Setter
	private String beanName;

	private Vertx singletonInstance;

	@Override
	public Vertx getObject() {
		singletonInstance = createNewObject();
		return singletonInstance;
	}

	private Vertx createNewObject() {
		VertxJsonMapperConfigurer.configure();

		VertxOptions options = new VertxOptions();
		if (workerPoolSize != null) {
			options.setWorkerPoolSize(workerPoolSize);
		}
		if (internalBlockingPoolSize != null) {
			options.setInternalBlockingPoolSize(internalBlockingPoolSize);
		}
		//String jmxDomain = beanName + "-" + instanceCounter.incrementAndGet();
		//options.setMetricsOptions(new DropwizardMetricsOptions().setJmxEnabled(true).setJmxDomain(jmxDomain));

		Vertx vertx = Vertx.vertx(options);
		vertx.exceptionHandler(throwable -> log.error("Error in Vertx", throwable));

		return vertx;
	}

	@Override
	public Class<Vertx> getObjectType() {
		return Vertx.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void destroy() {
		Vertx object = this.singletonInstance;
		if (object != null) {
			object.close(voidAsyncResult -> log.info("Successfully closed Vertx instance"));
		}
	}

}
