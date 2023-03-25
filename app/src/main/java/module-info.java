/*
 * Copyright (c) 2022. Johan Hoogenboezem
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
module hoogenbj.countary {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.sql;
    requires DsvParser;
    requires java.prefs;
    requires com.google.guice;
    requires org.xerial.sqlitejdbc;
    requires org.controlsfx.controls;
    requires org.apache.commons.text;
    requires org.apache.commons.io;

    exports hoogenbj.countary.app;
    opens hoogenbj.countary.app to javafx.fxml, com.google.guice, jmock.junit5;
    opens hoogenbj.countary.di to com.google.guice;
    exports hoogenbj.countary.model;
    opens hoogenbj.countary.model to com.google.guice, javafx.fxml, jmock.junit5;
}
