/**
 * 
 */
package au.org.arcs.shibext.sharedtoken;

import java.beans.PropertyVetoException;
import java.util.List;

import javax.sql.DataSource;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.spring.dc.AbstractDataConnectorParser;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.mchange.v2.c3p0.ComboPooledDataSource;

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
		DataSource connectionSource = processConnectionManagement(pluginConfig, pluginBuilder);
		if (connectionSource != null) {
		    pluginBuilder.addPropertyValue("dataSource", connectionSource);
		}

	}

	/**
	 * Processes the connection management configuration.
	 * 
	 * @param pluginConfig
	 *            configuration element for this connector
	 * @param pluginBuilder
	 *            bean definition builder
	 * 
	 * @return data source built from configuration
	 */
	protected DataSource processConnectionManagement(Element pluginConfig,
			BeanDefinitionBuilder pluginBuilder) {

		DataSource ds = null;

		try {
			// NOTE: the DatabaseConnection element is intentionally 
			// in the ARCS namespace as it has a slightly different 
			// syntax then what's defined within urn:mace:shibboleth:2.0:resolver:dc
			List<Element> le = ElementSupport.getChildElements(pluginConfig,
					new QName(ARCSDataConnectorNamespaceHandler.NAMESPACE,
							"DatabaseConnection"));
			// Only allow one DatabaseConnection element
			if (le != null && le.size() >1) {
				log.error("Only one DatabaseConnection element is allowed in a SharedTokenDataConnector definition");
				throw new BeanCreationException("Only one DatabaseConnection element is allowed in a SharedTokenDataConnector definition");
			}

			if (le != null && ! le.isEmpty()) {
				ds = buildDatabaseConnection(
						pluginConfig,
						pluginBuilder,
						le.get(0));
			} else {
				log.info("DatabaseConnection element is not set in SharedToken data connector");

			}

		} catch (Exception e) {
			log.error("Error parsing DatabaseConnection", e);
			throw new BeanCreationException("Error parsing DatabaseConnection", e);
		}
		return ds;

	}

	/**
	 * Builds a JDBC {@link DataSource} from an DatabaseConnection configuration
	 * element.
	 * 
	 * @param pluginConfig
	 *            configuration element for this connector
	 * @param pluginBuilder
	 *            bean definition builder
	 * @param dbc
	 *            the DatabaseConnection configuration element
	 * 
	 * @return the built data source
	 */
	protected DataSource buildDatabaseConnection(Element pluginConfig, BeanDefinitionBuilder pluginBuilder, Element dbc) {

		ComboPooledDataSource datasource = new ComboPooledDataSource();
		String driverClass = MiscHelper.safeTrim(dbc.getAttributeNS(null,
				"jdbcDriver"));
		ClassLoader classLoader = this.getClass().getClassLoader();
		try {
			classLoader.loadClass(driverClass);
		} catch (ClassNotFoundException e) {
			log.error("Unable to create relational database connector, JDBC driver can not be found on the classpath");
			throw new BeanCreationException(
					"Unable to create relational database connector, JDBC driver can not be found on the classpath", e);
		}
		try {
			datasource.setDriverClass(driverClass);

			datasource.setJdbcUrl(MiscHelper.safeTrim(dbc.getAttributeNS(
					null, "jdbcURL")));

			datasource.setUser(MiscHelper.safeTrim(dbc.getAttributeNS(null,
					"jdbcUserName")));
			datasource.setPassword(MiscHelper.safeTrim(dbc.getAttributeNS(
					null, "jdbcPassword")));

			if (dbc.hasAttributeNS(null, "preferredTestQuery")) {
				datasource.setPreferredTestQuery(MiscHelper.safeTrim(dbc.getAttributeNS(
					null, "preferredTestQuery")));
			};

			if (dbc.hasAttributeNS(null, "testConnectionOnCheckin")) {
				datasource.setTestConnectionOnCheckin(
					AttributeSupport.getAttributeValueAsBoolean(dbc.getAttributeNodeNS(
					null, "testConnectionOnCheckin")));
			};

			if (dbc.hasAttributeNS(null, "testConnectionOnCheckout")) {
				datasource.setTestConnectionOnCheckout(
					AttributeSupport.getAttributeValueAsBoolean(dbc.getAttributeNodeNS(
					null, "testConnectionOnCheckout")));
			};

			log.debug("Created data source for data connector {}", pluginConfig.getAttribute("id"));
			return datasource;
		} catch (PropertyVetoException e) {
			log.error(
					"Unable to create data source for data connector {} with JDBC driver class {}",
					pluginConfig.getAttribute("id"), driverClass);
			throw new BeanCreationException("Unable to create dataSource", e);
		}

	}

}
