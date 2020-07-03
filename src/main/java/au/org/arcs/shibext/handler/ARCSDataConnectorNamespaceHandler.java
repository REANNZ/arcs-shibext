/**
 * 
 */
package au.org.arcs.shibext.handler;

import net.shibboleth.ext.spring.util.BaseSpringNamespaceHandler;
import au.org.arcs.shibext.sharedtoken.SharedTokenDataConnectorBeanDefinitionParser;

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
				SharedTokenDataConnectorBeanDefinitionParser.TYPE_NAME,
				new SharedTokenDataConnectorBeanDefinitionParser());

	}

}
