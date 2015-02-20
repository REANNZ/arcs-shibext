package au.org.arcs.shibext.sharedtoken;

/** Helper class for working with various datatypes, based on org.opensaml.xml.util.DatatypeHelper. */
public class MiscHelper {

    /**
     * A "safe" null/empty check for strings.
     * 
     * @param s The string to check
     * 
     * @return true if the string is null or the trimmed string is length zero
     */
    public static boolean isEmpty(String s) {
        if (s != null) {
            String sTrimmed = s.trim();
            if (sTrimmed.length() > 0) {
                return false;
            }
        }

        return true;
    }
    
    /**
     * A safe string trim that handles nulls.
     * 
     * @param s the string to trim
     * 
     * @return the trimmed string or null if the given string was null
     */
    public static String safeTrim(String s) {
        if (s != null) {
            return s.trim();
        }

        return null;
    }
	
}
