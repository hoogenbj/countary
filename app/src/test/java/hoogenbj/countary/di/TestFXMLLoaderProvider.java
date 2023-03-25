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

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import javafx.fxml.FXMLLoader;
import org.jmock.junit5.JUnit5Mockery;

public class TestFXMLLoaderProvider implements Provider<FXMLLoader> {

    @Inject Injector injector;

    @Override
    public FXMLLoader get() {
        FXMLLoader loader = new FXMLLoader();
        loader.setControllerFactory(p -> {
            System.out.println(p.getName());
            return injector.getInstance(p);
        });
        return loader;
    }

}
