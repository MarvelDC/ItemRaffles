package me.marveldc.itemraffles;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Database {
    private final Connection connection;

    private PreparedStatement preparedStatement;

    public Database(Connection connection) {
        this.connection = connection;
    }

    public Database prepare(String query, Object... variables) throws SQLException {
        if (variables == null) {
            preparedStatement = connection.prepareStatement(query);
            return this;
        }

        preparedStatement = connection.prepareStatement(query);
        for (int i = 0; i < variables.length; i++) {
            preparedStatement.setObject(i + 1, variables[i]);
        }
        return this;
    }

    public boolean run() {
        try {
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException error) {
            error.printStackTrace();
            return false;
        }
    }

    public ResultSet get() {
        try {
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) return resultSet;
            else return null;
        } catch (SQLException error) {
            error.printStackTrace();
            return null;
        }
    }

    public void close() {
        try {
            if (connection != null) connection.close();
            if (preparedStatement != null) preparedStatement.close();
        } catch (SQLException error) {
            error.printStackTrace();
        }
    }
}