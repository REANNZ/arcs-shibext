package au.org.arcs.shibext.sharedtoken;

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

public class SharedTokenDataConnectorNamespaceHandler extends
		BaseSpringNamespaceHandler {

	/** Namespace for this handler. */
	public static String NAMESPACE = "urn:mace:arcs.org.au:shibboleth:2.0:resolver:dc";

	public void init() {
		registerBeanDefinitionParser(
				SharedTokenDataConnectorBeanDefinitionParser.TYPE_NAME,
				new SharedTokenDataConnectorBeanDefinitionParser());
	}

}
