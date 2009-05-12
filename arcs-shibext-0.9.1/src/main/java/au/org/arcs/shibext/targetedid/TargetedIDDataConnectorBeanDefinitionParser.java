/**
 * 
 */
package au.org.arcs.shibext.targetedid;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import au.org.arcs.shibext.handler.ARCSDataConnectorNamespaceHandler;

import edu.internet2.middleware.shibboleth.common.config.attribute.resolver.dataConnector.BaseDataConnectorBeanDefinitionParser;

/**
 * @author Damien Chen
 *
 */
public class TargetedIDDataConnectorBeanDefinitionParser extends
		BaseDataConnectorBeanDefinitionParser {

	/** Schema type name. */
	public static final QName TYPE_NAME = new QName(
			ARCSDataConnectorNamespaceHandler.NAMESPACE, "TargetedID");

	/** {@inheritDoc} */
	@Override
	protected Class getBeanClass(Element element) {
		return TargetedIDDataConnectorBeanFactory.class;
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
					"eduPersonTargetedID");
		}

		pluginBuilder.addPropertyValue("sourceAttribute", pluginConfig
				.getAttributeNS(null, "sourceAttributeID"));
		pluginBuilder.addPropertyValue("salt", pluginConfig.getAttributeNS(
				null, "salt").getBytes());

	}

}
