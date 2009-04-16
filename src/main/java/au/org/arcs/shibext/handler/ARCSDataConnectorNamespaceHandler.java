/**
 * 
 */
package au.org.arcs.shibext.handler;

import au.org.arcs.shibext.sharedtoken.SharedTokenDataConnectorBeanDefinitionParser;
import au.org.arcs.shibext.targetedid.TargetedIDDataConnectorBeanDefinitionParser;
import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

/**
 * @author Damien Chen
 *
 */
public class ARCSDataConnectorNamespaceHandler extends
		BaseSpringNamespaceHandler {
	
	/** Namespace for this handler. */
	public static String NAMESPACE = "urn:mace:arcs.org.au:shibboleth:2.0:resolver:dc";

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.xml.NamespaceHandler#init()
	 */
	public void init() {
		registerBeanDefinitionParser(
				TargetedIDDataConnectorBeanDefinitionParser.TYPE_NAME,
				new TargetedIDDataConnectorBeanDefinitionParser());

		registerBeanDefinitionParser(
				SharedTokenDataConnectorBeanDefinitionParser.TYPE_NAME,
				new SharedTokenDataConnectorBeanDefinitionParser());

	}

}
