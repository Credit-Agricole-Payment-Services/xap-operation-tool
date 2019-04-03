package gca.in.xap.tools.operationtool.xapauth;

import org.openspaces.admin.pu.config.UserDetailsConfig;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

@Component
public class XapClientUserDetailsConfigFactory {

	public UserDetailsConfig createFromUrlEncodedValue(String userLogin, String userPass) {
		if (userLogin == null || userLogin.isEmpty()) {
			return null;
		}
		try {
			return create(
					URLDecoder.decode(userLogin, "UTF-8"),
					URLDecoder.decode(userPass, "UTF-8")
			);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private UserDetailsConfig create(String userLogin, String userPass) {
		if (userLogin == null || userLogin.isEmpty()) {
			return null;
		}

		UserDetailsConfig result = new UserDetailsConfig();
		result.setUsername(userLogin);
		result.setPassword(userPass);
		return result;
	}

}
