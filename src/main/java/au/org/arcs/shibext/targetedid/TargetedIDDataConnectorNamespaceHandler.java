/**
 * 
 */
package au.org.arcs.shibext.targetedid;

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

/**
 * @author Damien Chen
 *
 */
public class TargetedIDDataConnectorNamespaceHandler extends
		BaseSpringNamespaceHandler {

	/** Namespace for this handler. */
	public static String NAMESPACE = "urn:mace:arcs.org.au:shibboleth:2.0:resolver:dc";

	public void init() {
		registerBeanDefinitionParser(
				TargetedIDDataConnectorBeanDefinitionParser.TYPE_NAME,
				new TargetedIDDataConnectorBeanDefinitionParser());
	}

}
