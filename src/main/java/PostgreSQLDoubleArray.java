/**
 * Created by Sermilion on 01/08/2015.
 */
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

/**
 * This is class provides {@link java.sql.Array} interface for PostgreSQL <code>int4</code> array.
 *
 * @author Valentine Gogichashvili
 *
 */

public class PostgreSQLDoubleArray implements java.sql.Array {

    private final double[] doubleArray;
    private final String stringValue;

    public PostgreSQLDoubleArray(double[] doubleArray) {
        this.doubleArray = doubleArray;
        this.stringValue = doubleArrayToPostgreSQLDoubleArrayString(doubleArray);
    }

    public String toString() {
        return stringValue;
    }

    /**
     * This static method can be used to convert an integer array to string representation of PostgreSQL integer array.
     * @param a source integer array
     * @return string representation of a given integer array
     */
    public static String doubleArrayToPostgreSQLDoubleArrayString(double[] a) {
        if ( a == null ) {
            return "NULL";
        }
        final int al = a.length;
        if ( al == 0 ) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder( 2 + al * 7 ); // as we usually operate with 6 digit numbers + 1 symbol for a delimiting comma
        sb.append('{');
        for (int i = 0; i < al; i++) {
            if ( i > 0 ) sb.append(',');
            sb.append(a[i]);
        }
        sb.append('}');
        return sb.toString();
    }


    public static String doubleArrayToCommaSeparatedString(double[] a) {
        if ( a == null ) {
            return "NULL";
        }
        final int al = a.length;
        if ( al == 0 ) {
            return "";
        }
        StringBuilder sb = new StringBuilder( al * 7 ); // as we usually operate with 6 digit numbers + 1 symbol for a delimiting comma
        for (int i = 0; i < al; i++) {
            if ( i > 0 ) sb.append(',');
            sb.append(a[i]);
        }
        return sb.toString();
    }

    public Object getArray() throws SQLException {
        return doubleArray == null ? null : Arrays.copyOf(doubleArray, doubleArray.length);
    }

    public Object getArray(Map<String, Class<?>> map) throws SQLException {
        return getArray();
    }

    public Object getArray(long index, int count) throws SQLException {
        return doubleArray == null ? null : Arrays.copyOfRange(doubleArray, (int)index, (int)index + count );
    }

    public Object getArray(long index, int count, Map<String, Class<?>> map) throws SQLException {
        return getArray(index, count);
    }

    public int getBaseType() throws SQLException {
        return java.sql.Types.INTEGER;
    }

    public String getBaseTypeName() throws SQLException {
        return "double";
    }

    public ResultSet getResultSet() throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getResultSet(Map<String, Class<?>> map) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getResultSet(long index, int count) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public ResultSet getResultSet(long index, int count, Map<String, Class<?>> map) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void free() throws SQLException {
    }

}