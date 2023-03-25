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
package hoogenbj.countary.di;

import com.google.inject.AbstractModule;
import hoogenbj.countary.app.Settings;
import hoogenbj.countary.app.UserInterface;
import hoogenbj.countary.model.DataModel;
import javafx.fxml.FXMLLoader;
import org.jmock.junit5.JUnit5Mockery;

public class TestGuiceModule extends AbstractModule {

    private final JUnit5Mockery context;

    public TestGuiceModule(JUnit5Mockery context) {
        this.context = context;
    }

    @Override
    protected void configure() {
        bind(FXMLLoader.class).toProvider(TestFXMLLoaderProvider.class);
        bind(Settings.class).toInstance(context.mock(Settings.class));
        bind(DataModel.class).toInstance(context.mock(DataModel.class));
        bind(UserInterface.class).toInstance(context.mock(UserInterface.class));
    }

}
