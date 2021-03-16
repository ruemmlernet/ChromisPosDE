/*
**    Chromis POS  - The New Face of Open Source POS
**    Copyright (c)2015-2016
**    http://www.chromis.co.uk
**
**    This file is part of Chromis POS Version V0.60.2 beta
**
**    Chromis POS is free software: you can redistribute it and/or modify
**    it under the terms of the GNU General Public License as published by
**    the Free Software Foundation, either version 3 of the License, or
**    (at your option) any later version.
**
**    Chromis POS is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
**    GNU General Public License for more details.
**
**    You should have received a copy of the GNU General Public License
**    along with Chromis POS.  If not, see <http://www.gnu.org/licenses/>
**
**
 */
package uk.chromis.data.loader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author adrianromero Created on February 6, 2007, 4:06 PM
 *
 */
public final class Session {

    private final String m_surl;
    private final String m_sappuser;
    private final String m_spassword;

    private Connection m_c;
    private boolean m_bInTransaction;

    /**
     *
     */
    public final SessionDB DB;

    /**
     * Creates a new instance of Session
     *
     * @param url
     * @param user
     * @param password
     * @throws java.sql.SQLException
     */
    public Session(String url, String user, String password) throws SQLException {
        m_surl = url;
        m_sappuser = user;
        m_spassword = password;

        m_c = null;
        m_bInTransaction = false;

        connect(); // no lazy connection

        DB = getDiff();
    }

    public Session(Connection connection) throws SQLException {
        m_surl = null;
        m_sappuser = null;
        m_spassword = null;
        m_bInTransaction = false;
        m_c = connection;
        m_c.setAutoCommit(true);
        DB = getDiff();
    }

    /**
     *
     * @throws SQLException
     */
    public void connect() throws SQLException {

        // primero cerramos si no estabamos cerrados
        close();

        // creamos una nueva conexion.
        m_c = ConnectionFactory.getInstance().getConnection();
        m_c.setAutoCommit(true);
        m_bInTransaction = false;
    }

    /**
     *
     */
    public void close() {

        if (m_c != null) {
            try {
                if (m_bInTransaction) {
                    m_bInTransaction = false; // lo primero salimos del estado
                    m_c.rollback();
                    m_c.setAutoCommit(true);
                }
                m_c.close();
            } catch (SQLException e) {
                // me la como
            } finally {
                m_c = null;
            }
        }
    }

    /**
     *
     * @return @throws SQLException
     */
    public Connection getConnection() throws SQLException {

        if (!m_bInTransaction) {
            ensureConnection();
        }
        return m_c;
    }

    /**
     *
     * @throws SQLException
     */
    public void begin() throws SQLException {

        if (m_bInTransaction) {
            throw new SQLException("Already in transaction");
        } else {
            ensureConnection();
            m_c.setAutoCommit(false);
            m_bInTransaction = true;
        }
    }

    /**
     *
     * @throws SQLException
     */
    public void commit() throws SQLException {
        if (m_bInTransaction) {
            m_bInTransaction = false; // lo primero salimos del estado
            m_c.commit();
            m_c.setAutoCommit(true);
        } else {
            throw new SQLException("Transaction not started");
        }
    }

    /**
     *
     * @throws SQLException
     */
    public void rollback() throws SQLException {
        if (m_bInTransaction) {
            m_bInTransaction = false; // lo primero salimos del estado
            m_c.rollback();
            m_c.setAutoCommit(true);
        } else {
            throw new SQLException("Transaction not started");
        }
    }

    /**
     *
     * @return
     */
    public boolean isTransaction() {
        return m_bInTransaction;
    }

    private void ensureConnection() throws SQLException {
        // solo se invoca si isTransaction == false

        boolean bclosed;
        try {
            bclosed = m_c == null || m_c.isClosed();
        } catch (SQLException e) {
            bclosed = true;
        }

        // reconnect if closed
        if (bclosed) {
            connect();
        }
    }

    /**
     *
     * @return @throws SQLException
     */
    public String getURL() throws SQLException {
        return getConnection().getMetaData().getURL();
    }

    private SessionDB getDiff() throws SQLException {

        String sdbmanager = getConnection().getMetaData().getDatabaseProductName();
        switch (sdbmanager) {
            case "MySQL":
                return new SessionDBMySQL();
            case "PostgreSQL":
                return new SessionDBPostgreSQL();
            case "Apache Derby":
                return new SessionDBDerby();
            default:
                return new SessionDBGeneric(sdbmanager);
        }
    }
}
