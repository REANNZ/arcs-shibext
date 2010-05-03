/**
 * 
 */
package au.org.arcs.shibext.sharedtoken;

import java.beans.PropertyVetoException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.xml.namespace.QName;

import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import au.org.arcs.shibext.handler.ARCSDataConnectorNamespaceHandler;

import edu.internet2.middleware.shibboleth.common.config.attribute.resolver.dataConnector.BaseDataConnectorBeanDefinitionParser;
import edu.internet2.middleware.shibboleth.common.config.attribute.resolver.dataConnector.DataConnectorNamespaceHandler;

/**
 * @author Damien Chen
 * 
 */
public class SharedTokenDataConnectorBeanDefinitionParser extends
		BaseDataConnectorBeanDefinitionParser {

	private final Logger log = LoggerFactory
			.getLogger(SharedTokenDataConnectorBeanDefinitionParser.class);

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
		if (pluginConfig.hasAttributeNS(null, "idpHome")) {
			pluginBuilder.addPropertyValue("idpHome", pluginConfig
					.getAttributeNS(null, "idpHome"));
		}

		if (pluginConfig.hasAttributeNS(null, "storeLdap")) {
			pluginBuilder.addPropertyValue("storeLdap", XMLHelper
					.getAttributeValueAsBoolean(pluginConfig
							.getAttributeNodeNS(null, "storeLdap")));
		} else {
			pluginBuilder.addPropertyValue("storeLdap", true);
		}

		if (pluginConfig.hasAttributeNS(null, "subtreeSearch")) {
			pluginBuilder.addPropertyValue("subtreeSearch", XMLHelper
					.getAttributeValueAsBoolean(pluginConfig
							.getAttributeNodeNS(null, "subtreeSearch")));
		} else {
			pluginBuilder.addPropertyValue("subtreeSearch", true);
		}

		pluginBuilder.addPropertyValue("sourceAttribute", pluginConfig
				.getAttributeNS(null, "sourceAttributeID"));
		pluginBuilder.addPropertyValue("salt", pluginConfig.getAttributeNS(
				null, "salt").getBytes());

		if (pluginConfig.hasAttributeNS(null, "storeDatabase")) {
			pluginBuilder.addPropertyValue("storeDatabase", XMLHelper
					.getAttributeValueAsBoolean(pluginConfig
							.getAttributeNodeNS(null, "storeDatabase")));
		} else {
			pluginBuilder.addPropertyValue("storeDatabase", false);
		}

		DataSource connectionSource = processConnectionManagement(pluginId,
				pluginConfigChildren, pluginBuilder);
		pluginBuilder.addPropertyValue("dataSource", connectionSource);

	}

	/**
	 * Processes the connection management configuration.
	 * 
	 * @param pluginId
	 *            ID of this data connector
	 * @param pluginConfigChildren
	 *            configuration elements for this connector
	 * @param pluginBuilder
	 *            bean definition builder
	 * 
	 * @return data source built from configuration
	 */
	protected DataSource processConnectionManagement(String pluginId,
			Map<QName, List<Element>> pluginConfigChildren,
			BeanDefinitionBuilder pluginBuilder) {

		DataSource ds = null;

		try {
			List<Element> le = pluginConfigChildren.get(new QName(
					ARCSDataConnectorNamespaceHandler.NAMESPACE,
					"DatabaseConnection"));
			if (le != null) {
				ds = buildDatabaseConnection(
						pluginId,
						pluginBuilder,
						pluginConfigChildren
								.get(
										new QName(
												ARCSDataConnectorNamespaceHandler.NAMESPACE,
												"DatabaseConnection")).get(0));
			} else {
				log
						.info("DatabaseConnection element is not set in SharedToken data connector");

			}

		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return ds;

	}

	/**
	 * Builds a JDBC {@link DataSource} from an DatabaseConnection configuration
	 * element.
	 * 
	 * @param pluginId
	 *            ID of this data connector
	 * @param dbc
	 *            the DatabaseConnection configuration element
	 * 
	 * @return the built data source
	 */
	protected DataSource buildDatabaseConnection(String pluginId,
			BeanDefinitionBuilder pluginBuilder, Element dbc) {

		if (dbc.hasAttributeNS(null, "primaryKeyName")) {
			pluginBuilder.addPropertyValue("primaryKeyName", dbc
					.getAttributeNS(null, "primaryKeyName"));
		} else {
			pluginBuilder.addPropertyValue("primaryKeyName", "uid");
		}

		ComboPooledDataSource datasource = new ComboPooledDataSource();
		String driverClass = DatatypeHelper.safeTrim(dbc.getAttributeNS(null,
				"jdbcDriver"));
		ClassLoader classLoader = this.getClass().getClassLoader();
		try {
			classLoader.loadClass(driverClass);
		} catch (ClassNotFoundException e) {
			log
					.error("Unable to create relational database connector, JDBC driver can not be found on the classpath");
			throw new BeanCreationException(
					"Unable to create relational database connector, JDBC driver can not be found on the classpath");
		}
		try {
			datasource.setDriverClass(driverClass);
			datasource.setJdbcUrl(DatatypeHelper.safeTrim(dbc.getAttributeNS(
					null, "jdbcURL")
					+ "?autoReconnect=true"));
			datasource.setUser(DatatypeHelper.safeTrim(dbc.getAttributeNS(null,
					"jdbcUserName")));
			datasource.setPassword(DatatypeHelper.safeTrim(dbc.getAttributeNS(
					null, "jdbcPassword")));

			log.debug("Created data source for data connector {}", pluginId);
			return datasource;
		} catch (PropertyVetoException e) {
			log
					.error(
							"Unable to create data source for data connector {} with JDBC driver class {}",
							pluginId, driverClass);
			return null;
		}

	}

}
