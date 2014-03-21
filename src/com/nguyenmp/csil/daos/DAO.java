package com.nguyenmp.csil.daos;


import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A Data Access Object interface defines
 * {@link #parseResultSet(java.sql.ResultSet)} and the bean it handles.
 * @param <BeanType> the class this DAO is supposed
 *                  to interface the table with
 */
public interface DAO<BeanType> {

    /**
     * Takes a single {@link java.sql.ResultSet} who's cursor has already
     * been moved to the appropriate location.  This method will simply
     * extract the information from the ResultSet and return an initialized BeanType.
     * @param resultSet The result of a database query whose cursor has
     *                  been moved to the desired row and is ready to have
     *                  the extraction methods called ({@link ResultSet#getString(String)},
     *                  {@link ResultSet#getInt(String)}, etc).
     * @return The bean with its parameters fully initialized from the {@link ResultSet}
     * passed in as a parameter.
     * @throws java.sql.SQLException for a lot of reasons... wow...
     */
    public BeanType parseResultSet(ResultSet resultSet) throws SQLException;
}
