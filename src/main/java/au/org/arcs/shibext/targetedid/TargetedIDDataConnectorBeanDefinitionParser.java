/**
 * 
 */
package au.org.arcs.shibext.targetedid;

import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.spring.dc.AbstractDataConnectorParser;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import au.org.arcs.shibext.handler.ARCSDataConnectorNamespaceHandler;

/**
 * @author Damien Chen
 *
 */
public class TargetedIDDataConnectorBeanDefinitionParser extends
		AbstractDataConnectorParser {

	/** Schema type name. */
	public static final QName TYPE_NAME = new QName(
			ARCSDataConnectorNamespaceHandler.NAMESPACE, "TargetedID");

	/** {@inheritDoc} */
	@Override
	protected Class<TargetedIDDataConnector> getBeanClass(Element element) {
		return TargetedIDDataConnector.class;
	}

	/** {@inheritDoc} */
	@Override
	protected void doParse(Element pluginConfig,
			ParserContext parserContext,
			BeanDefinitionBuilder pluginBuilder) {

		super.doParse(pluginConfig, parserContext,
				pluginBuilder);

		if (pluginConfig.hasAttributeNS(null, "generatedAttributeID")) {
			pluginBuilder.addPropertyValue("generatedAttributeId", pluginConfig
					.getAttributeNS(null, "generatedAttributeID"));
		} else {
			pluginBuilder.addPropertyValue("generatedAttributeId",
					"eduPersonTargetedID");
		}

		pluginBuilder.addPropertyValue("sourceAttributeId", pluginConfig
				.getAttributeNS(null, "sourceAttributeID"));
		pluginBuilder.addPropertyValue("salt", pluginConfig.getAttributeNS(
				null, "salt").getBytes());

	}

}
