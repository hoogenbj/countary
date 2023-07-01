/*
 * Copyright (c) 2022-2023. Johan Hoogenboezem
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

package hoogenbj.countary.util;

import javafx.beans.value.ObservableValue;

import java.util.EnumSet;
import java.util.function.Function;

public class InputUtils {

    private Runnable callback;

    public InputUtils(Runnable callback) {
        this.callback = callback;
    }

    public <E extends Enum<E>, F> void observeChangesInInput(ObservableValue<F> observableValue, EnumSet<E> inputState, E enumValue) {
        observableValue.addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                inputState.add(enumValue);
            } else {
                inputState.remove(enumValue);
            }
            this.callback.run();
        });
    }

    public <E extends Enum<E>, F> void observeChangesInInput(ObservableValue<F> observableValue, EnumSet<E> inputState, E enumValue, Function<F, Boolean> isValid) {
        observableValue.addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue) && isValid.apply(newValue)) {
                inputState.add(enumValue);
            } else {
                inputState.remove(enumValue);
            }
            this.callback.run();
        });
    }

    public void setCallback(Runnable callback) {
        this.callback = callback;
    }
}
