/**
 * 
 */
package au.org.arcs.shibext.sharedtoken;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

	public String getSharedToken(String uid, String primaryKeyName) throws IMASTException {
		log.debug("calling getSharedToken ...");

		Connection conn = null;
		String sharedToken = null;
		PreparedStatement st = null;
		ResultSet rs = null;

		try {

			try {
				conn = dataSource.getConnection();

				st = conn
						.prepareStatement("SELECT sharedToken from tb_st WHERE " + primaryKeyName + "=?");
				st.setString(1, uid);
				log.debug("SELECT sharedToken from tb_st WHERE " + primaryKeyName + "=" + uid);
				rs = st.executeQuery();

				while (rs.next()) {
					sharedToken = rs.getString("sharedToken");
				}
			} catch (SQLException e) {
				e.printStackTrace();
				log.error(e.getMessage());
				throw new IMASTException(e.getMessage());
			} finally {
				try {
					rs.close();
					conn.close();
				} catch (SQLException e) {
					throw new IMASTException(e.getMessage());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new IMASTException(e.getMessage()
					+ "\n Failed to get SharedToken from database");

		}
		log.info("SharedToken : " + sharedToken);

		return sharedToken;
	}

	public void storeSharedToken(String uid, String sharedToken, String primaryKeyName)
			throws IMASTException {
		log.debug("calling storeSharedToken ...");
		Connection conn = null;
		PreparedStatement st = null;

		try {

			try {
				conn = dataSource.getConnection();
				//st = conn
				//		.prepareStatement("REPLACE INTO tb_st SET sharedToken = ?, " + primaryKeyName + " = ?");
				st = conn.prepareStatement("INSERT INTO tb_st VALUES ('?','?')");
				st.setString(1, sharedToken);
				st.setString(2, uid);
				//log.debug("REPLACE INTO tb_st SET SharedToken = " + sharedToken
				//		+ ", " + primaryKeyName + " = " + uid);
				log.debug("INSERT INTO tb_st VALUES ('" + uid + "','" + sharedToken + "')");
				int rows = st.executeUpdate();
				log.debug("Successfully store the SharedToken in the database");
			} catch (SQLException e) {
				e.printStackTrace();
				throw new IMASTException(e.getMessage());
			} finally {
				try {
					conn.close();
				} catch (SQLException e) {
					throw new IMASTException(e.getMessage());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new IMASTException(e.getMessage()
					+ "Failed to store SharedToken to database");

		}

	}
}
