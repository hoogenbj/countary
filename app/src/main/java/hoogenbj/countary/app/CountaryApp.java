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
package hoogenbj.countary.app;

import com.google.inject.Guice;
import com.google.inject.Injector;
import hoogenbj.countary.di.GuiceModule;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

public class CountaryApp extends Application implements Thread.UncaughtExceptionHandler {

    public static Window OWNER_WINDOW = null;
    public static Injector injector;

    @Override
    public void init() throws Exception {
        super.init();
        injector = Guice.createInjector(new GuiceModule());
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void start(Stage stage) {
        new CountaryController().show(injector, stage);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("countary.png")));
    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        ErrorDialogController instance = ErrorDialogController.getInstance(OWNER_WINDOW, e);
        instance.showAndWait();
    }
}
