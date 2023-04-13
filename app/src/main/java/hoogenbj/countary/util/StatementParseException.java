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

package hoogenbj.countary.util;

public class StatementParseException extends RuntimeException {

    public StatementParseException(String path, int lineNumber, int fieldNumber, Throwable e) {
        super(String.format("Error in file %s at line %d in field %d", path, lineNumber, fieldNumber), e);
    }

    public StatementParseException(String path, int lineNumber, Throwable e) {
        super(String.format("Error in file %s at line %d", path, lineNumber), e);
    }

    public StatementParseException(String path, Throwable e) {
        super(String.format("Error in file %s ", path), e);
    }

    public StatementParseException(String message, String path) {
        super(String.format("%s in file %s ", message, path));
    }
}
