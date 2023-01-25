/*
 * Copyright (c) 2023 Infosys Ltd.
 * Use of this source code is governed by MIT license that can be found in the LICENSE file
 * or at https://opensource.org/licenses/MIT
 */
package com.infosys.camundaconnectors.db.mssql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.infosys.camundaconnectors.db.mssql.model.request.DatabaseConnection;
import com.infosys.camundaconnectors.db.mssql.model.response.QueryResponse;
import com.infosys.camundaconnectors.db.mssql.utility.DatabaseClient;

import java.sql.*;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MSSQLFunctionTest extends BaseTest {
  @Mock
  private Connection connectionMock;
  @Mock
  private DatabaseClient databaseClient;
  @Mock
  private Statement statementMock;
  @Mock
  private PreparedStatement preparedStatementMock;
  @Mock
  private ResultSet resultSetMock;
  @Mock
  private ResultSetMetaData rsMetaMock;
  private MSSQLFunction MSSQLFunction;

  @BeforeEach
  public void init() throws SQLException {
    MSSQLFunction = new MSSQLFunction(gson, databaseClient);
    when(databaseClient.getConnectionObject(any(DatabaseConnection.class), any(String.class)))
            .thenReturn(connectionMock);
    when(connectionMock.createStatement()).thenReturn(statementMock);
    when(statementMock.execute(anyString())).thenReturn(true);
    when(statementMock.executeQuery(anyString())).thenReturn(resultSetMock);
    when(resultSetMock.getMetaData()).thenReturn(rsMetaMock);
    when(resultSetMock.next()).thenReturn(true).thenReturn(false);
    when(rsMetaMock.getColumnCount()).thenReturn(1);
    when(rsMetaMock.getColumnName(1)).thenReturn("something");
    when(resultSetMock.getObject(1)).thenReturn(576);
    when(statementMock.executeUpdate(anyString())).thenReturn(1);
    when(connectionMock.prepareStatement(any(String.class))).thenReturn(preparedStatementMock);
    when(preparedStatementMock.executeUpdate()).thenReturn(1);
  }

  @ParameterizedTest
  @MethodSource("executeCreateDatabaseTestCases")
  public void execute_shouldCreateDatabase(String input) throws Exception {
    // Given
    context = getContextBuilderWithSecrets().variables(input).build();
    // When
    Object executeResponse = MSSQLFunction.execute(context);
    // Then
    Mockito.verify(statementMock, Mockito.times(1)).executeUpdate(any(String.class));
    Mockito.verify(connectionMock, Mockito.times(1)).close();
    assertThatItsValid(executeResponse, "created successfully");
  }

  @ParameterizedTest
  @MethodSource("executeInvalidCreateDatabaseTestCases")
  public void execute_shouldThrowErrorForCreateDatabase(String input) {
    // Given
    context = getContextBuilderWithSecrets().variables(input).build();
    // When
    assertThatThrownBy(() -> MSSQLFunction.execute(context))
            // Then
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("must not be blank");
  }

  @ParameterizedTest
  @MethodSource("executeCreateTableTestCases")
  public void execute_shouldCreateTable(String input) throws Exception {
    // Given
    context = getContextBuilderWithSecrets().variables(input).build();
    // When
    Object executeResponse = MSSQLFunction.execute(context);
    // Then
    Mockito.verify(statementMock, Mockito.times(1)).execute(any(String.class));
    Mockito.verify(connectionMock, Mockito.times(1)).close();
    assertThatItsValid(executeResponse, "created successfully");
  }

  @ParameterizedTest
  @MethodSource("executeInvalidCreateTableTestCases")
  public void execute_shouldThrowErrorForCreateTable(String input) {
    // Given
    context = getContextBuilderWithSecrets().variables(input).build();
    // When
    assertThatThrownBy(() -> MSSQLFunction.execute(context))
            // Then
            .isInstanceOf(RuntimeException.class)
            .message()
            .isNotBlank();
  }

  @ParameterizedTest
  @MethodSource("executeInsertDataTestCases")
  public void execute_shouldInsertData(String input) throws Exception {
    // Given
    context = getContextBuilderWithSecrets().variables(input).build();
    // When
    Object executeResponse = MSSQLFunction.execute(context);
    // Then
    Mockito.verify(preparedStatementMock, Mockito.times(1)).executeUpdate();
    Mockito.verify(connectionMock, Mockito.times(1)).close();
    assertThatItsValid(executeResponse, "inserted successfully");
  }

  @ParameterizedTest
  @MethodSource("executeInvalidInsertDataTestCases")
  public void execute_shouldThrowErrorInvalidDataToInsert(String input) throws SQLException {
    // Given
    if (input.contains("SurName"))
      Mockito.when(preparedStatementMock.executeUpdate())
              .thenThrow(new SQLException("\"SurName\": invalid identifier"));
    else if (!input.contains("PersonID")) {
      Mockito.when(preparedStatementMock.executeUpdate())
              .thenThrow(new SQLException("\"PersonID\": is " + "required, primary key"));
    }
    context = getContextBuilderWithSecrets().variables(input).build();
    // When
    RuntimeException thrown =
            assertThrows(
                    RuntimeException.class,
                    () -> MSSQLFunction.execute(context),
                    "RuntimeException was expected");
    assertNotNull(thrown.getMessage());
  }

  @ParameterizedTest
  @MethodSource("executeDeleteDataTestCases")
  public void execute_shouldDeleteData(String input) throws Exception {
    // Given
    context = getContextBuilderWithSecrets().variables(input).build();
    // When
    Object executeResponse = MSSQLFunction.execute(context);
    // Then
    Mockito.verify(statementMock, Mockito.times(1)).executeUpdate(any(String.class));
    Mockito.verify(connectionMock, Mockito.times(1)).close();
    assertThatItsValid(executeResponse, "deleted successfully");
  }

  @ParameterizedTest
  @MethodSource("executeInvalidDeleteDataTestCases")
  public void execute_shouldThrowErrorInvalidDeleteDataInputs(String input) {
    // Given
    context = getContextBuilderWithSecrets().variables(input).build();
    // When
    RuntimeException thrown =
            assertThrows(
                    RuntimeException.class,
                    () -> MSSQLFunction.execute(context),
                    "RuntimeException was expected");
    assertNotNull(thrown.getMessage());
  }

  @ParameterizedTest
  @MethodSource("executeReadDataTestCases")
  public void execute_shouldReadData(String input) throws Exception {
    // Given
    context = getContextBuilderWithSecrets().variables(input).build();
    // When
    Object executeResponse = MSSQLFunction.execute(context);
    // Then
    Mockito.verify(statementMock, Mockito.times(1)).executeQuery(any(String.class));
    Mockito.verify(connectionMock, Mockito.times(1)).close();
    assertThat(executeResponse).isInstanceOf(QueryResponse.class);
    @SuppressWarnings("unchecked")
    QueryResponse<List<Map<String, Object>>> response =
            (QueryResponse<List<Map<String, Object>>>) executeResponse;
    assertThat(response.getResponse()).isNotNull();
    assertThat(response)
            .extracting("response")
            .asInstanceOf(InstanceOfAssertFactories.LIST)
            .isNotEmpty();
  }

  @ParameterizedTest
  @MethodSource("executeInvalidReadDataTestCases")
  public void execute_shouldThrowErrorInvalidReadDataInputs(String input) {
    // Given
    context = getContextBuilderWithSecrets().variables(input).build();
    // When
    RuntimeException thrown =
            assertThrows(
                    RuntimeException.class,
                    () -> MSSQLFunction.execute(context),
                    "RuntimeException was expected");
    assertNotNull(thrown.getMessage());
  }

  @ParameterizedTest
  @MethodSource("executeUpdateDataTestCases")
  public void execute_shouldUpdateData(String input) throws Exception {
    // Given
    context = getContextBuilderWithSecrets().variables(input).build();
    // When
    Object executeResponse = MSSQLFunction.execute(context);
    // Then
    Mockito.verify(statementMock, Mockito.times(1)).executeUpdate(any(String.class));
    Mockito.verify(connectionMock, Mockito.times(1)).close();
    assertThatItsValid(executeResponse, "updated successfully");
  }

  @ParameterizedTest
  @MethodSource("executeInvalidUpdateDataTestCases")
  public void execute_shouldThrowErrorInvalidUpdateDataInputs(String input) {
    // Given
    context = getContextBuilderWithSecrets().variables(input).build();
    // When
    RuntimeException thrown =
            assertThrows(
                    RuntimeException.class,
                    () -> MSSQLFunction.execute(context),
                    "RuntimeException was expected");
    assertNotNull(thrown.getMessage());
  }

  @ParameterizedTest
  @MethodSource("executeAlterTableTestCases")
  public void execute_shouldAlterTable(String input) throws Exception {
    // Given
    context = getContextBuilderWithSecrets().variables(input).build();
    // When
    Object executeResponse = MSSQLFunction.execute(context);
    // Then
    Mockito.verify(statementMock, Mockito.times(1)).executeUpdate(any(String.class));
    Mockito.verify(connectionMock, Mockito.times(1)).close();
    assertThatItsValid(executeResponse, "executed successfully");
  }

  @ParameterizedTest
  @MethodSource("executeInvalidAlterTableTestCases")
  public void execute_shouldThrowErrorInvalidAlterTableInputs(String input) {
    // Given
    context = getContextBuilderWithSecrets().variables(input).build();
    // When
    RuntimeException thrown =
            assertThrows(
                    RuntimeException.class,
                    () -> MSSQLFunction.execute(context),
                    "RuntimeException was expected");
    assertNotNull(thrown.getMessage());
  }

  private void assertThatItsValid(Object executeResponse, String validateAgainst) {
    assertThat(executeResponse).isInstanceOf(QueryResponse.class);
    @SuppressWarnings("unchecked")
    QueryResponse<String> response = (QueryResponse<String>) executeResponse;
    assertThat(response.getResponse()).isNotNull();
    assertThat(response)
            .extracting("response")
            .asInstanceOf(InstanceOfAssertFactories.STRING)
            .contains(validateAgainst);
  }
}
