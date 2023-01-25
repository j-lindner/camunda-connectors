/*
 * Copyright (c) 2023 Infosys Ltd.
 * Use of this source code is governed by MIT license that can be found in the LICENSE file
 * or at https://opensource.org/licenses/MIT
 */
package com.infosys.camundaconnectors.db.mssql;

import com.google.gson.Gson;
import com.infosys.camundaconnectors.db.mssql.model.request.MSSQLRequest;
import com.infosys.camundaconnectors.db.mssql.utility.DatabaseClient;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@OutboundConnector(
    name = "MS SQL",
    inputVariables = {"databaseConnection", "operation", "data"},
    type = "com.infosys.camundaconnectors.db:mssql:1")
public class MSSQLFunction implements OutboundConnectorFunction {
  private static final Logger LOGGER = LoggerFactory.getLogger(MSSQLFunction.class);
  private final Gson gson;
  private final DatabaseClient databaseClient;

  public MSSQLFunction() {
    this(GsonSupplier.getGson(), new DatabaseClient());
  }

  public MSSQLFunction(Gson gson, DatabaseClient databaseClient) {
    this.gson = gson;
    this.databaseClient = databaseClient;
  }

  @Override
  public Object execute(OutboundConnectorContext outboundConnectorContext) throws Exception {
    final var variables = outboundConnectorContext.getVariables();
    final var msSQLDBRequest = gson.fromJson(variables, MSSQLRequest.class);
    outboundConnectorContext.validate(msSQLDBRequest);
    outboundConnectorContext.replaceSecrets(msSQLDBRequest);
    LOGGER.debug("Request verified successfully and all required secrets replaced");
    return msSQLDBRequest.invoke(databaseClient);
  }
}
