/*
 * Copyright (c) 2023. Johan Hoogenboezem
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

package hoogenbj.countary.util.ofx;

import java.util.regex.Pattern;

public class Tags {
    public static final Pattern HEADER_REGEX = Pattern.compile("([A-Z]+):(\\w+)");
    public static final Pattern SIMPLE_TAG_REGEX = Pattern.compile("\\<([A-Z0-9.]+)\\>(.+)");
    public static final Pattern COMPLEX_TAG_REGEX = Pattern.compile("\\<([A-Z0-9.]+)\\>$");
    public static final Pattern END_TAG_REGEX = Pattern.compile("\\</([A-Z0-9.]+)\\>$");
}
