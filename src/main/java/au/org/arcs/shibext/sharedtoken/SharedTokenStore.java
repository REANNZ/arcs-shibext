/**
 * 
 */
package au.org.arcs.shibext.sharedtoken;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Damien Chen
 *
 */
public class SharedTokenStore {
	
	/** Class logger. */
	private final Logger log = LoggerFactory
			.getLogger(SharedTokenStore.class);	
	
	private DataSource dataSource;
	
	public SharedTokenStore(DataSource dataSource){
		
		this.dataSource = dataSource;
		
	}
	public String getSharedToken(){
		log.debug("calling getSharedToken ...");
		return null;
	}

	public void storeSharedToken(){
		log.debug("calling storeSharedToken ...");
	
	}
}
