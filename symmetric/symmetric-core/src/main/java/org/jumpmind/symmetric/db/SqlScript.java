/*
 * Licensed to JumpMind Inc under one or more contributor 
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding 
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU Lesser General Public License (the
 * "License"); you may not use this file except in compliance
 * with the License. 
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see           
 * <http://www.gnu.org/licenses/>.
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.  */
package org.jumpmind.symmetric.db;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jumpmind.symmetric.common.logging.ILog;
import org.jumpmind.symmetric.common.logging.LogFactory;
import org.jumpmind.symmetric.util.AppUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * This class is for running SQL scripts against a DataSource.
 */
public class SqlScript {

    static final String COMMENT_CHARS_1 = "--";
    static final String COMMENT_CHARS_2 = "#";

    static final ILog log = LogFactory.getLog(SqlScript.class);

    public final static String QUERY_ENDS = ";";

    private String delimiter = QUERY_ENDS;

    private List<String> script;

    private DataSource dataSource;

    private int commitRate = 10000;

    private boolean failOnError = true;

    private Map<String, String> replacementTokens;

    private String fileName = "memory";
    
    private String lineDeliminator;

    public SqlScript(URL url, DataSource ds) {
        this(url, ds, true, QUERY_ENDS, null);
    }

    public SqlScript(URL url, DataSource ds, boolean failOnError) {
        this(url, ds, failOnError, QUERY_ENDS, null);
    }

    public SqlScript(URL url, DataSource ds, String delimiter) {
        this(url, ds, true, delimiter, null);
    }

    @SuppressWarnings("unchecked")
    public SqlScript(URL url, DataSource ds, boolean failOnError,
            String delimiter, Map<String, String> replacementTokens) {
        try {
            fileName = url.getFile();
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            log.info("ScriptLoading", fileName);
            init(IOUtils.readLines(new InputStreamReader(url.openStream(),
                    "UTF-8")), ds, failOnError, delimiter, replacementTokens);
        } catch (IOException ex) {
            log.error(ex);
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public SqlScript(String sqlScript, DataSource ds, boolean failOnError,
            String delimiter, Map<String, String> replacementTokens) {
        try {
            init(IOUtils.readLines(new StringReader(sqlScript)), ds,
                    failOnError, delimiter, replacementTokens);
        } catch (IOException ex) {
            log.error(ex);
            throw new RuntimeException(ex);
        }
    }

    private void init(List<String> sqlScript, DataSource ds,
            boolean failOnError, String delimiter,
            Map<String, String> replacementTokens) {
        this.script = sqlScript;
        this.dataSource = ds;
        this.failOnError = failOnError;
        this.delimiter = delimiter;
        this.replacementTokens = replacementTokens;
    }

    private void closeQuietly(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    public void execute() {
        execute(false);    
    }
    
    public void execute(final boolean autoCommit) {
        JdbcTemplate template = new JdbcTemplate(this.dataSource);
        template.execute(new ConnectionCallback<Object>() {
            public Object doInConnection(Connection connection)
                    throws SQLException, DataAccessException {
                Statement st = null;
                int lineCount = 0;

                try {
                    connection.setAutoCommit(autoCommit);
                    st = connection.createStatement();
                    StringBuilder sql = new StringBuilder();
                    int count = 0;
                    int notFoundCount = 0;
                    for (String line : script) {
                        lineCount++;
                        line = trimComments(line);
                        if (line.trim().length() > 0) {
                            if (checkStatementEnds(line)) {
                                if (sql.length() > 0) {
                                    sql.append("\n");
                                }
                                sql.append(line.substring(0, line
                                        .lastIndexOf(delimiter)).trim());
                                String toExecute = sql.toString();
                                if (StringUtils.isNotBlank(lineDeliminator)) {
                                    toExecute = toExecute.replaceAll(lineDeliminator, "\n");
                                }
                                log.debug("Sql", sql);
                                try {
                                    st.execute(AppUtils.replaceTokens(toExecute, replacementTokens));
                                    count++;
                                    if (count % commitRate == 0) {
                                        connection.commit();
                                    }
                                } catch (SQLException e) {
                                    if (failOnError) {
                                        log
                                                .error("SqlError", e, sql
                                                        .toString());
                                        throw e;
                                    } else {
                                        if (e.getErrorCode() != 942
                                                && e.getErrorCode() != 2289) {
                                            log.warn("Sql", e.getMessage()
                                                    + ": " + sql.toString());
                                        } else if (sql.toString().toLowerCase()
                                                .startsWith("drop")) {
                                            notFoundCount++;
                                        }
                                    }
                                }
                                sql.setLength(0);
                            } else {
                                sql.append("\n");
                                sql.append(line);
                            }
                        }
                    }

                    if (!autoCommit) {
                        connection.commit();
                    }

                    log.info("ScriptCompleted", count, fileName);
                    if (notFoundCount > 0) {
                        log.info("ScriptDropError", notFoundCount);
                    }
                } catch (Exception e) {
                    log.info("ScriptError", lineCount, fileName);
                    throw new RuntimeException(e);
                } finally {
                    closeQuietly(st);
                }
                return null;
            }
        });
    }

    private String trimComments(String line) {
        int index = line.indexOf(COMMENT_CHARS_1);
        if (index >= 0) {
            line = line.substring(0, index);
        }
        index = line.indexOf(COMMENT_CHARS_2);
        if (index >= 0) {
            line = line.substring(0, index);
        }
        return line;
    }

    private boolean checkStatementEnds(String s) {
        return s.trim().endsWith("" + delimiter);
    }

    public int getCommitRate() {
        return commitRate;
    }

    public void setCommitRate(int commitRate) {
        this.commitRate = commitRate;
    }
    
    public void setLineDeliminator(String lineDeliminator) {
        this.lineDeliminator = lineDeliminator;
    }

}