package gca.in.xap.tools.operationtool.xapauth;

import gca.in.xap.tools.operationtool.XapClientDiscovery;
import org.openspaces.admin.pu.config.UserDetailsConfig;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class XapClientUserDetailsConfigFactoryBean implements FactoryBean<UserDetailsConfig> {

	@Autowired
	private XapClientUserDetailsConfigFactory xapClientUserDetailsConfigFactory;

	@Autowired
	private XapClientDiscovery xapClientDiscovery;

	@Override
	public UserDetailsConfig getObject() {
		return xapClientUserDetailsConfigFactory.createFromUrlEncodedValue(
				xapClientDiscovery.getUsername(),
				xapClientDiscovery.getPassword()
		);
	}

	@Override
	public Class<UserDetailsConfig> getObjectType() {
		return UserDetailsConfig.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
