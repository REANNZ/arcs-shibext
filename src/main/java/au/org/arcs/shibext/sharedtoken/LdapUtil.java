/**
 * 
 */
package au.org.arcs.shibext.sharedtoken;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Iterator;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
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
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
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
	private static String DATA_CONNECTOR_XML_NS = "urn:mace:shibboleth:2.0:resolver:dc";
	private static String RESOLVER_XML_NS = "urn:mace:shibboleth:2.0:resolver";
	private static String LDAP_PROPERTY_PASSTHROUGH_PREFIX = "shibboleth.resolver.dc.ldapproperty.";

	public LdapUtil() throws IMASTException {

		shareTokenProperties = new Properties();
		try {
			shareTokenProperties.load(this.getClass().getClassLoader()
					.getResourceAsStream(PROPERTIES_FILE));
		} catch (IOException e) {
			throw new IMASTException("Could not load properties file " + PROPERTIES_FILE, e);
		}
	}

	public void saveAttribute(String attributeName, String attributeValue,
			String dataConnectorID, String principalName, String idpHome, boolean subtreeSearch)
			throws IMASTException {

		log.info("storing sharedToken to Ldap ...");

		log.info("attributeName: " + attributeName);
		log.info("attributeValue: " + attributeValue);
		log.info("dataConnectorID: " + dataConnectorID);
		log.info("principalName " + principalName);

		try {

			if (shareTokenProperties == null || shareTokenProperties.isEmpty()) {
				throw new IMASTException("failed to get properties file ");
			}

			String attributeResolver = shareTokenProperties
					.getProperty("ATTRIBUTE_RESOLVER");

			if (idpHome != null && !idpHome.trim().equals("")) {
				log.debug("get IDP_HOME from data connector");
				log.debug("IDP_HOME : " + idpHome);
			} else {
				log
						.debug("couldn't get IDP_HOME from data connector. try to get it from system env");
				idpHome = System.getenv("IDP_HOME");
				if (idpHome != null && !idpHome.trim().equals("")) {
					log.debug("IDP_HOME : " + idpHome);
				} else {
					log
							.debug("couldn't get IDP_HOME from system env. try to get it sharedtoken.properties");
					idpHome = shareTokenProperties
							.getProperty("DEFAULT_IDP_HOME");
					if (idpHome != null && !idpHome.trim().equals("")) {
						log.debug("IDP_HOME : " + idpHome);
					} else {
						log.error("couldn't get IDP_HOME anywhere");
					}

				}
			}

			/*
			 * if (idpHome == null || idpHome.trim().equals("")) { log.debug(
			 * "couldn't get IDP_HOME from data connector. try to get it from system env"
			 * ); idpHome = System.getenv("IDP_HOME");
			 * 
			 * if (idpHome == null || idpHome.trim().equals("")) { idpHome =
			 * shareTokenProperties .getProperty("DEFAULT_IDP_HOME"); log
			 * .debug(
			 * "couldn't get IDP_HOME from system env. use defaut instead : " +
			 * idpHome); } }
			 */

			if (idpHome != null && attributeResolver != null) {
				attributeResolver = attributeResolver.replace("$IDP_HOME",
						idpHome);
			} else {
				throw new IMASTException(
						"failed to get attribute resolver file");
			}

			Element ldapConf = getLdapConfig(dataConnectorID, attributeResolver);
			HashMap<String, String> ldapRawProp = getLdapRawProperties(ldapConf);
			Properties properties = buildLdapProperties(ldapRawProp);
			InitialDirContext context = initConnection(properties);

			String searchFilterSpec = shareTokenProperties
					.getProperty("SEARCH_FILTER_SPEC");

			String searchFilter = null;
			if (searchFilterSpec == null || searchFilterSpec.equals(""))
				throw new IMASTException("couldn't find search filter spec");
			else {
				searchFilter = searchFilterSpec.replace("{0}", principalName);
				log.info("ldap search filter : " + searchFilter);
			}
			
			String objectName = null;

			if(subtreeSearch){
			// do search with subtree and find the object name in the subtree
			SearchControls ctls = new SearchControls();
			ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			NamingEnumeration e = context.search("", searchFilter, ctls);
			
			while (e.hasMore()) {
				SearchResult entry = (SearchResult) e.next();
				objectName = entry.getName();
				log.debug("the objects to modify: " + objectName);
			}
			log.debug("the last one - " + objectName
					+ " is considered to be modified");
			}else{
				objectName = searchFilter;
			}

			Attribute mod0 = new BasicAttribute(attributeName, attributeValue);
			ModificationItem[] mods = new ModificationItem[1];

			log.info("adding " + attributeName + " : " + attributeValue
					+ " to " + properties.getProperty(context.PROVIDER_URL)
					+ "," + objectName);

			mods[0] = new ModificationItem(DirContext.ADD_ATTRIBUTE, mod0);
			try {
				context.modifyAttributes(objectName, mods);
				log.info("add successfully");
			} catch (NamingException ex) {
				ex.printStackTrace();
				throw new IMASTException("Failed to add sharedToken to ldap entry", ex);
				// mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
				// mod0);
				// dirContext.modifyAttributes(populatedSearch, mods);
			}
		} catch (Exception e) {
			log.error("Failed to add sharedToken to ldap entry", e);
			throw new IMASTException("Failed to save attribute to ldap entry", e);
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
				log
						.info("setting SECURITY_AUTHENTICATION to NONE before starting TLS");

				String backupAuthType = properties
						.getProperty(Context.SECURITY_AUTHENTICATION);
				properties.setProperty(Context.SECURITY_AUTHENTICATION, "NONE");

				log.info("initiating ldap context without bind: "
						+ properties.toString());
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
			log.error("Failed to initialize LDAP context", e);
			throw new IMASTException("Failed to initialize LDAP context", e);
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

			// pass through any property prefixed with
			// LDAP_PROPERTY_PASSTHROUGH_PREFIX (and strip this prefix again)
			for (Iterator<String> it = ldapRawProp.keySet().iterator(); it.hasNext();) {
                            String propertyKey=it.next();
                            if (propertyKey.startsWith(LDAP_PROPERTY_PASSTHROUGH_PREFIX)) {
                                properties.put(propertyKey.substring(LDAP_PROPERTY_PASSTHROUGH_PREFIX.length()),ldapRawProp.get(propertyKey));
                            };
                        };
                            
		} catch (Exception e) {
			throw new IMASTException("Failed to build ldap properties", e);
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
			String multiLdap = ldapConfig.getAttribute("ldapURL");
			StringTokenizer st = new StringTokenizer(multiLdap);
			if (st.countTokens() > 1)
				log
						.warn("You set multiple Ldap URLs, only first one will be used in this version");
			String ldapURL = st.nextToken();

			ldapRawProperties.put("ldapURL", ldapURL);
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
					.getElementsByTagNameNS(DATA_CONNECTOR_XML_NS, "FilterTemplate").item(0)
					.getTextContent().trim());
			// Iterate over all LDAP properties defined in connector and store
			// them in ldapRawProperties to pass them through to the LDAP
			// connection we establish.
			// Look for anything like
			//    <dc:LDAPProperty name="java.naming.referral" value="follow"/>

                        NodeList propertyNodeList = ldapConfig.getElementsByTagNameNS(DATA_CONNECTOR_XML_NS, "LDAPProperty");
                        for (int i = 0; i<propertyNodeList.getLength(); i++) {
                            Node propertyNode = propertyNodeList.item(i);
                            NamedNodeMap propertyAttributes = propertyNode.getAttributes();
                            ldapRawProperties.put(LDAP_PROPERTY_PASSTHROUGH_PREFIX+propertyAttributes.getNamedItem("name").getNodeValue(),
                                    propertyAttributes.getNamedItem("value").getNodeValue());
			};

			

			log.debug("ldapRawProperties " + ldapRawProperties);
		} catch (Exception e) {
			throw new IMASTException("Could not parse LDAP config", e);
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
			docBuilderFactory.setNamespaceAware(true);
			DocumentBuilder docBuilder = null;
			docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc;
			doc = docBuilder.parse(new File(attributeResolver));
			NodeList dataConnectors = doc
					.getElementsByTagNameNS(RESOLVER_XML_NS, "DataConnector");
			for (int s = 0; s < dataConnectors.getLength(); s++) {
				elem = (Element) dataConnectors.item(s);
				String id = elem.getAttribute("id");
				if (id != null && id.equalsIgnoreCase(connectorID))
					break;
			}
		} catch (ParserConfigurationException e) {
			log.error("Failed to parse attribute resolver file", e);
			throw new IMASTException("Failed to parse attribute resolver file", e);
		} catch (SAXException e) {
			log.error("Failed to parse attribute resolver file", e);
			throw new IMASTException("Failed to parse attribute resolver file", e);
		} catch (IOException e) {
			log.error("Failed to parse attribute resolver file", e);
			throw new IMASTException("Failed to parse attribute resolver file", e);
		} catch (Exception e) {
			log.error("Failed to parse attribute resolver file", e);
			throw new IMASTException("Failed to parse attribute resolver file", e);

		}

		return elem;
	}
}
