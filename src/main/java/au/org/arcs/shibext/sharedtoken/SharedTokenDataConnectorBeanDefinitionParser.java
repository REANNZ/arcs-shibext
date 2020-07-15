/**
 * 
 */
package au.org.arcs.shibext.sharedtoken;

import java.util.List;

import javax.sql.DataSource;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.spring.dc.AbstractDataConnectorParser;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import au.org.arcs.shibext.handler.ARCSDataConnectorNamespaceHandler;

/**
 * @author Damien Chen
 * @author Vlad Mencl
 * 
 */
public class SharedTokenDataConnectorBeanDefinitionParser extends
		AbstractDataConnectorParser {

	private final Logger log = LoggerFactory
			.getLogger(SharedTokenDataConnectorBeanDefinitionParser.class);

	/** Schema type name. */
	public static final QName TYPE_NAME = new QName(
			ARCSDataConnectorNamespaceHandler.NAMESPACE, "SharedToken");

	/** {@inheritDoc} */
	@Override
	protected Class<SharedTokenDataConnector> getNativeBeanClass() {
		return SharedTokenDataConnector.class;
	}

	/** {@inheritDoc} */
	@Override
	protected void doV2Parse(Element pluginConfig,
			ParserContext parserContext,
			BeanDefinitionBuilder pluginBuilder) {

		if (pluginConfig.hasAttributeNS(null, "generatedAttributeID")) {
			pluginBuilder.addPropertyValue("generatedAttributeId", pluginConfig
					.getAttributeNS(null, "generatedAttributeID"));
		} else {
			pluginBuilder.addPropertyValue("generatedAttributeId",
					"auEduPersonSharedToken");
		}

		if (pluginConfig.hasAttributeNS(null, "idpIdentifier")) {
			pluginBuilder.addPropertyValue("idpIdentifier", pluginConfig
					.getAttributeNS(null, "idpIdentifier"));
		}

		if (pluginConfig.hasAttributeNS(null, "storeLdap")) {
			pluginBuilder.addPropertyValue("storeLdap", AttributeSupport
					.getAttributeValueAsBoolean(pluginConfig
							.getAttributeNodeNS(null, "storeLdap")));
		} else {
			pluginBuilder.addPropertyValue("storeLdap", true);
		}

		if (pluginConfig.hasAttributeNS(null, "ldapConnectorId")) {
			pluginBuilder.addPropertyValue("ldapConnectorId", pluginConfig
					.getAttributeNS(null, "ldapConnectorId"));
		}

		if (pluginConfig.hasAttributeNS(null, "storedAttributeName")) {
			pluginBuilder.addPropertyValue("storedAttributeName", pluginConfig
					.getAttributeNS(null, "storedAttributeName"));
		}
		
		pluginBuilder.addPropertyValue("sourceAttributeId", pluginConfig
				.getAttributeNS(null, "sourceAttributeID"));
		pluginBuilder.addPropertyValue("salt", pluginConfig.getAttributeNS(
				null, "salt").getBytes());

		if (pluginConfig.hasAttributeNS(null, "storeDatabase")) {
			pluginBuilder.addPropertyValue("storeDatabase", AttributeSupport
					.getAttributeValueAsBoolean(pluginConfig
							.getAttributeNodeNS(null, "storeDatabase")));
		} else {
			pluginBuilder.addPropertyValue("storeDatabase", false);
		}

		if (pluginConfig.hasAttributeNS(null, "databaseConnectionID")) {
			pluginBuilder.addPropertyReference("dataSource", pluginConfig
					.getAttributeNS(null, "databaseConnectionID"));
		}

	}

}
