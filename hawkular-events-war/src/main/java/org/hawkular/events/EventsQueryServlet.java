/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.events;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

public class EventsQueryServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Resource(mappedName = "java:jboss/datasources/HawkularDS")
    private DataSource eventsDataSource;

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String maxRowsString = request.getParameter("maxRows");
        int maxRows = 100;
        if (maxRowsString != null) {
            try {
                maxRows = Integer.parseInt(maxRowsString);
            } catch (Exception e) {
                throw new ServletException("Invalid maxRows parameter", e);
            }
        }

        String sql = "SELECT ID, CORRELATION_ID, SUBSYSTEM, EVENT_TIME, MESSAGE, DETAILS FROM HAWKULAR_EVENTS";

        Connection conn = null;
        try {
            PrintWriter writer = response.getWriter();
            writer.println("<table border=\"1\">");
            writer.println("<tr>");
            writer.println("<th>ID</th>");
            writer.println("<th>CorrelationID</th>");
            writer.println("<th>Subsystem</th>");
            writer.println("<th>Timestamp</th>");
            writer.println("<th>Message</th>");
            writer.println("<th>Details</th>");
            writer.println("</tr>");

            int rows = 0;

            conn = eventsDataSource.getConnection();
            Statement stmt = conn.createStatement();
            stmt.setMaxRows(maxRows);
            ResultSet results = stmt.executeQuery(sql);
            while (results.next()) {
                rows++;
                String id = results.getString(1);
                String corId = results.getString(2);
                String subsystem = results.getString(3);
                Date eventsTime = new Date(results.getLong(4));
                String msg = results.getString(5);
                String details = results.getString(6);
                writer.printf("<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>\n", id,
                        corId, subsystem, eventsTime, msg, details);
            }

            writer.println("</table>");
            writer.printf("<p><b>Total rows: %d</b></p>\n", rows);
        } catch (Exception e) {
            throw new ServletException("Cannot query for events data", e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    throw new ServletException("Cannot close connection", e);
                }
            }
        }
    }
}
