/**
 * 
 */
package au.org.arcs.shibext.sharedtoken;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import au.org.arcs.shibext.handler.ARCSDataConnectorNamespaceHandler;

import edu.internet2.middleware.shibboleth.common.config.attribute.resolver.dataConnector.BaseDataConnectorBeanDefinitionParser;

/**
 * @author Damien Chen
 * 
 */
public class SharedTokenDataConnectorBeanDefinitionParser extends
		BaseDataConnectorBeanDefinitionParser {

	/** Schema type name. */
	public static final QName TYPE_NAME = new QName(
			ARCSDataConnectorNamespaceHandler.NAMESPACE, "SharedToken");

	/** {@inheritDoc} */
	@Override
	protected Class getBeanClass(Element element) {
		return SharedTokenDataConnectorBeanFactory.class;
	}

	/** {@inheritDoc} */
	@Override
	protected void doParse(String pluginId, Element pluginConfig,
			Map<QName, List<Element>> pluginConfigChildren,
			BeanDefinitionBuilder pluginBuilder, ParserContext parserContext) {

		super.doParse(pluginId, pluginConfig, pluginConfigChildren,
				pluginBuilder, parserContext);

		if (pluginConfig.hasAttributeNS(null, "generatedAttributeID")) {
			pluginBuilder.addPropertyValue("generatedAttribute", pluginConfig
					.getAttributeNS(null, "generatedAttributeID"));
		} else {
			pluginBuilder.addPropertyValue("generatedAttribute",
					"auEduPersonSharedToken");
		}

		if (pluginConfig.hasAttributeNS(null, "idpIdentifier")) {
			pluginBuilder.addPropertyValue("idpIdentifier", pluginConfig
					.getAttributeNS(null, "idpIdentifier"));
		} 

		if (pluginConfig.hasAttributeNS(null, "storeLdap")) {
			pluginBuilder.addPropertyValue("storeLdap", XMLHelper
					.getAttributeValueAsBoolean(pluginConfig
							.getAttributeNodeNS(null, "storeLdap")));
		} else {
			pluginBuilder.addPropertyValue("storeLdap", true);
		}

		pluginBuilder.addPropertyValue("sourceAttribute", pluginConfig
				.getAttributeNS(null, "sourceAttributeID"));
		pluginBuilder.addPropertyValue("salt", pluginConfig.getAttributeNS(
				null, "salt").getBytes());

	}
}
