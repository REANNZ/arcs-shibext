/**
 * 
 */
package au.org.arcs.shibext.sharedtoken;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Damien Chen
 * 
 */
public class SharedTokenStore {

	/** Class logger. */
	private final Logger log = LoggerFactory.getLogger(SharedTokenStore.class);

	private DataSource dataSource;

	public SharedTokenStore(DataSource dataSource) {

		this.dataSource = dataSource;

	}

	public String getSharedToken(String uid, String primaryKeyName)
			throws IMASTException {
		log.debug("calling getSharedToken ...");

		Connection conn = null;
		String sharedToken = null;
		PreparedStatement st = null;
		ResultSet rs = null;

		try {

			try {
				conn = dataSource.getConnection();
				if (!isValid(conn)) {
					conn.close();
					conn = dataSource.getConnection();
					//FIXME: this is not enough - the new connection
					//is just taken from the pool and might be invalid as well
					//Sort this at the DataSource layer instead!
				}

				st = conn
						.prepareStatement("SELECT sharedToken from tb_st WHERE "
								+ primaryKeyName + "=?");
				st.setString(1, uid);
				log.debug("SELECT sharedToken from tb_st WHERE "
						+ primaryKeyName + "=" + uid);
				rs = st.executeQuery();

				while (rs.next()) {
					sharedToken = rs.getString("sharedToken");
				}
			} catch (SQLException e) {
				log.error("Error executing SQL statement", e);
				throw new IMASTException("Error executing SQL statement", e);
			} finally {
				try {
					rs.close();
					conn.close();
				} catch (SQLException e) {
					throw new IMASTException("Error closing database connection", e);
				}
			}
		} catch (Exception e) {
			log.error("Failed to get SharedToken from database", e);
			throw new IMASTException("Failed to get SharedToken from database", e);
		}
		log.debug("SharedTokenStore: found value {} for uid {}", sharedToken, uid);

		return sharedToken;
	}

	public void storeSharedToken(String uid, String sharedToken,
			String primaryKeyName) throws IMASTException {
		log.info("SharedTokenStore: storing value {} for uid {}", sharedToken, uid);
		Connection conn = null;
		// PreparedStatement st = null;
		Statement st = null;

		try {

			try {
				conn = dataSource.getConnection();
				st = conn.createStatement();
				st.execute("INSERT INTO tb_st VALUES ('" + uid + "','"
						+ sharedToken + "')");
				log.debug("INSERT INTO tb_st VALUES ('" + uid + "','"
						+ sharedToken + "')");
				log.debug("Successfully store the SharedToken in the database");
			} catch (SQLException e) {
				log.error("Failed to store SharedToken into database", e);
				throw new IMASTException("Failed to store SharedToken into database", e);
			} finally {
				try {
					conn.close();
				} catch (SQLException e) {
					throw new IMASTException("Error closing database connection", e);
				}
			}
		} catch (Exception e) {
			log.error("Failed to store SharedToken into database", e);
			throw new IMASTException("Failed to store SharedToken into database", e);
		}

	}

	private boolean isValid(Connection conn) throws SQLException {

		log.debug("testing if the connection is still valid");

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT 1");
			if (rs.next()) {
				log.debug("the connection is still valid");
				return true;
			} else {
				log.debug("the connection is not valid, will reconnect");
				return false;
			}
		} catch (SQLException e) {
			log.warn("Database connection is invalid, will reconnect", e);
			return false;
		} finally {
			if (stmt != null)
				stmt.close();
			if (rs != null)
				rs.close();
		}
	}
}
