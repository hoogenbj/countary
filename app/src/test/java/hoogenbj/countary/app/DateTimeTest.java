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

import org.junit.jupiter.api.Test;

import java.util.Date;

public class DateTimeTest {
    @Test
    public void testMillis() {
        long instant = 1642636800000L;
        System.out.println(new Date(instant));
        instant = 1642629600000L; //posting date
        System.out.println(new Date(instant));
        instant = 1642543200000L; // tx date
        System.out.println(new Date(instant));
    }
}
