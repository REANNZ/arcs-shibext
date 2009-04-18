/**
 * 
 */
package au.org.arcs.shibext.sharedtoken;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Damien Chen
 * 
 */
public class LdapUtil {

	/** Class logger. */
	private final Logger log = LoggerFactory.getLogger(LdapUtil.class);

	private Properties shareTokenProperties;

	private static String PROPERTIES_FILE = "conf/sharedtoken.properties";

	public LdapUtil() throws IMASTException {

		shareTokenProperties = new Properties();
		try {
			shareTokenProperties.load(this.getClass().getClassLoader()
					.getResourceAsStream(PROPERTIES_FILE));
		} catch (IOException e) {
			throw new IMASTException(e.getMessage().concat("\n couldn't load ")
					.concat(PROPERTIES_FILE).concat(" file"), e.getCause());
		}
	}

	public void saveAttribute(String attributeName, String attributeValue,
			String dataConnectorID, String principalName) throws IMASTException {

		log.info("storing sharedToken to Ldap ...");

		log.info("attributeName: " + attributeName);
		log.info("attributeValue: " + attributeValue);
		log.info("dataConnectorID: " + dataConnectorID);
		log.info("principalName " + principalName);

		try {

			String attributeResolver = shareTokenProperties
					.getProperty("ATTRIBUTE_RESOLVER");

			Element ldapConf = getLdapConfig(dataConnectorID, attributeResolver);
			HashMap<String, String> ldapRawProp = getLdapRawProperties(ldapConf);
			Properties properties = buildLdapProperties(ldapRawProp);
			InitialDirContext context = initConnection(properties);

			String searchFilterSpec = shareTokenProperties
					.getProperty("SEARCH_FILTER_SPEC");

			String searchFilter = searchFilterSpec
					.replace("{0}", principalName);
			if (searchFilterSpec == null || searchFilterSpec.equals("")) {
				throw new IMASTException("couldn't find search filter spec");
			} else {
				log.info("ldap search filter : " + searchFilter);
			}

			Attribute mod0 = new BasicAttribute(attributeName, attributeValue);
			ModificationItem[] mods = new ModificationItem[1];

			log.info("adding sharedToken to ldap entry");

			mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, mod0);
			log.info("add successfully");
			try {
				context.modifyAttributes(searchFilter, mods);
			} catch (NamingException e) {
				e.printStackTrace();
				throw new IMASTException(e.getMessage().concat(
						"\n failed to add sharedToken to ldap entry"), e
						.getCause());
				// mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
				// mod0);
				// dirContext.modifyAttributes(populatedSearch, mods);
			}
		} catch (Exception e) {
			log.error(e.getMessage().concat(
					"\n failed to add sharedToken to ldap entry"));
			throw new IMASTException(e.getMessage().concat(
					"\n failed to save attribute to ldap entry"), e.getCause());
		}

	}

	private InitialDirContext initConnection(Properties properties)
			throws IMASTException {

		log.info("calling initConnection() ...");

		InitialDirContext context = null;
		SSLSocketFactory sslsf;
		boolean useExternalAuth = false;
		boolean useStartTLS = (Boolean) properties.get("useStartTLS");

		try {

			if (useStartTLS) {
				log.info("useStartTLS is true");
				if ("EXTERNAL".equals(properties
						.getProperty(Context.SECURITY_AUTHENTICATION))) {
					log.info("use EXTERNAL authentication");
					useExternalAuth = true;
					properties.remove(Context.SECURITY_AUTHENTICATION);
				}
				log.info("setting SECURITY_AUTHENTICATION to NONE before starting TLS");

				String backupAuthType = properties
						.getProperty(Context.SECURITY_AUTHENTICATION);
				properties.setProperty(Context.SECURITY_AUTHENTICATION, "NONE");

				log.info("initiating ldap context without bind: " + properties.toString());
				context = new InitialLdapContext(properties, null);

				log.info("creating tls context ...");
				SSLContext sslc;
				sslc = SSLContext.getInstance("TLS");
				sslc.init(new KeyManager[] { null }, null, new SecureRandom());
				sslsf = sslc.getSocketFactory();

				StartTlsResponse tls = (StartTlsResponse) ((LdapContext) context)
						.extendedOperation(new StartTlsRequest());
				log.info("tls negotiating ...");
				tls.negotiate(sslsf);
				log.info("tls negotiating successful ...");

				if (useExternalAuth) {
					context.addToEnvironment(Context.SECURITY_AUTHENTICATION,
							"EXTERNAL");
				} else {
					log.debug("binding ...");
					properties.setProperty(Context.SECURITY_AUTHENTICATION,
							backupAuthType);
					log.debug("after starting " + properties.toString());
					context
							.addToEnvironment(
									Context.SECURITY_AUTHENTICATION,
									properties
											.getProperty(Context.SECURITY_AUTHENTICATION));

				}

			} else {
				log.debug(properties.toString());
				context = new InitialDirContext(properties);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IMASTException(e.getMessage().concat(
					"\n failed to initiate ldap context"), e.getCause());
		}

		return context;
	}

	private Properties buildLdapProperties(HashMap<String, String> ldapRawProp)
			throws IMASTException {
		// Properties properties = new Properties(System.getProperties());

		log.info("setting up ldap context environment");
		Properties properties = new Properties();

		if (ldapRawProp == null || ldapRawProp.isEmpty()) {
			throw new IMASTException("null or empty ldap properties");
		}

		try {
			String providerUrl = ldapRawProp.get("ldapURL") + "/"
					+ ldapRawProp.get("baseDN");
			String secAuth = ldapRawProp.get("authenticationType") == "" ? "SIMPLE"
					: ldapRawProp.get("authenticationType");
			String secPrincipal = ldapRawProp.get("principal");
			String pricipalCre = ldapRawProp.get("principalCredential");
			boolean useStartTLS = ldapRawProp.get("useStartTLS").trim().equals(
					"true") ? true : false;
			properties.put(Context.INITIAL_CONTEXT_FACTORY,
					"com.sun.jndi.ldap.LdapCtxFactory");
			properties.put(Context.PROVIDER_URL, providerUrl);
			properties.put(Context.SECURITY_AUTHENTICATION, secAuth);
			properties.put(Context.SECURITY_PRINCIPAL, secPrincipal);
			properties.put(Context.SECURITY_CREDENTIALS, pricipalCre);
			properties.put("useStartTLS", useStartTLS);
		} catch (Exception e) {
			throw new IMASTException(e.getMessage().concat(
					"\n fail to build ldap properties"));
		}
		log.info(properties.toString());
		return properties;

	}

	private HashMap<String, String> getLdapRawProperties(Element ldapConfig)
			throws IMASTException {

		if (ldapConfig == null) {
			throw new IMASTException(
					"ldapConfig element is null, please check it with attribute resolver file");
		} else {
			log.debug("ldapConfig element: \n".concat(ldapConfig.toString()));
		}

		HashMap<String, String> ldapRawProperties = new HashMap<String, String>();
		try {
			ldapRawProperties
					.put("ldapURL", ldapConfig.getAttribute("ldapURL"));
			ldapRawProperties.put("baseDN", ldapConfig.getAttribute("baseDN"));
			ldapRawProperties.put("authenticationType", ldapConfig
					.getAttribute("authenticationType"));
			ldapRawProperties.put("principal", ldapConfig
					.getAttribute("principal"));
			ldapRawProperties.put("principalCredential", ldapConfig
					.getAttribute("principalCredential"));
			ldapRawProperties.put("useStartTLS", ldapConfig
					.getAttribute("useStartTLS"));
			ldapRawProperties.put("filterTemplate", ldapConfig
					.getElementsByTagName("FilterTemplate").item(0)
					.getTextContent().trim());

			log.debug("ldapRowProperties " + ldapRawProperties);
		} catch (Exception e) {

			throw new IMASTException(e.getMessage(), e.getCause());
		}
		return ldapRawProperties;

	}

	private Element getLdapConfig(String connectorID, String attributeResolver)
			throws IMASTException {

		log.info("getting ldap config from attribute resolver file");

		Element elem = null;

		if (connectorID == null || connectorID.equals("")) {
			throw new IMASTException("invalid connectorID: "
					.concat(connectorID));
		} else {
			log.info("connectorID : " + connectorID);
		}

		if (attributeResolver == null || attributeResolver.equals("")) {
			throw new IMASTException("invalid attttibute resolver file: "
					.concat(attributeResolver));
		} else {
			log.info("attribute resolver file : " + attributeResolver);
		}

		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = null;
			docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc;
			doc = docBuilder.parse(new File(attributeResolver));
			NodeList dataConnectors = doc
					.getElementsByTagName("resolver:DataConnector");
			for (int s = 0; s < dataConnectors.getLength(); s++) {
				elem = (Element) dataConnectors.item(s);
				String id = elem.getAttribute("id");
				if (id != null && id.equalsIgnoreCase(connectorID))
					break;
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			throw new IMASTException(e.getMessage().concat(
					"\n failed to parse attribute resolver file."), e
					.getCause());
		} catch (SAXException e) {
			e.printStackTrace();
			throw new IMASTException(e.getMessage().concat(
					"\n failed to parse attribute resolver file."), e
					.getCause());
		} catch (IOException e) {
			e.printStackTrace();
			throw new IMASTException(e.getMessage().concat(
					"\n failed to parse attribute resolver file."), e
					.getCause());
		} catch (Exception e) {
			e.printStackTrace();
			throw new IMASTException(e.getMessage().concat(
					"\n failed to parse attribute resolver file."), e
					.getCause());

		}

		return elem;
	}
}
