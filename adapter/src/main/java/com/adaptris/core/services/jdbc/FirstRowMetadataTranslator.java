/*
 * Copyright 2015 Adaptris Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.adaptris.core.services.jdbc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.MetadataElement;
import com.adaptris.core.ServiceException;
import com.adaptris.jdbc.JdbcResult;
import com.adaptris.jdbc.JdbcResultRow;
import com.adaptris.jdbc.JdbcResultSet;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Translate the first row of the result set into metadata.
 * 
 * <p>
 * Each column of the result set is used to create a new item of metadata. The metadata key for each new metadata item is the
 * combination of {@link #getMetadataKeyPrefix()}, {@link #getSeparator()} and the column name (or label if it's different). The
 * value is the value of the column.
 * </p>
 * 
 * @config jdbc-first-row-metadata-translator
 * 
 * @license STANDARD
 * @author lchan
 * 
 */
@XStreamAlias("jdbc-first-row-metadata-translator")
public class FirstRowMetadataTranslator extends MetadataResultSetTranslatorImpl {

  public FirstRowMetadataTranslator() {
    super();
  }

  @Override
  public void translate(JdbcResult source, AdaptrisMessage target) throws SQLException, ServiceException {
    List<MetadataElement> added = new ArrayList<MetadataElement>();

    int resultSetCount = 0;
    for (JdbcResultSet resultSet : source.getResulSets()) {
      if ((resultSet.getRows() != null) && (resultSet.getRows().size() > 0)) {
        JdbcResultRow storedProcedureResultRow = resultSet.getRows().get(0);
        if (storedProcedureResultRow != null) {
          for (int i = 0; i < storedProcedureResultRow.getFieldCount(); i++) {
            String column = storedProcedureResultRow.getFieldName(i);
            String resultSetPrefix = source.countResultSets() > 1
                ? Integer.toString(resultSetCount) + getResultSetCounterPrefix()
                : "";
            MetadataElement md = new MetadataElement(resultSetPrefix + getMetadataKeyPrefix() + getSeparator()
                + getColumnNameStyle().format(column), toString(storedProcedureResultRow, i));
            if (log.isTraceEnabled()) {
              added.add(md);
            }
            target.addMetadata(md);
          }
          if (log.isTraceEnabled()) {
            log.debug("Added metadata : " + added);
          }
        }
        else {
          log.debug("No Rows to process");
        }
        resultSetCount++;
      }
    }
  }

}
